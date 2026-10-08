# Epic 4 Context: Programmer ses virements, gérer bénéficiaires et budget, traitements de nuit

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Le client gère ses bénéficiaires externes SEPA (vérification d'IBAN, ajout, suppression logique), crée, suspend, réactive et supprime des virements programmés, et suit son budget mensuel par catégorie avec des plafonds. Le cœur bancaire exécute toutes les 5 minutes les échéances dues, chacune soumise au même contrôle de fraude que le virement immédiat, et prévient le client d'une échéance non exécutée. Les traitements de nuit et périodiques (relevés mensuels, intérêts, transactions importantes, soldes faibles) tournent à leurs horaires en Europe/Paris, sont journalisés, et l'administrateur consulte leur journal dans « Traitements nocturnes ».

## Stories

- Story 4.1: Consulter ses bénéficiaires externes et en supprimer un
- Story 4.2: Vérifier un IBAN et ajouter un bénéficiaire
- Story 4.3: Créer un virement programmé et voir ses virements programmés
- Story 4.4: Suspendre, réactiver ou supprimer un virement programmé
- Story 4.5: Journaliser les traitements planifiés, relevés mensuels et section « Traitements nocturnes »
- Story 4.6: Exécuter automatiquement les échéances dues des virements programmés
- Story 4.7: Traiter les échéances non exécutées et prévenir le client
- Story 4.8: Suspendre une échéance suspecte et la soumettre à la revue
- Story 4.9: Créditer les intérêts mensuels des comptes épargne
- Story 4.10: Compter chaque nuit les transactions importantes
- Story 4.11: Alerter le client d'un solde faible
- Story 4.12: Consulter ses dépenses du mois par catégorie
- Story 4.13: Définir et suivre des plafonds de dépenses par catégorie

## Requirements & Constraints

- Les écrans Bénéficiaires, Virements programmés et Budget sont réservés aux utilisateurs authentifiés : sans jeton, redirection vers la connexion côté front et 401 `{"message": …}` côté API. Le client est toujours déduit du jeton ; aucune donnée d'un autre client n'est visible ni modifiable.
- Accès à la ressource d'un autre client : suppression d'un bénéficiaire refusée en 403 « Erreur lors de la suppression. » ; action sur un virement programmé d'un autre client refusée en 404 identique à celui d'un identifiant inexistant, l'écran affichant le même message. Les actions sur état (suppression logique d'un bénéficiaire, suspension/suppression d'un virement programmé) sont des mises à jour conditionnelles : la première transition gagne, prouvé par un test `*TransitionConcurrente*Test`.
- Bénéficiaires : IBAN validé uniquement par le cœur bancaire (MOD-97, jamais côté front), normalisé à l'enregistrement, affiché masqué ; le beneficiaire est désactivé sans supprimer la ligne ; libellé, titulaire et banque limités à 100 caractères ; zone SEPA calculée depuis le code pays. L'IBAN en clair n'apparaît jamais dans les journaux.
- Virements programmés : bénéficiaire interne uniquement, existence contrôlée dès la création ; « Montant invalide » est le message unique pour montant vide, nul ou négatif ; une date de première exécution passée est acceptée et l'échéance part au prochain passage. Les messages d'erreur technique sont dédiés et sans détail.
- Exécution des échéances : toutes les 5 minutes, une seule échéance exécutée par virement et par passage même en retard de plusieurs périodes, sans dérive du jour et de l'heure (15 du mois à 08:00 reste inchangé après douze mois) ; échéances dues pendant un arrêt exécutées au passage suivant ; un échec isolé annule complètement cette échéance sans bloquer les autres. Audit au nom du titulaire, sans adresse IP.
- Notifications des échéances : une échéance exécutée ne notifie pas ; échéance non exécutée (solde insuffisant ou compte destinataire introuvable) : exactement une notification ALERTE, le refus « compte introuvable » est évalué avant le solde et désactive le virement programmé ; échéance suspendue par la fraude : une notification FRAUDE ; refus pour erreur du contrôle : aucune notification, nouvelle tentative au passage suivant.
- Échéance suspendue : sans débit, heure butoir = instant de l'échéance + 24 h, la prochaine date avance tout de suite, le compteur d'exécutions n'augmente qu'après libération, une échéance rejetée n'est pas rattrapée.
- Traitements planifiés : journal `batch_job_logs` à une ligne par exécution (job, début, fin, durée, enregistrements, statut SUCCESS/FAILED, détail « Erreur: {message} » en cas d'échec) ; une erreur est interceptée, journalisée et n'empêche pas l'exécution suivante ; au plus une exécution par jour calendaire Europe/Paris (y compris aux changements d'heure), sans rattrapage hors virements programmés.
- Règles métier des traitements : intérêts épargne crédités une seule fois, le 1er du mois à l'exécution de 02:00 (1/12 du taux, arrondi moitié vers le haut, compte à solde nul ignoré, aucun crédit quotidien) ; transactions importantes = 10 000 € ou plus (borne incluse, débits et crédits) sur 24 heures écoulées exactes, comptées sans créer d'alerte ; relevés mensuels : une ligne de journal pour les clients actifs de rôle CLIENT, aucun document généré ; solde faible = strictement sous 100 €, clients actifs seulement, plafond de 20 événements par compte et par jour calendaire Europe/Paris, un client ou compte en erreur est ignoré avec avertissement.
- Budget : période au format AAAA-MM (24 mois les plus récents ayant des opérations ; sinon mois courant fourni par le service, jamais l'horloge du navigateur) ; seuls les débits sont ventilés, rattachés à la première catégorie dont un mot-clé figure dans le libellé (insensible à la casse), sinon « Autres ». Un plafond par (client, catégorie, mois), remplacé à l'enregistrement ; catégorie « Revenus » refusée ; plafond de 13 chiffres entiers au plus avec validation explicite ; progression 79 % vert, 80 % orange, 100 % rouge, au-delà « Depassement ».
- Bugs legacy à corriger, chacun avec un test : suppression d'un bénéficiaire d'autrui (BUG-047), virement programmé d'autrui (BUG-013), bénéficiaire inexistant (BUG-015), solde insuffisant silencieux (BUG-014), dérive de l'échéance (BUG-069), refus « compte introuvable » (BUG-068), échec en lot (BUG-070), intérêts quotidiens (BUG-064), fenêtre de 24 h (BUG-066), plafond Revenus (BUG-023), plafond de 20 notifications (BUG-071, BUG-072, BUG-079).
- Contrôle de fin d'epic par l'humain : scénarios golden master SC-004-01, SC-004-02, SC-004-03, SC-006-01, SC-013-01, SC-013-02 via `scripts/golden-master/verifier-scenario.sh` (ne pas rejouer le golden master avant l'epic 10). Les stories 4.4 à 4.11 et 4.13 n'ont aucun scénario.

## Technical Decisions

- Tous les traitements planifiés vivent dans le cœur bancaire : `@Scheduled` dans `adapter.in.planification`, cron avec zone `Europe/Paris` explicite, appel d'un cas d'usage. Horaires : virements programmés toutes les 5 minutes, soldes faibles (`NOTIFICATION_CHECK`) toutes les 30 minutes, intérêts (`INTEREST_CALCULATION`) 02:00, transactions importantes (`FRAUD_DETECTION`) 02:30, relevés (`MONTHLY_STATEMENTS`) le 1er à 03:00. Jobs journalisés : `RECURRING_TRANSFERS` (une ligne par passage), les quatre précédents ; le rejet automatique `SUSPENDED_TRANSFERS_EXPIRY` de l'epic 3 n'écrit qu'en cas de rejet ou d'erreur.
- Garde par date dans le journal pour une exécution par jour ; garde « mois déjà crédité » portée par le domaine pour les intérêts. Aucun traitement métier en profil `rejeu` (seul le relais de l'outbox reste). Pas de verrou distribué : une instance en local, doublons absorbés par les consommateurs idempotents.
- Journal exposé par `GET /api/coeur-bancaire/traitements?limite=50` (ADMIN ; 403 pour un CLIENT, 401 sans jeton), trié par début décroissant. La table `batch_job_logs` est reprise de l'epic 3 si elle existe, sinon créée par migration Flyway ; idem pour la table des bénéficiaires.
- Le temps passe uniquement par le port `Horloge` (Europe/Paris, horloge injectée dans les tests) ; montants `BigDecimal` 2 décimales, chaîne en JSON.
- Chaque échéance passe par le même cas d'usage de contrôle de fraude que le virement immédiat (après contrôles bénéficiaire et solde), avec la machine à états et le rejet automatique de l'epic 3. L'observation de la tâche planifiée porte `virement.controle.issue` (dont `REFUSE_ERREUR_CONTROLE` avec statut d'erreur) ; aucun span créé à la main. Deux passages concurrents sur une même échéance ne créent qu'un virement et un débit.
- Nouveaux événements, schémas JSON dans `contrats/evenements/` avec exemple : `coeur-bancaire.echeance-non-executee.v1` (clé et champ `virementProgrammeId`, bloc `notification`) et `coeur-bancaire.solde-faible-detecte.v1` (clé et champ `compteId`, bloc `notification`). Topic déclaré par un bean `NewTopic` du cœur bancaire, publication par l'outbox, tests `*ContratEvenement*Test` côté producteur et côté `notifications`. La notification (type, titre, texte) est rendue par le producteur ; `notifications` stocke de façon idempotente (`evenements_traites`), plafonne à 20 par utilisateur et ne porte aucune règle métier. Le nombre d'envois par échéance et par jour est compté par le cœur bancaire dans sa base.
- Solde faible : notification de type ACCOUNT_BALANCE, priorité HIGH, icône ⚠️. Entre services, uniquement `compteId` numérique ; le cœur bancaire lit la projection locale des clients (Kafka), jamais d'appel HTTP.
- Journaux : IBAN masqué (`FR35****9010`) via l'objet-valeur `Iban`. Audit par `outbox_audit` en transaction indépendante. Erreurs au format `{"message": …}` ; session expirée : 401 « Session expirée, veuillez vous reconnecter. », le front supprime `nb.jeton` et revient à `/login`.
- Options du `<select>` de fréquence = noms de l'énumération legacy (`MENSUEL`, etc.). Données de référence du cœur bancaire (`reference-data.sql`) : compte `1` de `user1`, compte interne `FR76-0001-0002`, bénéficiaire `FR76***`.

## UX & Interaction Patterns

- Textes repris à l'identique du legacy (`docs/retro/00-global/MESSAGES.md`, casse et absence d'accents comprises) ou de `textes-nouveaux.md` (lot « Stories » : notifications d'échéance, clés `budget.motscles.*`, `budget.erreur.plafond.max`) ; aucun texte inventé.
- `data-testid` au format `{écran}-{élément}[-{id}]` : `beneficiaires-*`, `vp-*` (dont `vp-suspendre-{id}`, `vp-reactiver-{id}`, `vp-supprimer-{id}`), `budget-*`, `admin-traitements-rafraichir`.
- Tableaux natifs avec `<th scope="col">` et ligne « vide » sur toute la largeur ; messages d'erreur annoncés (`role="alert"`, `aria-invalid`, valeurs saisies conservées), succès en `role="status"`, résultat de vérification d'IBAN en `aria-live="polite"` ; boutons d'action sans modale de confirmation ; liste rafraîchie sans rechargement de page.
- Couleurs uniquement par les tokens du thème (barre de progression `success`/`warning`/`error`, fond de dépassement `error-container`) ; la couleur ne remplace jamais le texte (badge « Depassement »). Dates `dd/MM/yyyy` à l'écran des virements programmés, `yyyy-MM-dd` en saisie ; centre de notifications avec horodatage `dd/MM/yyyy HH:mm:ss`.
- La section « Traitements nocturnes » s'ajoute à l'écran `/administration` (ADMIN), avec bouton « Rafraichir ».

## Cross-Story Dependencies

- Prérequis de l'epic : epics 2 et 3 terminés (authentification, gabarit et routes protégées, comptes, contrôle de fraude, machine à états, rejet automatique, file de revue, centre de notifications, projection des clients, `verifier-scenario.sh`).
- 4.2 dépend de 4.1 ; 4.4 de 4.3 ; 4.13 de 4.12.
- 4.5 pose le journal `batch_job_logs`, la garde par date, le profil `rejeu` et la section d'administration ; 4.9, 4.10 et 4.11 en dépendent.
- 4.6 dépend de 4.3 et 4.5 ; 4.7 de 4.6 ; 4.8 de 4.6 et 4.7.
- 4.3 crée le contrôle d'existence du compte destinataire (BUG-015) ; 4.7 le rejoue à l'échéance.
- 4.11 dépend du centre de notifications et du plafond par utilisateur de l'epic 3.
- Le journal de saturation par 48 entrées par jour du solde faible reste un point ouvert jusqu'à l'epic 9.
