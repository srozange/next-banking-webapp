---
title: 'Déposer une demande de prêt et la transmettre au scoring'
type: 'feature'
created: '2026-10-06'
baseline_revision: '1686810f2bb375eef5845c2e19b361acc8903b1c'
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

**Problème :** le formulaire « Nouveau pret » de `/prets` est inerte et `credits` n'a ni création de prêt, ni outbox, ni Kafka : une demande ne peut être ni enregistrée ni transmise au scoring.

**Approche :** ajouter `POST /api/credits/prets` (validation, prêt EN_ATTENTE et ligne `outbox` du topic `credits.pret-demande.v1` dans une seule transaction, publication Kafka après validation, relais en secours) et brancher le formulaire (soumission, message d'information, rafraîchissement de la liste, erreurs).

## Boundaries & Constraints

**Always :**
- Requête `POST /api/credits/prets`, corps (chaînes) `{"type","montant","tauxAnnuel","dureeMois","objet"}` ; réponse 201 = le prêt (même JSON que `GET /api/credits/prets`, `id` attribué). Tout rôle authentifié ; `clientId` = `sub` du jeton ; sans jeton 401 `{"message":"Authentification requise"}`.
- Validation de la demande, dans cet ordre, 400 `{"message"}` : type absent ou inconnu → `{"message":"Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer."}` (aucun texte du legacy n'existe pour ce cas, rien n'est inventé); puis montant (vide, non numérique ou ≤ 0 → « Montant invalide », garde 17 chiffres entiers de 5.1 conservé) ; taux (hors 0-30 → « Taux invalide », sans plage) ; durée (non entière, ≤ 0 ou > 300 → « Duree invalide », sans plage : 301 à 360 refusés dès la soumission, 300 accepté) ; objet (null, vide ou espaces → « Veuillez indiquer un motif »). Rien n'est écrit en cas de 400 (ni `loans` ni `outbox`). Le simulateur garde ses bornes et ses messages (1-360, avec plage).
- Prêt créé : statut EN_ATTENTE, mensualité / coût total / total intérêts calculés par `Annuite.simuler` comme la simulation, `dateDebut` = 1er du mois de `Horloge.aujourdhui()`, `dateFin` = `dateDebut.plusMonths(dureeMois)`, objet stocké dans `motif`. Objet de plus de 255 caractères : bug du legacy conservé, non corrigé (l'insert échoue → erreur inattendue, rollback).
- Publication : `outbox(id_evenement uuid, topic, cle, charge, publie)` (migration `V2__outbox.sql`, schéma copié de coeur-bancaire) écrite dans la même transaction que le prêt (port `Transaction.executer`) ; clé = `pretId` ; enveloppe `{idEvenement,type:"pret-demande",version:1,horodatage (ISO_INSTANT, port Horloge),producteur:"credits",donnees:{pretId}}`, JSON `StringSerializer`, sans en-tête `__TypeId__`. Après validation (hors transaction) `publier(idLigne)` ; un `@Scheduled(fixedDelay=1000)` republie les lignes restées non publiées. « Transmise » = ligne d'outbox validée : un Kafka injoignable ne fait pas échouer la demande (201).
- Échec d'écriture de la ligne `outbox` : transaction annulée (aucun EN_ATTENTE), 503 `{"message":"Service credit temporairement indisponible, veuillez reessayer plus tard"}`. Autre erreur inattendue à l'enregistrement : 500 `{"message":"Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer."}` (clé `pret.erreur.demande`), sans détail technique ni pile. `Textes.statutPublie` doit publier 503.
- Schéma `contrats/evenements/credits.pret-demande.v1.schema.json` (forme de `coeur-bancaire.temoin-ecrit.v1.schema.json`, `donnees.pretId` entier ≥ 1, exemple) ; bean `NewTopic` 3 partitions / 1 réplica dans `infrastructure` (`ConfigurationEvenements`, `@EnableScheduling`) ; propriétés Kafka copiées de coeur-bancaire (`application.properties`) ; `spring-boot-starter-kafka` et `testcontainers-kafka` (test) au pom ; `compose.yaml` : `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092` et `depends_on` kafka healthy pour `credits`. Aucun consommateur (le scoring est la story 5.3).
- Front : `pret-soumettre` envoie `{type,montant,tauxAnnuel,dureeMois,objet}` (chaînes saisies), désactivé avec `aria-busy` pendant l'envoi ; succès → texte exact « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) » dans `pret-message-info` (nouveau composant d'info `role="status"`), liste rechargée (prêt « En attente d approbation », « En cours de traitement... ») ; erreurs 400 et 503 → `message` du service dans `pret-message-erreur`, valeurs saisies conservées, aucun message d'info ; 500, réseau ou réponse illisible → texte `pret.erreur.demande` ; 401 → purge du jeton et déconnexion. Textes dans `textes.ts` (aucun texte inventé, apostrophe droite pour `pret.erreur.demande`).
- Tags de test : back `@Tag("FR-007-05")`, `FR-007-06`+`BUG-025`, `FR-007-07`, `FR-024-01`, `FR-024-02`+`BUG-077`, `FR-004-05` ; front `[FR-007-05]`, `[FR-007-07]`, `[FR-024-02]`, etc. dans les titres. Test `PretDemandeContratEvenementProducteurTest` (champs exacts de l'enveloppe et de `donnees` contre le schéma, exemple identique, pas de `__TypeId__`).
- `domain`/`application` en Java pur, 100 % lignes/branches ; temps par le port `Horloge` ; couverture et seuils intacts.

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; jamais de rejeu du golden master.
- Pas de scoring, de consommateur Kafka, d'échéancier, de notification, d'audit ni de pipeline (stories 5.3 à 5.6) ; pas de validation de longueur de l'objet ; pas d'appel HTTP entre services ; pas de texte inventé ; pas de nouvelle dépendance npm ; pas de `vitest.config.*`. Ne pas écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| Demande nominale | CONSOMMATION / 10000 / 3.5 / 300 / « Voiture » | 201, prêt EN_ATTENTE, début 1er du mois, fin +300 mois, 1 ligne `outbox` (clé `pretId`) | — |
| Durée 301 puis 360 | reste valide | 400 « Duree invalide » | aucun `loans` ni `outbox` |
| Durée 300 | reste valide | acceptée, `outbox` écrite | — |
| Taux -1 / 31 | reste valide | 400 « Taux invalide » | aucun prêt |
| Durée 0 / 361 | reste valide | 400 « Duree invalide » | aucun prêt |
| Objet vide / espaces / null | reste valide | 400 « Veuillez indiquer un motif » | aucun prêt |
| Montant vide / 0 | reste valide | 400 « Montant invalide » | aucun prêt |
| Panne de l'écriture outbox | demande valide | 503 « Service credit temporairement indisponible, veuillez reessayer plus tard », rollback | pas de message de succès |
| Erreur inattendue (ex. objet > 255) | demande valide | 500 `pret.erreur.demande` | aucun détail technique |
| Kafka injoignable | demande valide | 201, ligne `outbox` non publiée, relayée plus tard | — |
| Sans jeton | POST /prets | 401 | — |

</intent-contract>

## Code Map

- `services/credits/pom.xml` -- ajouter `spring-boot-starter-kafka`, `testcontainers-kafka` (test) ; modèle `services/coeur-bancaire/pom.xml` l.41 et l.72.
- `services/credits/src/main/resources/application.properties` -- bloc Kafka de `services/coeur-bancaire/src/main/resources/application.properties` l.8-22 ; `db/migration/V2__outbox.sql` -- copie de l'`outbox` de coeur-bancaire `V1__temoins_et_outbox.sql`.
- `services/credits/.../domain/` -- `DemandeSimulation.lire` (validation 5.1), `SimulationInvalide.Motif`, `Annuite.simuler`, `Pret` (record sans pré-création : créer `NouveauPret`/`DemandePret` du domaine), `TypePret`, `StatutPret`.
- `services/credits/.../application/` -- `SimulerEmpruntCasUsage` (modèle), `port/out/{PretRepository,Horloge}` ; à créer `port/in/DeposerDemandePret`, cas d'usage, ports `Transaction` et `EvenementsSortants` (modèle `services/coeur-bancaire/.../application/{EcrireTemoinCasUsage,port/out/Transaction,port/out/EvenementsSortants}.java`), exceptions `TransmissionEnEchec` / `DemandePretEnEchec`.
- `services/credits/.../adapter/out/persistance/PretRepositoryJdbc.java` -- ajouter l'insert (`returning id`, `objet`→`motif`, `statut`→`status`) ; créer `TransactionSpring` (copie coeur-bancaire).
- `services/credits/.../adapter/out/evenements/` -- à créer : `EvenementsSortantsKafka`, `LigneOutbox`, `RelaisOutbox`, `EnveloppePretDemande` (copies adaptées de `services/coeur-bancaire/.../adapter/out/evenements/`) ; `infrastructure/ConfigurationEvenements` (`NewTopic`, `@EnableScheduling`) et `AssemblageCredits` (bean du cas d'usage).
- `services/credits/.../adapter/in/http/` -- `PointEntreePrets` (`@PostMapping`, `PretJson` réutilisé, `clientId(Jwt)`), `GestionErreurs` (+ `switch` sur `Motif`, handlers 503/500), `Textes` (nouvelles constantes, `statutPublie` 503).
- `contrats/evenements/credits.pret-demande.v1.schema.json` -- modèle `coeur-bancaire.temoin-ecrit.v1.schema.json`.
- `services/credits/src/test/**` -- `Conteneurs` (+ `KafkaContainer("apache/kafka:4.2.2")`), `OutilsHttp.chargerDonnees` (+ `truncate outbox`), `PretsHttpIT`/`SimulationHttpIT` (modèle IT et panne par `@MockitoSpyBean`), `TemoinEcritContratEvenementTest` et `LecteurTopic` (modèles), `CasUsageTest`, `KafkaInjoignableTest` (modèle coeur-bancaire).
- `compose.yaml` -- service `credits` (~l.262) : Kafka.
- `frontend/src/adapters/ecrans/prets/{SectionDemande,EcranPrets}.tsx` -- formulaire inerte (test `[FR-007-04]` l.239 d'`EcranPrets.test.tsx` à réécrire) ; `SectionSimulateur.tsx` (modèle : `AlerteErreur`, `occupe`, `surDeconnexion`) ; `adapters/ui/AlerteErreur.tsx` (modèle du nouveau composant d'info).
- `frontend/src/application/prets.ts` (`simulerEmprunt`/`listerPrets` en modèle), `domain/pret.ts` (`SaisieDemande`), `adapters/http/ClientPretsHttp.ts` (`deposer`), `adapters/textes.ts` (section `prets`, textes manquants), `adapters/test/rendreApplication.tsx`.
- Sources en lecture seule : `docs/retro/00-global/MESSAGES.md` l.505-521, `docs/retro/features/FEAT-007/RG.md` (RG-007-011 à 019, 028), `FEAT-024/RG.md` (RG-024-001/002/020), `textes-nouveaux.md` l.52.

## Tasks & Acceptance

**Execution :**
- `services/credits/pom.xml`, `application.properties`, `V2__outbox.sql`, `compose.yaml` -- Kafka et table `outbox` -- FR-024-01.
- `services/credits/src/main/java/**` (domain, application, adapter.out.persistance, adapter.out.evenements, adapter.in.http, infrastructure) -- création du prêt, outbox transactionnelle, publication après commit, relais, topic, erreurs -- FR-007-05..07, FR-024-01, FR-024-02.
- `contrats/evenements/credits.pret-demande.v1.schema.json` -- schéma et exemple -- AD-22.
- `services/credits/src/test/**` -- tests unitaires (matrice I/O, domaine et cas d'usage à 100 %), IT HTTP + Testcontainers Postgres/Kafka, panne outbox (rollback), Kafka injoignable, contrat producteur -- tags de la story.
- `frontend/src/{domain,application,adapters,infrastructure}/**` (+ tests) -- soumission, `AlerteInfo` `pret-message-info`, rechargement de liste, erreurs, `aria-busy` -- FR-007-05, FR-007-07, FR-024-02.

**Acceptance Criteria :**
- Given « Consommation », 10000, 3.5, 300, « Voiture », when « Soumettre la demande », then `POST /api/credits/prets` crée un prêt EN_ATTENTE (id renvoyé) et une ligne `outbox` `credits.pret-demande.v1` dans la même transaction ; la page affiche le message d'information et la liste montre « En attente d approbation » / « En cours de traitement... ».
- Given 301 puis 360 mois, then 400 « Duree invalide », rien écrit ; 300 accepté.
- Given taux -1/31, durée 0/361, objet vide/espaces, montant vide/0, then 400 avec les messages exacts, valeurs conservées, aucun prêt.
- Given l'écriture outbox en panne, then rollback, 503 au message `A-024-1`, affiché dans `pret-message-erreur`, pas de message de succès ; erreur inattendue → `pret.erreur.demande`.
- Given le dépôt, then le schéma, le bean `NewTopic` (3 partitions) et `PretDemandeContratEvenementProducteurTest` existent.
- Given `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-05|FR-007-06|FR-007-07|FR-024-01|FR-024-02|BUG-025|BUG-077|FR-004-05'` et `cd frontend && npx vitest run -t 'FR-007-05'` (`FR-007-07`, `FR-024-02`), then code 0 avec au moins un test chacun.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 0, false 4, maybe-false 0 (1 rejeté hors périmètre utile)
- findings:
  - `[false]` `[reject]` panne d'outbox injectée au port `EvenementsSortants` et non à l'adaptateur JDBC — l'espion est dans la transaction du cas d'usage ; le rollback est constaté sur `loans` et `outbox`, ce que l'AC exige.
  - `[false]` `[reject]` aucun test ne relie le front réel au POST réel — l'AC ne demande pas de test de bout en bout ; couvert par `DemandePretHttpIT` (HTTP réel, Postgres, Kafka) et les tests jsdom + `ClientPretsHttp.test.ts`.
  - `[false]` `[reject]` résultats des commandes et des contrôles absents du diff — exécutés après implémentation : `build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh`, `trace-smoke.sh` et les trois `vitest -t` en code 0, `mvn -Dgroups` en code 0 avec chaque tag porté.
  - `[low]` `[reject]` type de prêt inconnu ou absent refusé en 400 avec `pret.erreur.demande` (hors liste des AC) — prévu par la spec, aucun conflit avec les AC, aucun texte inventé.
  - `[false]` `[reject]` ajouts hors AC (`KafkaInjoignableTest`, relais, rôles, 401) — issus de la matrice I/O de la spec, sans effet sur les AC.

## Design Notes

L'écriture de l'outbox se fait dans `Transaction.executer` avec l'insert du prêt ; seule cette ligne échoue en 503, tout le reste en 500. Le bug « objet trop long » est conservé : la contrainte `varchar(255)` provoque l'erreur inattendue (500, rollback). Le type de prêt inconnu ou absent n'a pas de texte du legacy : il est refusé par le message générique `pret.erreur.demande` plutôt que par un texte inventé.

## Verification

**Commands :**
- `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-05|FR-007-06|FR-007-07|FR-024-01|FR-024-02|BUG-025|BUG-077|FR-004-05'` puis `mvn -B -q -f services/credits/pom.xml verify` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-007-05'` (puis `FR-007-07`, `FR-024-02`) -- expected: code 0, au moins un test chacun
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `POST /api/credits/prets` (validation, prêt EN_ATTENTE et ligne `outbox` `credits.pret-demande.v1` dans une transaction, publication Kafka après validation, relais en secours, 503 si l'outbox échoue, 500 `pret.erreur.demande` sinon) et formulaire « Nouveau pret » branché (`pret-message-info`, rechargement de la liste, erreurs, `aria-busy`).

**Fichiers :** `services/credits/**` (domaine `DemandePret`/`NouveauPret`, cas d'usage, ports `Transaction`/`EvenementsSortants`, adaptateurs Kafka/outbox/JDBC, `V2__outbox.sql`, `ConfigurationEvenements`, erreurs HTTP, tests dont `PretDemandeContratEvenementProducteurTest`), `contrats/evenements/credits.pret-demande.v1.schema.json`, `compose.yaml` (Kafka pour credits), `frontend/src/{domain/pret.ts,application/prets.ts,adapters/{ecrans/prets,http/ClientPretsHttp.ts,ui/AlerteInfo.*,textes.ts,test/rendreApplication.tsx}}`.

**Revue :** 0 patch, 0 report, 5 rejets motivés (cf. Review Triage Log). Revue complémentaire recommandée : `false` (patches : high 0, medium 0).

**Vérification :** `mvn verify` credits (167 tests) et `mvn test -Dgroups=…` en code 0, chaque tag porté ; `vitest` (385 tests, seuils) et `vitest -t FR-007-05|FR-007-07|FR-024-02` en code 0 ; `build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh`, `trace-smoke.sh` en code 0.

**Risques résiduels :** `credits.pret-demande.v1` est un topic interne (non inscrit au catalogue AD-22, conforme à la spine) ; `setval` ajouté côté tests seulement pour les identifiants de `reference-data.sql` ; taux de la réponse 201 en 4 décimales comme la liste ; objet de plus de 255 caractères → 500 (bug du legacy conservé).
