---
title: 'Approuver ou refuser automatiquement une demande par le scoring crédit'
type: 'feature'
created: '2026-10-06'
baseline_revision: 'ba83fe40769021eb875b3dfb7623282fc2d11880'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-5-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problème :** `credits` publie `credits.pret-demande.v1` (story 5.2) mais n'a aucun consommateur : un prêt reste EN_ATTENTE, sans échéancier ni décision, et rien n'est publié pour le centre de notifications.

**Approche :** un écouteur Kafka unitaire (groupe `credits`) applique le scoring du domaine, fait la transition conditionnelle EN_ATTENTE → EN_COURS (avec échéancier) ou REFUSE, marque l'événement traité et écrit l'outbox `credits.pret-decide.v1`, le tout dans une transaction.

## Boundaries & Constraints

**Always :**
- Scoring dans `domain`, ordre fixe : durée > 300 → refus ; type absent (`null`) → refus ; plafond (CONSOMMATION 25 000, AUTO 40 000, TRAVAUX 80 000, ETUDIANT 20 000, IMMOBILIER 600 000, comparaison `montant > plafond` : égal = approuvé, +0.01 = refusé) ; sinon approbation. « Type hors des cinq » est déjà impossible en base (contrainte `loans_type`) : le seul « type absent » testable est `type` NULL, d'où `V3` (voir plus bas).
- Écouteur `@KafkaListener(topics = "credits.pret-demande.v1")` dans `adapter.in.evenements`, modèle `services/notifications/.../adapter/in/RecepteurTemoin.java` (chaîne JSON, sans en-tête de type ; type `pret-demande`, version 1, sinon ignoré et journalisé) ; propriétés `spring.kafka.consumer.group-id=credits` et `auto-offset-reset=earliest` ; écouteur unitaire (déjà `listener.type=single`).
- Cas d'usage `DecisionPretCasUsage` (port `in.DecisionPret`) en Java pur : dans `Transaction.executer` — `EvenementsTraites.marquerTraite(idEvenement)` (faux = déjà traité, rien d'autre) ; `PretRepository.parId` (absent ou statut ≠ EN_ATTENTE = ignoré sans erreur, sans effet, sans événement) ; scoring ; `PretRepository.transitionner(pretId, de, vers)` par `UPDATE loans SET status = ? WHERE id = ? AND status = 'EN_ATTENTE'`, zéro ligne = ignoré sans effet de bord ; si approuvé, `creerEcheancier` (une échéance par mois : numéro 1..N, `date_echeance` = `dateDebut.plusMonths(numero)`, montants et capital restant issus de `Annuite.simuler`, `paye = false`) ; `EvenementsSortants.consigner(PretDecide)`. Après validation, `publier(idLigne)` (échec avalé, relais en secours, comme 5.2).
- Table `evenements_traites(id_evenement uuid primary key)` et `loans.type` nullable : migration `V3__evenements_traites_et_type_facultatif.sql` ; `PretRepositoryJdbc.pret` et `PretJson` tolèrent `type` NULL (JSON `null`) sans planter la liste.
- Événement `credits.pret-decide.v1` (outbox, clé = `pretId`, enveloppe comme 5.2 : `type:"pret-decide"`, `version:1`, `producteur:"credits"`) ; `donnees` = `{pretId, clientId, statut, notification}`, `statut` = `EN_COURS` ou `REFUSE`, `notification` = `{clientId, type, titre, message}`. Approbation : `CONFIRMATION`, titre « Demande de prêt approuvée » (`pret.notif.titre.approuve`), message « Votre demande de pret {libelle} de {montant} EUR a ete approuvee par le service credit. ». Refus : `ALERTE`, titre « Demande de prêt refusée » (`pret.notif.titre.refuse`), message « Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). ». `{libelle}` : « Immobilier », « Automobile », « Consommation », « Travaux », « Etudiant », type absent « (type non reconnu) » (`pret.libelle.inconnu`) ; `{montant}` = `montant.toPlainString()` (2 décimales). Textes rendus dans `domain`/`application`, jamais dans `notifications`.
- `contrats/evenements/credits.pret-decide.v1.schema.json` (forme de `credits.pret-demande.v1.schema.json`, `statut` enum, exemple) ; bean `NewTopic` `credits.pret-decide.v1` 3 partitions / 1 réplica dans `ConfigurationEvenements` ; `EvenementsSortants`/`EvenementsSortantsKafka` généralisés pour consigner les deux événements (pas de duplication de l'outbox).
- Les tests existants de 5.2 qui constatent EN_ATTENTE après un POST (`DemandePretHttpIT`, `KafkaInjoignableTest`, etc.) doivent désactiver l'écouteur (`spring.kafka.listener.auto-startup=false` dans leur `@DynamicPropertySource`) ; leurs assertions ne changent pas.
- Tags de test : `@Tag("FR-007-09")`, `FR-007-10`, `FR-024-03`, `FR-024-04`, `FR-024-05` (+ `BUG-078` pour le prêt sans type), `FR-024-06`, `FR-024-07`, `FR-024-08` ; tests `PretTransitionConcurrenteTest`, `PretDecideContratEvenementProducteurTest`, idempotence Testcontainers (même message brut livré deux fois sur le vrai topic : une décision, un échéancier).
- `domain`/`application` en Java pur, 100 % lignes/branches ; temps par le port `Horloge` ; seuils intacts.

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; jamais de rejeu du golden master.
- Pas de consommateur côté `notifications` (5.4), pas de bouton « Voir » ni d'échéancier à l'écran (5.5), pas de pipeline ni d'approbation manuelle (5.6), pas de modification du front, pas d'appel HTTP entre services, pas de texte inventé, pas de nouvelle dépendance. Ne pas écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| Approbation 36 mois | EN_ATTENTE CONSOMMATION 10 000 | EN_COURS, 36 échéances non payées, 1 ligne outbox `pret-decide` CONFIRMATION | — |
| Plafond exact / +0.01 | chaque type | approuvé / refusé | — |
| Durée 301 sur montant faible | EN_ATTENTE | REFUSE, aucune échéance, ALERTE | — |
| Sans type | 10 000 EUR, 60 mois, `type` NULL inséré | REFUSE, libellé « (type non reconnu) » | — |
| Prêt non EN_ATTENTE | EN_COURS, REFUSE, REMBOURSE | message ignoré : statut inchangé, 0 échéance, 0 événement | pas d'erreur |
| Prêt inconnu | `pretId` absent de `loans` | ignoré | journalisé, pas d'erreur |
| Même message livré 2 fois | brut, sans en-tête | un seul effet | — |
| Deux transitions concurrentes | approbation et refus | une seule gagne, un seul échéancier | perdant ignoré |

</intent-contract>

## Code Map

- `services/credits/src/main/java/fr/nextbanking/credits/domain/` -- `TypePret`, `Pret`, `StatutPret`, `Annuite.simuler`, `NouveauPret` (modèles) ; à créer `Scoring`/`DecisionScoring`, `PretDecide` (modèle `PretDemande`), `Notification`, `EcheancePret`.
- `.../application/` -- `DeposerDemandePretCasUsage` (modèle de transaction et de publication après validation) ; à créer `port/in/DecisionPret`, `DecisionPretCasUsage`, `port/out/EvenementsTraites` ; étendre `port/out/{PretRepository,EvenementsSortants}`.
- `.../adapter/out/persistance/PretRepositoryJdbc.java` -- ajouter `transitionner`, `creerEcheancier` (batch sur `loan_repayments`) ; créer `EvenementsTraitesJdbc` (copie de `services/notifications/.../EvenementsTraitesJdbc.java`).
- `.../adapter/out/evenements/{EvenementsSortantsKafka,EnveloppePretDemande}.java` -- généraliser `consigner` ; créer `EnveloppePretDecide`.
- `.../adapter/in/evenements/` -- à créer : `RecepteurPretDemande` (modèle `services/notifications/.../adapter/in/RecepteurTemoin.java`).
- `.../adapter/in/http/PointEntreePrets.java` -- `PretJson.de` : `type` nullable.
- `.../infrastructure/{AssemblageCredits,ConfigurationEvenements}.java` -- bean du cas d'usage, `NewTopic` `pret-decide`.
- `services/credits/src/main/resources/{application.properties,db/migration/V3__evenements_traites_et_type_facultatif.sql}` -- groupe `credits`, `earliest`, migration.
- `contrats/evenements/credits.pret-decide.v1.schema.json` -- modèle `credits.pret-demande.v1.schema.json`.
- `services/credits/src/test/**` -- `PretDemandeContratEvenementProducteurTest`, `LecteurTopic`, `Conteneurs`, `OutilsHttp.chargerDonnees` (ajouter `evenements_traites` au `truncate`) en modèles ; ITs 5.2 à neutraliser (écouteur arrêté).
- Lecture seule : `docs/retro/features/FEAT-024/{RG,MESSAGES}.md`, `textes-nouveaux.md` l.124-126, PRD FR-024-03 à 08, spine l.364-371.

## Tasks & Acceptance

**Execution :**
- `services/credits/src/main/java/**/domain` -- scoring, décision, notification, échéance, `PretDecide` -- FR-007-09, FR-024-03..05.
- `services/credits/src/main/java/**/application` -- cas d'usage de décision et ports -- FR-007-10, FR-024-06..08.
- `services/credits/src/main/java/**/adapter` + `infrastructure` -- écouteur, transition conditionnelle, échéancier, `evenements_traites`, outbox `pret-decide`, topic -- FR-024-06..08.
- `services/credits/src/main/resources/**`, `contrats/evenements/credits.pret-decide.v1.schema.json` -- migration V3, propriétés, schéma.
- `services/credits/src/test/**` -- tests unitaires (matrice I/O, `@ParameterizedTest` plafond et plafond + 0.01 par type), IT consommateur Testcontainers (Postgres + Kafka, double livraison), `PretTransitionConcurrenteTest`, `PretDecideContratEvenementProducteurTest`, adaptation des ITs 5.2 -- tags de la story.

**Acceptance Criteria :**
- Given un prêt EN_ATTENTE publié sur `credits.pret-demande.v1`, when l'écouteur le traite, then le scoring décide dans l'ordre durée, type, plafond ; un test paramétré prouve plafond = approuvé et plafond + 0.01 = refusé pour chaque type.
- Given un prêt de 36 mois approuvé, then `UPDATE … WHERE status = 'EN_ATTENTE'` le passe EN_COURS, 36 échéances non payées existent, créées avec la ligne `evenements_traites` dans une seule transaction ; `GET /api/credits/prets` renvoie `EN_COURS`.
- Given un prêt refusé, then REFUSE, aucune échéance ; sans type (10 000 EUR, 60 mois, insertion directe) → REFUSE (`FR-024-05`, `BUG-078`).
- Given un prêt non EN_ATTENTE, then message ignoré sans erreur : statut inchangé, aucune échéance, aucun `pret-decide` dans l'outbox.
- Given le même message brut livré deux fois (Testcontainers), then une décision et un échéancier ; `PretTransitionConcurrenteTest` constate une seule transition gagnante.
- Given une décision appliquée, then une ligne outbox `credits.pret-decide.v1` (clé `pretId`) existe dans la même transaction, avec `donnees` et textes exacts ; le schéma, le bean `NewTopic` et `PretDecideContratEvenementProducteurTest` existent.
- Given `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-09|FR-007-10|FR-024-03|FR-024-04|FR-024-05|FR-024-06|FR-024-07|FR-024-08|BUG-078'` puis `mvn -B -q -f services/credits/pom.xml verify`, then code 0, chaque tag porté par au moins un test.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 0, false 4, maybe-false 0
- findings:
  - `[false]` `[reject]` la notification est écrite dans l'événement mais ni livrée ni stockée, aucun test ne la montre à un client — la story 5.4 porte le consommateur de `notifications` (hors périmètre explicite de l'intention et de la spec) ; la story 5.3 produit `credits.pret-decide.v1`, vérifié par `PretDecideContratEvenementProducteurTest` et `DecisionPretKafkaIT` (lecture du topic).
  - `[false]` `[reject]` effets de bord hors texte littéral (V3 `type` nullable, `JdbcTemplate`, groupe `credits`, écouteur arrêté dans les ITs de 5.2) — tous prévus par la spec (V3, groupe/offset, neutralisation des ITs de 5.2) ou nécessaires à l'échéancier par lot ; aucune assertion existante modifiée.
  - `[false]` `[reject]` textes et plafonds du legacy codés en dur, sources citées seulement en commentaire — voulu : textes à l'identique, aucun test ne doit lire le corpus.
  - `[false]` `[reject]` consommation de `pret-decide` absente et cas non couverts (type/version inattendus, prêt inconnu sans ligne d'outbox) — consommation = story 5.4 ; type/version couverts par `RecepteurPretDemandeTest`, prêt inconnu par le cas d'usage et l'IT (`999_999`).

## Design Notes

Le « bouton Voir » de la liste est livré par la story 5.5 (échéancier) ; la story 5.3 est observée à l'API (`GET /api/credits/prets`). Le message est marqué traité même quand le prêt est ignoré (non EN_ATTENTE ou inconnu) : une redistribution ne refait rien. `V3` rend `type` nullable seulement pour pouvoir enregistrer le cas « sans type » du legacy (BUG-078) ; la contrainte `loans_type` continue de refuser toute valeur hors des cinq types.

## Verification

**Commands :**
- `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-09|FR-007-10|FR-024-03|FR-024-04|FR-024-05|FR-024-06|FR-024-07|FR-024-08|BUG-078'` puis `mvn -B -q -f services/credits/pom.xml verify` -- expected: code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** un écouteur Kafka unitaire (groupe `credits`) applique le scoring (durée > 300, type absent, plafond par type) à chaque `credits.pret-demande.v1` : transition conditionnelle EN_ATTENTE → EN_COURS avec échéancier mensuel, ou REFUSE, marquage `evenements_traites` et ligne d'outbox `credits.pret-decide.v1` (notification rendue par `credits`) dans une seule transaction.

**Fichiers :** `services/credits/**` (domaine `Scoring`, `DecisionScoring`, `EcheancePret`, `PretDecide`, `Notification`, `EvenementSortant` ; cas d'usage `DecisionPretCasUsage` ; `RecepteurPretDemande` ; `PretRepositoryJdbc` (transition, échéancier), `EvenementsTraitesJdbc`, outbox généralisée ; migration `V3` ; `NewTopic` ; tests dont `PretTransitionConcurrenteTest`, `DecisionPretKafkaIT`, `PretDecideContratEvenementProducteurTest`), `contrats/evenements/credits.pret-decide.v1.schema.json`.

**Revue :** 0 patch, 0 report, 4 rejets motivés (cf. Review Triage Log). Revue complémentaire recommandée : `false` (patches : high 0, medium 0).

**Vérification :** `mvn test -Dgroups=…` et `mvn verify` credits en code 0 (209 tests, 0 échec), chaque tag porté ; `build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh`, `trace-smoke.sh` en code 0.

**Risques résiduels :** `DecisionPretKafkaIT` dépend un peu du timing (assignation des partitions + 1 s) ; le bouton « Voir » et l'affichage de la notification relèvent des stories 5.5 et 5.4 ; `loans.type` devient nullable (cas BUG-078) sans changer la contrainte sur les valeurs.
