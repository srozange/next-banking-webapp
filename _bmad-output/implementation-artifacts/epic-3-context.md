# Epic 3 Context: Consulter ses comptes et virer de l'argent en sécurité

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Un client vire de l'argent entre ses comptes. Chaque virement passe par un contrôle de fraude synchrone avant tout débit : il est exécuté, suspendu en attente de la revue de l'administrateur (24 h au plus, puis rejet automatique sans débit), ou refusé sans débit si le contrôle tombe en erreur. Le client consulte ses comptes, ses dernières opérations, son relevé, ses notifications et l'état de ses virements passés en revue. L'administrateur libère ou rejette un virement suspendu, avec un motif obligatoire et audité.

## Stories

- Story 3.1: Exécuter un virement entre ses comptes
- Story 3.2: Évaluer les signaux de fraude de chaque virement
- Story 3.3: Notifier le client de l'issue de son virement
- Story 3.4: Suspendre, refuser, revoir et rejeter à l'échéance un virement (socle RG-027-003)
- Story 3.5: Suspendre un virement qui fait dépasser le cumul glissant sur 24 heures
- Story 3.6: Refaire un virement refusé depuis un formulaire prérempli
- Story 3.7: Consulter le relevé d'un compte
- Story 3.8: Voir ses comptes et ses dernières opérations sur le tableau de bord
- Story 3.9: Lire et gérer ses notifications sur le tableau de bord
- Story 3.10: Lever une alerte de fraude après des échecs de connexion répétés

## Requirements & Constraints

