# Epic 5 Context: Demander et suivre un prêt

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) simule un emprunt, dépose une demande de prêt traitée en arrière-plan par le scoring crédit, reçoit la décision dans son centre de notifications, consulte l'échéancier de ses prêts, marque une échéance payée et supprime un prêt. L'administrateur voit le pipeline des prêts en attente, les approuve ou les refuse (actions auditées) et consulte l'historique de tous les prêts. L'epic livre le service `credits` (base propre, topic interne `credits.pret-demande.v1`, topic `credits.pret-decide.v1` consommé par `notifications`, projection locale des clients).

## Stories

- Story 5.1: Ouvrir la page Prêts et simuler un emprunt
- Story 5.2: Déposer une demande de prêt et la transmettre au scoring
- Story 5.3: Approuver ou refuser automatiquement une demande par le scoring crédit
- Story 5.4: Recevoir la décision sur sa demande de prêt dans le centre de notifications
- Story 5.5: Consulter l'échéancier, marquer une échéance payée et supprimer un prêt
- Story 5.6: Traiter le pipeline des prêts en attente depuis l'administration

## Requirements & Constraints

- Page Prêts ouverte à tout rôle authentifié, même interface ; sans session, redirection vers la connexion (côté API : 401). Sections « Simulateur de pret », « Demande de pret », « Mes emprunts ».
- Simulation : valeurs par défaut 10 000 EUR, 3,50 %, 36 mois ; taux 0-30 (« Taux invalide (0-30%) »), durée 1-360 (« Duree invalide (1-360 mois) »), montant selon « Montant invalide » (story d'epic 4). Annuité constante, mensualité = montant/mois si taux nul. Référence : 10 000 EUR, 3,5 %, 36 mois = 293.02 EUR, coût total 10,548.72 EUR (format anglo-saxon conservé).
- Demande : cinq types (Immobilier, Automobile, Consommation par défaut, Travaux, Etudiant), objet non vide (« Veuillez indiquer un motif »), « Taux invalide », « Duree invalide ». Durée de 301 à 360 mois refusée dès la soumission (« Duree invalide »), rien enregistré ni transmis ; 300 acceptée.
- Demande valide : prêt EN_ATTENTE avec mensualité, coût, intérêts, début = 1er du mois courant, fin = début + durée ; transmise de façon asynchrone ; message « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) ». Si la transmission échoue : message d'indisponibilité, prêt annulé, aucun EN_ATTENTE résiduel.
- Scoring : refus si durée > 300 mois (évalué en premier) ; plafonds par type (Consommation 25 000, Auto 40 000, Travaux 80 000, Etudiant 20 000, Immobilier 600 000 ; égal au plafond = approuvé) ; type absent ou inconnu = refus.
- Approbation : EN_COURS + échéancier d'une échéance par mois + notification CONFIRMATION ; refus : REFUSE + notification ALERTE (textes du legacy, « {type} » en libellé). Demande sur un prêt non EN_ATTENTE : ignorée, sans effet ni notification (vaut aussi pour l'ADMIN).
- Liste « Emprunts en cours » : seulement les prêts de l'utilisateur, tri par date de début décroissante, statuts en clair, « En cours de traitement... » pour EN_ATTENTE, « Aucun emprunt enregistre » si vide. DEFAUT existe sans transition.
- Échéancier (EN_COURS ou REMBOURSE) : « Voir », statuts « Paye » / « En attente », dates jj/MM/aaaa. « Marquer paye » : message « Echeance marquee comme payee », pas de double paiement, tout payé = REMBOURSE. « Supprimer » : suppression définitive du prêt et de ses échéances, sans confirmation.
- Ni paiement d'échéance ni approbation ne déplacent d'argent. Aucun message technique interne affiché à l'utilisateur. Bugs conservés (non corrigés) : objet de prêt trop long.
- Golden master : SC-007-01 et SC-007-02, vérifiés par `scripts/golden-master/verifier-scenario.sh` ; contrôle de fin d'epic à la charge de l'humain. Textes du legacy à l'identique, aucun texte inventé.

## Technical Decisions

- `credits` possède `LOANS`, `LOAN_REPAYMENTS`, `CREDIT_SCORES`, `RISK_PROFILES` ; base `postgres-credits` propre, port de dev 8083, proxy `/api/credits/**` tel quel ; architecture hexagonale testée (`HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest`). Pas d'appel HTTP entre services : projection des clients alimentée par Kafka.
- Kafka : topic `{producteur}.{evenement-au-participe}.v{n}`, enveloppe JSON (`idEvenement`, `type`, `version`, `horodatage`, `producteur`, `donnees`), clé = identifiant numérique de l'agrégat, sans en-tête de type. `credits.pret-demande.v1` (interne, champ `pretId`, à inscrire au catalogue des topics) ; `credits.pret-decide.v1` (clé `pretId` ; `pretId`, `clientId`, `statut`, `notification`). Producteur déclare son topic par `NewTopic`.
- Outbox transactionnelle pour toute publication liée à une écriture ; demande « transmise » quand la ligne d'outbox est validée. Consommateurs idempotents (`evenements_traites`, écouteurs unitaires). Audit par `outbox_audit` en transaction indépendante.
- Machine d'états du prêt dans `domain`, transitions par `UPDATE … WHERE statut = ?` ; zéro ligne = refus sans effet de bord (pas d'échéancier, d'événement ni d'audit en double). Test d'intégration de transition concurrente exigé (`*TransitionConcurrente*Test`).
- La notification est décidée par le producteur (`credits`) : titre, message, type rendus dans `donnees.notification` ; `notifications` stocke, plafonne à 20 par utilisateur (la plus ancienne supprimée) et n'a aucune règle métier.
- Temps uniquement par le port `Horloge` (Europe/Paris) ; dates seules en `yyyy-MM-dd`, instants en UTC. Montants `BigDecimal` 2 décimales, `numeric(19,2)`, chaîne en JSON ; le front calcule en centimes. IBAN masqué dans les journaux ; JSON en français camelCase ; erreurs au format `{"message"}`.
- `data-testid` au format `pret-{élément}[-{id}]` sur tout élément interactif, messages `pret-message-erreur|succes|info`.

## UX & Interaction Patterns

- Le résultat de simulation est un bloc repliable : le lien « Voir l echeancier complet ({n} echeances) » porte `aria-expanded` et `aria-controls` ; colonnes « # », « Capital », « Interets », « Total », « Capital restant ».
- Statuts de prêt en badge : la couleur double le texte sans le remplacer.
- Montants et dates au format du legacy écran par écran, pas le format « 1 234,56 € » du design.
- Administration : section « Pipeline Prets — En attente d approbation » avec « Approuver » / « Refuser », retour par message (« Prêt approuvé ») ; en cas d'erreur, texte de remplacement « Erreur lors du traitement du prêt. Veuillez réessayer. » au lieu du message technique.

## Cross-Story Dependencies

- 5.1 crée le module `credits` et la base ; 5.2 à 5.6 en dépendent. 5.2 transmet, 5.3 décide, 5.4 notifie (consomme `credits.pret-decide.v1`), 5.5 exploite l'échéancier généré en 5.3, 5.6 réutilise la transition d'approbation/refus de 5.3 (garde « non EN_ATTENTE » commune).
- 5.1 exige l'epic 2 terminé (jeton, format d'erreur, gabarit, navigation, garde des routes, script de vérification de scénario) et la story d'epic 4 qui livre « Montant invalide ».
- Le service `notifications` et le journal d'audit (existants avant l'epic) reçoivent de nouveaux événements ; l'espace administration (epic 9 pour le reste) est touché seulement par le pipeline des prêts.