- Aucun débit avant la décision du contrôle de fraude ; jamais deux débits, ni aucun, ni deux notifications pour un même virement.
- Un rejet automatique, un rejet administrateur ou un refus pour erreur de contrôle ne débite jamais le compte.
- Motif du virement limité à 100 caractères (refus 400 avec message dédié) ; messages d'erreur non techniques pour montant invalide et solde insuffisant ; texte d'échec de remise défini.
- Libérer et rejeter : rôle ADMIN, motif obligatoire, identité tracée ; la libération vérifie le solde à l'instant de la commande.
- Seuils de fraude (score, seuil de fraude, cumul glissant 24 h, seuil d'alerte, seuil de mise en attente) en configuration de déploiement, validés au démarrage (valeur invalide = démarrage refusé).
- Un échec ou dépassement de délai d'accès à l'historique donne un refus pour erreur de contrôle.
- Bugs legacy listés dans `docs/retro/00-global/LEGACY-BUGS.md` (ex. mention « [EN ATTENTE] », transaction importante) à corriger, chacun avec un test.
- Contrôle de fin d'epic par l'humain : scénarios golden master SC-002-01, SC-002-02, SC-003-01, SC-003-02, SC-003-04, SC-005-01 via `scripts/golden-master/verifier-scenario.sh` (ne pas rejouer le golden master avant l'epic 10 ; SC-003-04 se rapporte au déterminisme).

## Technical Decisions

- Le contrôle de fraude est décidé dans le domaine du cœur bancaire, sans appel réseau, par un même cas d'usage pour virement immédiat et échéance programmée. Aucun point d'entrée public d'évaluation isolée du score.
- Machine à 5 états : EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE. Transitions : soumission vers EXÉCUTÉ, SUSPENDU ou REFUSÉ ; SUSPENDU vers LIBÉRÉ ou REJETÉ ; LIBÉRÉ vers EXÉCUTÉ ou REJETÉ (« solde insuffisant au débit », acteur SYSTEM). Toute autre transition est refusée ; mise à jour conditionnelle, la première transition gagne. Un virement refusé est persisté (état terminal). Statut d'affichage SUCCESS/PENDING distinct des 5 états.
- Débit EXÉCUTÉ dans la transaction de soumission (mise à jour conditionnelle `solde >= montant`) ; débit d'un LIBÉRÉ par le consommateur de `coeur-bancaire.virement-a-traiter.v1` (consommé par le cœur bancaire lui-même), sans notification.
- Atomicité : transition, débit, remise au traitement asynchrone et événement de décision écrits dans la même transaction via l'outbox. Un échec d'écriture fait échouer l'opération.
- Signaux d'historique lus par un port unique `HistoriqueVirements` ; contrôle et enregistrement sérialisés par client (`pg_advisory_xact_lock(clientId)`) avec `lock_timeout` ; expiration d'accès à l'historique de 2 s (`nb.fraude.acces-historique.delai`).
- Audit : chaque entrée d'audit de virement va dans `outbox_audit` via `Transaction.executerIndependamment(...)` (transaction indépendante, conservée même en cas d'échec). Types : VIREMENT, TRANSFER_EXECUTED, ADMIN_TRANSFER_RELEASE, ADMIN_TRANSFER_REJECT, TRANSFER_AUTO_REJECT, TRANSFER_REJECT_INSUFFICIENT_FUNDS.
- `POST /api/coeur-bancaire/virements` renvoie `etat`, `heureButoir` (si SUSPENDU), `reference` (si refus pour erreur) et l'écho des champs saisis (reprise préremplie). En-tête `Idempotency-Key` unique par soumission : une répétition renvoie le premier résultat.
- Une seule notification, à la décision : événement `coeur-bancaire.virement-decide.v1` (CONFIRMATION, ALERTE ou FRAUDE, texte rendu par le cœur bancaire). Aucune notification pour la remise asynchrone, la libération, le rejet, le rejet automatique ni le refus pour erreur.
- Heure butoir = soumission + 24 h calendaires via le port `Horloge` ; elle fait foi : Libérer refuse à l'heure butoir ou après. Rejet automatique par traitement planifié du cœur bancaire (toutes les minutes), y compris nuit et week-end.
- MULTIPLE_FAILED_AUTH : identité et clients comptent les échecs consécutifs (comptes désactivés inclus, remise à zéro au succès) et publient `identite-clients.echecs-connexion-repetes.v1` au 5e échec puis à chaque suivant ; le cœur bancaire, seul propriétaire de `FRAUD_ALERTS`, crée l'alerte OPEN. Libération : alertes OPEN passent à REVIEWED.
- Traçage : aucun span créé à la main (pas d'`Observation`, `@Observed`, `@NewSpan`) ; l'adaptateur entrant enrichit l'observation courante avec `virement.controle.issue` (EXECUTE, SUSPENDU, REFUSE_ERREUR_CONTROLE) et signale l'erreur.
- Textes de virement dans un seul fichier de textes du cœur bancaire. Conventions générales : montants en `BigDecimal` 2 décimales, temps uniquement via `Horloge`, entre services `compteId` numérique, consommateurs idempotents (`evenements_traites`).
- Tests de référence : `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dtest='MachineEtatsVirement*Test,ControleFraude*Test,EcheanceRevue*Test,LiberationVirement*Test,AuditVirement*Test'`.

## UX & Interaction Patterns

- Couleurs uniquement par les tokens du thème : CREDIT en `success`, DEBIT en `error`, opérations en attente sur fond `warning-container`. Pas de couleur codée en dur ni d'élément natif stylé hors composants du design system.
- Textes repris du legacy (`docs/retro/00-global/MESSAGES.md`) ou de `textes-nouveaux.md` ; aucun texte inventé (un texte manquant est un point ouvert). `data-testid` au format `{écran}-{élément}[-{id}]`.
- Écran de refus : affiche la référence de trace et propose de refaire le virement avec un formulaire prérempli (compte source, destinataire, montant, motif).
- Le tableau de bord (gabarit, « Bonjour, {fullName} », lien « Deconnexion ») affiche comptes, dernières opérations (vide si aucun compte COURANT) et notifications ; le client voit l'état de ses virements passés en revue.

## Cross-Story Dependencies

- Prérequis de l'epic : epic 2 terminé (JWT, format d'erreur `{"message"}`, adresse IP, service `audit`, gabarit, routeur, route `/tableau-de-bord`, `verifier-scenario.sh`) ; story 1.2 (port `Horloge`).
- 3.1 est la base (comptes, opérations, données de référence) ; 3.2 dépend de 3.1 ; 3.3 de 3.1 et 3.2 ; 3.4 pose le socle de suspension/revue/rejet (RG-027-003) ; 3.5 suit immédiatement 3.4 ; 3.6 dépend de 3.4 et 3.5.
- 3.7 dépend de 3.1 ; 3.8 de 3.1, 3.4 et 3.7 ; 3.9 de 3.3 et 3.8 ; 3.10 de 3.2 (`FRAUD_ALERTS`) et de la connexion de l'epic 2.
- Chaque signal de fraude est livré avec sa FR de FEAT-003 et sa FR de FEAT-027 (livraison conjointe).
