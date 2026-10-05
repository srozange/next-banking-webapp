---
title: 'Story 1.4 — Faire traverser les deux services par un flux témoin tracé'
type: 'feature'
created: '2026-10-05'
status: 'done'
baseline_commit: '16e4e37352c977677b3dfcd1d0688e05223d4a93'
baseline_revision: '16e4e37352c977677b3dfcd1d0688e05223d4a93'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/quality-gates.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Les règles de routage du conteneur web (nginx) et du proxy Vite n'ont aucune vérification automatique en dehors du POST témoin de coeur-bancaire.
    evidence: |-
      trace-smoke.sh n'envoie que POST /api/coeur-bancaire/temoin. Retirer le bloc /api/audit de nginx.conf, passer X-Forwarded-For à $proxy_add_x_forwarded_for, ou retirer le bypass du proxy Vite ne ferait échouer aucun contrôle. Les curl /tableau-de-bord (200) et /api/audit/ (404) des critères sont manuels. Piste : un test Vitest qui importe vite.config.ts et vérifie server.proxy (bypass compris) ; côté nginx, une porte au niveau de la pile (scripts/gates/, protégé : décision humaine).
    location: >-
      frontend/nginx.conf ; frontend/vite.config.ts
    severity: low
  - summary: >-
      AGENTS.md porte encore « TODO après la story 1.4 : ajouter scripts/gates/trace-smoke.sh à la liste ci-dessus ».
    evidence: |-
      La liste « Avant de déclarer un travail terminé » d'AGENTS.md ne cite pas trace-smoke.sh. Fichier de contexte des agents : modification réservée (report obligatoire), à faire au prochain rafraîchissement (bmad-project-context).
    location: >-
      AGENTS.md (section Running and verifying)
    severity: low
---

<intent-contract>

## Intent

**Problem:** Rien ne prouve aujourd'hui que le contexte de trace se propage sur HTTP, JDBC et Kafka entre deux services : les deux services n'ont ni base, ni Kafka, ni web, ni pile locale, et `scripts/gates/trace-smoke.sh` manque (`check-gates-exist.sh` en `KO`).

**Approach:** Flux témoin `POST /api/coeur-bancaire/temoin` → table `temoins` + ligne `outbox` (même transaction) → publication `KafkaTemplate` après validation (même fil) + relais planifié → `coeur-bancaire.temoin-ecrit.v1` → écouteur unitaire de `notifications` → `temoins_recus` + `evenements_traites` (même transaction). Le tout lancé par `compose.yaml` (2 bases, Kafka, collecteur, Jaeger v2, `web`), tracé sans span manuel, et vérifié dans Jaeger par `trace-smoke.sh`.

## Boundaries & Constraints

**Always:**
- Versions d'`epic-1-context.md` uniquement ; `datasource-micrometer-spring-boot` 2.3.0 en propriété du POM parent ; starters Boot 4 `spring-boot-starter-webmvc`, `-jdbc`, `-flyway` + `flyway-database-postgresql`, `-kafka`, `-actuator`, `-opentelemetry` ; tests : `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-kafka` (BOM).
- Propriétés explicites dans chaque `application.properties` : `management.opentelemetry.tracing.export.otlp.endpoint=http://otel-collector:4318/v1/traces`, `management.tracing.sampling.probability=1.0`, `spring.kafka.template.observation-enabled=true`, `spring.kafka.listener.observation-enabled=true`, `management.endpoints.web.exposure.include=health`, motif de journal `logging.pattern.correlation` portant `traceId=` et `spanId=`.
- Hexagone : `domain`/`application` en Java pur ; port `application.port.out.Transaction` (`executer`, `executerIndependamment`, sur `Supplier`) implémenté dans `adapter.out.persistance` ; aucune annotation transactionnelle dans `application` ; horodatage de l'enveloppe par le port `Horloge` ; aucun `now()`/`CURRENT_TIMESTAMP` dans les migrations.
- Enveloppe JSON `idEvenement` (UUID), `type` (`temoin-ecrit`), `version` (1), `horodatage` (ISO 8601 UTC), `producteur` (`coeur-bancaire`), `donnees.temoinId` ; clé = `temoinId` ; `StringSerializer`/`StringDeserializer`, aucun en-tête `__TypeId__` ; `group.id=notifications` ; écouteur unitaire (`spring.kafka.listener.type=single`).
- Bean `NewTopic` (3 partitions, réplication 1) dans `infrastructure` de `coeur-bancaire`.
- Secrets : `${NB_COEUR_BANCAIRE_BD_MOT_DE_PASSE:-}` / `${NB_NOTIFICATIONS_BD_MOT_DE_PASSE:-}` ; un service sans mot de passe de base refuse de démarrer (contrôle au démarrage, avant la `DataSource`).
- Couverture : seuils de la 1.3 atteints par des tests (Testcontainers `postgres:18.6`, `apache/kafka:4.2.2`).
- Seul fichier protégé créé : `scripts/gates/trace-smoke.sh`.

**Never:**
- Span créé à la main (`Observation.createNotStarted|start`, `@Observed`, `@NewSpan`, `spanBuilder(`, `nextSpan(`) ; `management.otlp.tracing.export.enabled` ; écouteur batch ; appel HTTP entre services ; image `jaegertracing/all-in-one` ; `${VAR:?}`.
- Modifier un autre fichier protégé, un seuil, `sprint-status.yaml` ; rejouer le golden master ; vérification du JWT (epic 2) ; PIT/Stryker (1.5) ; CVE (1.6).
- Texte d'interface inventé : erreurs du point témoin sans corps (statut seul).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Écriture | `POST /api/coeur-bancaire/temoin` `{"libelle":"x"}` | 201 `{"temoinId":n}` ; ligne `temoins`, ligne `outbox` publiée | — |
| Libellé vide/absent | `{"libelle":" "}` | 400 sans corps, ni témoin ni outbox | — |
| Kafka indisponible à la publication | send en échec | 201 quand même ; ligne `outbox` non publiée, republiée par le relais | erreur journalisée, relais s'arrête à la première ligne en échec (ordre) |
| Transaction annulée | exception après écriture | aucune ligne `temoins` ni `outbox` | — |
| Doublon | même enveloppe livrée deux fois | une seule ligne `temoins_recus`, une `evenements_traites` | second ignoré |
| Mot de passe absent | `spring.datasource.password` vide | démarrage refusé | `IllegalStateException` |

</intent-contract>

## Code Map

- `pom.xml` -- POM parent (Boot 4.1.1, JaCoCo) : ajouter propriété et `dependencyManagement` de `datasource-micrometer-spring-boot` 2.3.0. Les modules héritent de JaCoCo.
- `services/<s>/pom.xml` -- seulement `spring-boot-starter` + test + ArchUnit : ajouter starters et dépendances de test.
- `services/coeur-bancaire/.../adapter/in/PointEntreeTemoin.java` -- `@Component` public appelé par les tests ; devient l'adaptateur HTTP (reste public : `canary-arch.sh` cherche un type public sous `adapter`).
- `services/coeur-bancaire/.../adapter/out/DepotTemoinsEnMemoire.java`, `services/notifications/.../adapter/out/RegistreTemoinsRecusEnMemoire.java` -- annoncés « remplacés par la persistance en 1.4 » : à supprimer au profit de `adapter.out.persistance`.
- `services/coeur-bancaire/.../application/EcrireTemoinCasUsage.java`, `port/in/EcrireTemoin.java`, `port/out/DepotTemoins.java` ; `domain/Temoin.java` (record libellé obligatoire) -- à étendre (identifiant, événement, transaction).
- `services/notifications/.../adapter/in/RecepteurTemoin.java`, `application/RecevoirTemoinCasUsage.java`, `port/in/RecevoirTemoin.java`, `domain/TemoinRecu.java` (identifiant > 0) -- à brancher sur Kafka avec idempotence.
- `services/<s>/.../adapter/out/horloge/HorlogeSysteme.java` -- port `Horloge` (`instant()`), à réutiliser pour `horodatage`.
- `services/<s>/src/test/.../infrastructure/Application*Test.java` + `ContexteDemarre.java` -- tests de contexte et de `main` (via `--spring.main.sources`) : à adapter (base et Kafka par Testcontainers, arguments de connexion passés à `main`).
- `services/<s>/src/test/.../*ArchitectureTest.java` -- inchangés ; `ClientsHttpArchitectureTest` ignore les classes de test.
- `frontend/vite.config.ts` -- plugin React + `test.coverage` ; ajouter `server.proxy`. `tsconfig.json` inclut `vite.config.ts`, sans `@types/node` (`skipLibCheck`).
- `scripts/gates/check-gates-exist.sh` (lecture seule) -- lit `docker compose config --services` : `otel|collector`, `jaeger`, nom de chaque service ; `scripts/gates/trace-smoke.sh` exécutable.
- `scripts/gates/build.sh` (lecture seule) -- `mvn -B -q clean verify -DfailIfNoTests=true` : les tests Testcontainers y tournent (Docker présent).
- `docs/tests-des-controles.tsv` -- registre des essais des scripts de contrôle (date, script, sha256 tronqué à 16, niveau, résultat, commit, note).
- `_bmad-output/planning-artifacts/architecture/.../versions-verifiees.md` -- consigner `node:24.21.0-alpine` (présent, HTTP 200 Docker Hub) et `spring-boot-starter-flyway` 4.1.1 / `flyway-database-postgresql` (BOM 4.1.1, Flyway 12.4.0), vérifiés le 2026-10-05.
- Images : collecteur sans shell (healthcheck par `/otelcol validate`), Jaeger avec `wget` (extension santé 13133), `eclipse-temurin:21(-jre)` avec `curl`.

## Tasks & Acceptance

**Execution:**
- [x] `pom.xml`, `services/<s>/pom.xml` -- dépendances ci-dessus -- tracing, JDBC, Kafka, web.
- [x] `services/coeur-bancaire/src/main/...` -- domaine `TemoinEcrit` ; ports `Transaction`, `EvenementsSortants` (`consigner`, `publier`), `DepotTemoins` rendant l'identifiant ; cas d'usage (transaction : témoin + outbox, puis publication) ; adaptateurs `adapter.in.PointEntreeTemoin` (REST), `adapter.out.persistance` (dépôt JDBC, transaction), `adapter.out.evenements` (outbox, enveloppe, publication `KafkaTemplate`, relais `@Scheduled` 1 s par ordre d'identifiant) ; `infrastructure` (`@EnableScheduling`, `NewTopic`, contrôle du mot de passe) ; `db/migration/V1__*.sql` ; `application.properties`.
- [x] `services/notifications/src/main/...` -- ports `Transaction`, `EvenementsTraites` ; cas d'usage idempotent ; `adapter.in.RecepteurTemoin` (`@KafkaListener`, lecture de l'enveloppe) ; `adapter.out.persistance` ; contrôle du mot de passe ; migration ; propriétés.
- [x] `services/<s>/src/test/...` -- tests unitaires (domaine, application à 100 %) ; intégration Testcontainers : POST bout en bout, transaction annulée → aucune ligne `outbox`, relais, échec de publication, double livraison brute → un seul effet ; `*ContratEvenement*Test` producteur (champs exacts contre le schéma) et consommateur (désérialise l'exemple) ; démarrage refusé sans mot de passe.
- [x] `contrats/evenements/coeur-bancaire.temoin-ecrit.v1.schema.json` -- schéma JSON avec `examples`.
- [x] `services/<s>/Dockerfile`, `frontend/Dockerfile`, `nginx.conf`, `.dockerignore` -- images (`eclipse-temurin:21` → `21-jre`, Maven 3.9.16 téléchargé avec somme de contrôle ; `node:24.21.0-alpine` → `nginx:1.30.5-alpine`, écoute 8080).
- [x] `compose.yaml`, `config/otelcol/config.yaml` -- pile complète, healthchecks, `TZ`, réseau `172.30.0.0/24`, volumes `pgdata-<id>`, ports 8080/16686/8082/8085.
- [x] `Makefile`, `.env.example`, `.gitignore` -- `make secrets` (`.env` sans écraser l'existant, paire RSA dans `.secrets/`).
- [x] `frontend/vite.config.ts` -- proxy `/api/{identifiant}/` (sauf audit) avec `X-Forwarded-For` écrasé.
- [x] `scripts/gates/trace-smoke.sh` -- démarre la pile si aucun conteneur ne tourne ; POST par `web` ; interroge Jaeger ; lignes `OK`/`KO` ; échec si manque : deux services, span JDBC, émission Kafka, consommation Kafka.
- [x] `versions-verifiees.md`, `docs/tests-des-controles.tsv` -- versions vérifiées ; essais de `trace-smoke.sh`.

**Acceptance Criteria:**
- Given le dépôt sans `.env`, when `docker compose -f compose.yaml config --services`, then code 0, les 8 services attendus, `grep -c '^postgres-'` = 2, aucun service sans healthcheck, pas d'`all-in-one`, images AR-53, `TZ=Europe/Paris` partout, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` ; la commande `awk` de `SPRING_DATASOURCE_URL` n'affiche rien.
- Given `make secrets`, when `git ls-files .env .secrets`, then vide, `.env.example` versionné sans valeur.
- Given `spring.datasource.password` vide, when le service démarre, then échec (test, et conteneur non nul).
- Given la configuration, when `! grep -rnE 'Observation\.createNotStarted|Observation\.start|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main` et `! grep -rn 'management.otlp.tracing' services`, then code 0.
- Given `temoinId` renommé dans l'enveloppe produite, when `mvn -B -q -f services/coeur-bancaire/pom.xml verify`, then code non nul.
- Given la pile démarrée, when `curl` `http://localhost:8080/tableau-de-bord` et `/api/audit/`, then `200` et non-2xx.
- Given la story terminée, when `scripts/gates/trace-smoke.sh`, then code 0 ; avec `SPRING_KAFKA_LISTENER_OBSERVATION_ENABLED=false` sur `notifications` (pile reconstruite), code non nul et `KO` nommant le span de consommation ; avec `notifications` arrêté, code non nul.
- Given la story terminée, when `build.sh`, `golden-master.sh`, `check-gates-exist.sh`, then code 0 pour les deux premiers ; les 5 lignes compose/trace-smoke en `OK` ; diff des fichiers protégés limité à `scripts/gates/trace-smoke.sh`.

## Spec Change Log

## Review Triage Log

### 2026-10-05 — Review pass
- verdicts: 34 findings — high 0, medium 2, low 22, false 10, maybe-false 0
- findings:
  - `[low]` `[reject]` (blind) Ordre non garanti : la publication directe d'une ligne N passe avant une ligne N-1 bloquée — voulu par AD-18 (publication dans le fil + relais de secours) ; l'ordre par agrégat tient par la clé ; le relais, lui, garde l'ordre.
  - `[low]` `[reject]` (blind) Le relais et la publication directe peuvent envoyer la même ligne deux fois — doublon absorbé par `evenements_traites` (AD-18), décrit dans les Design Notes ; un verrou ajouterait de la complexité.
  - `[low]` `[reject]` (blind) `outbox` et `evenements_traites` jamais purgées — volume négligeable pour le témoin ; une purge est un nouveau traitement, pas une correction directe.
  - `[low]` `[reject]` (blind) Message illisible ou sans `donnees` : exception, tentatives par défaut puis saut sans file de rebut — seul coeur-bancaire produit ce topic ; le gestionnaire par défaut journalise l'abandon ; une file de rebut est une politique transverse à décider.
  - `[low]` `[patch]` (blind) `@ExceptionHandler` large et Javadoc « une erreur répond sans corps » inexacte (405/415/500 gardent le corps Spring) — Javadoc corrigée : seule une demande refusée (400) répond sans corps ; la conversion d'une `IllegalArgumentException` interne en 400 n'est pas atteignable (identité SQL > 0).
  - `[low]` `[patch]` (blind) `trace-smoke.sh` peut valider une pile démarrée avec d'anciennes images — ligne `..  pile déjà démarrée : images non reconstruites` ajoutée ; la logique de démarrage reste celle du critère (sinon le test négatif « notifications arrêté » repasserait).
  - `[low]` `[reject]` (blind) Le bilan ne vérifie pas quel service porte quel span — le script vérifie exactement ce que demande le critère (deux services, JDBC, émission, consommation sous une même trace).
  - `[low]` `[reject]` (blind) Ports de web, Jaeger et du proxy Vite non paramétrables — 8080 et 16686 sont imposés par les critères et AD-17 ; conflit propre au poste.
  - `[false]` `[reject]` (blind) Utiliser `${VAR:?}` pour les mots de passe — interdit explicitement par le critère et AD-6.
  - `[low]` `[reject]` (blind) Healthcheck du collecteur par `otelcol validate` — l'image n'a ni shell ni client HTTP : aucune sonde de vivacité possible de l'intérieur ; limite écrite dans le commentaire de compose.yaml.
  - `[low]` `[reject]` (blind) Lignes du registre avec le commit 16e4e37, qui ne contient pas le script — la colonne sha256 identifie la version essayée ; le /16 cité est bien celui du réseau local en conflit.
  - `[low]` `[defer]` (blind) TODO d'AGENTS.md (ajouter trace-smoke.sh à la liste) non fait — fichier de contexte des agents : reporté (DW).
  - `[false]` `[reject]` (blind) `executerIndependamment` sans appelant — exigé par AD-1 et par la section Always du spec ; servira à l'audit (AD-18).
  - `[false]` `[reject]` (blind) Code dupliqué entre services, propriétés producteur/écouteur superflues — duplication voulue (aucune bibliothèque partagée entre services, AD-17) ; aucune nuisance nommée.
  - `[false]` `[reject]` (blind) `/api/coeur-bancaire` sans barre ou `/api/xyz/` renvoie `index.html` — conforme à AD-17 : tout autre chemin sert la SPA.
  - `[low]` `[reject]` (blind) `Makefile` masque stderr d'`openssl genpkey` — stderr ne contient que la barre de progression (vérifié) ; un échec arrête quand même `make` par le code de sortie.
  - `[low]` `[reject]` (edge) Envoi en double de la même ligne par deux fils — même cause et même motif que le doublon ci-dessus.
  - `[low]` `[reject]` (edge) Lignes publiées hors ordre d'identifiant — même motif que l'ordre ci-dessus.
  - `[low]` `[reject]` (edge) Erreur base dans `publier` après validation → 500 alors que le témoin est écrit — base validée l'instant d'avant, cas improbable ; ajouter un garde n'est pas une correction directe.
  - `[low]` `[patch]` (edge) Le relais charge tout l'arriéré à chaque seconde — `limit 100` ajouté à `nonPubliees()` (ordre conservé).
  - `[low]` `[reject]` (edge) Panne brève de la base de notifications : 10 tentatives sans délai puis événement perdu — réel, mais sans effet sur le flux témoin ; la politique de reprise (délai, rebut) concerne tous les futurs consommateurs : signalé dans les risques résiduels.
  - `[low]` `[reject]` (edge) Enveloppe sans `donnees` ou UUID invalide → NPE, tentatives inutiles, abandon — même motif que le message illisible.
  - `[false]` `[reject]` (edge) L'API v3 de Jaeger renverrait la trace en morceaux concaténés — vérifié : `curl /api/v3/traces/<id> | jq -s length` = 1, un seul objet de 16 spans.
  - `[false]` `[reject]` (edge) Un échec de `jq` sur une interrogation suivante efface le bilan — un attribut OTLP a toujours une clé (`KeyValue.key` obligatoire) : `startswith` ne voit jamais `null`.
  - `[low]` `[reject]` (edge) Ports 8080/16686 déjà pris sur le poste — même motif que les ports figés.
  - `[false]` `[reject]` (edge) Proxy Vite vers 8082/8085 alors que le service écoute 8080 — AD-17 : ce sont les ports publiés par les conteneurs (8080 dedans) ; hors compose, le service ne joint de toute façon pas sa base.
  - `[low]` `[reject]` (edge) `openssl` absent → `.env` écrit avec des mots de passe vides — `openssl` est présent sur tout poste de dev visé ; ajouter un garde n'est pas une correction directe ; le service refuse alors de démarrer, nommément.
  - `[medium]` `[patch]` (edge) `EchecPublicationTest` restubbe le mock pendant que le relais l'appelle (test instable) — un seul stub, réponse pilotée par un `AtomicBoolean` basculé au lieu d'un second `when(...)`.
  - `[false]` `[reject]` (edge) nginx : chemin `/api/...` hors liste → SPA 200 — conforme à AD-17 (même constat que côté blind).
  - `[medium]` `[patch]` (verification-gap) L'annulation de `transaction.executer` dans notifications n'est pas testée — test `une_transaction_annulee_ne_marque_pas_l_evenement` ajouté à `ReceptionTemoinTest` (aucune ligne `evenements_traites` ni `temoins_recus`).
  - `[low]` `[defer]` (verification-gap) Routage nginx et proxy Vite sans vérification automatique — reporté comme proposé (DW) ; les curl des critères ont été rejoués à la main dans cette passe (200 et 404).
  - `[false]` `[reject]` (intent) `trace-smoke.sh` lancé seulement avec port et sous-réseau remplacés — la configuration par défaut est valide (`docker compose config` code 0) ; le conflit est propre au poste.
  - `[low]` `[reject]` (intent) Le script porte son propre `traceparent` : il ne prouve pas que le service ouvre la trace, et un échantillonnage < 1.0 passerait — l'échantillonnage à 1.0 est vérifié par la configuration (critère) ; retrouver la trace autrement est une refonte du script.
  - `[false]` `[reject]` (intent) Les preuves sur la pile ne sont que des notes de l'agent — rejouées dans cette passe : `trace-smoke.sh` code 0, test négatif `notifications` arrêté en KO, curl 200/404.

### 2026-10-05 — Review pass (revue complémentaire)
- verdicts: 44 findings — high 2, medium 0, low 31, false 11, maybe-false 0
- findings:
  - `[low]` `[reject]` (blind) Ordre non garanti entre publication directe et relais ; une ligne toujours en échec bloque les suivantes — carried : voulu par AD-18 et par la matrice (« relais s'arrête à la première ligne en échec (ordre) »).
  - `[low]` `[reject]` (blind) Publication concurrente (fil de la requête, relais, plusieurs instances) sans `for update skip locked` — carried : doublon absorbé par `evenements_traites` (AD-18).
  - `[low]` `[reject]` (blind) L'outbox ne garde pas le `traceparent` : un événement republié par le relais ouvre une nouvelle trace — réel, mais l'intention ne demande la trace que sur le flux nominal ; restaurer le contexte demanderait une colonne et une reprise manuelle du contexte (proche du span manuel interdit), pas une correction directe.
  - `[low]` `[reject]` (blind) Outbox sans purge ni `publie_le` — carried : volume négligeable pour le témoin ; une purge est un nouveau traitement.
  - `[low]` `[reject]` (blind) Message illisible ou sans `donnees` : nouveaux essais puis abandon, sans file de rebut — carried : politique transverse à décider.
  - `[low]` `[reject]` (blind) Tests de contrat sans validateur JSON Schema ; tolérance aux champs inconnus non prouvée — les formats sont vérifiés (`UUID.fromString`, `Instant.parse`, `isIntegralNumber`, exemple égal au produit) ; un validateur est une nouvelle dépendance, pas une correction directe ; la tolérance n'est pas demandée.
  - `[false]` `[reject]` (blind) `EchecPublicationTest` annonce l'ordre sans le vérifier, `update outbox` global, attente fixe — `times(1)` sur la seconde ligne prouve que le relais ne dépasse jamais une ligne plus ancienne en échec : c'est l'ordre annoncé ; les classes de test tournent l'une après l'autre ; `atLeast(3)` compte aussi la tentative directe.
  - `[low]` `[reject]` (blind) `trace-smoke.sh` peut valider une pile périmée ou à moitié démarrée — carried : ligne d'information déjà ajoutée ; même cause pour la pile partielle.
  - `[low]` `[patch]` (blind) `trace-smoke.sh` vérifie moins qu'il n'annonce ; message « après 60 s » faux quand Jaeger ne répond pas (jusqu'à 30 × (2 s + 10 s)) — carried pour l'attribution des spans (reject) ; message corrigé en « après 30 essais espacés de 2 s », essais rejoués (registre).
  - `[low]` `[reject]` (blind) Ports 8080 et 16686 figés — carried : imposés par les critères et AD-17.
  - `[false]` `[reject]` (blind) `${VAR:?}` pour les mots de passe — carried : interdit par le critère et AD-6.
  - `[low]` `[reject]` (blind) Healthcheck du collecteur par `otelcol validate` — carried : l'image n'a ni shell ni client HTTP ; l'extension `health_check` n'y change rien sans client dans l'image.
  - `[false]` `[reject]` (blind) nginx sert `index.html` pour un `/api/...` inconnu — carried : conforme à AD-17.
  - `[low]` `[reject]` (blind) `@ExceptionHandler(IllegalArgumentException)` large ; libellé sans borne de longueur — carried pour le gestionnaire (Javadoc corrigée à la passe précédente) ; aucune borne de longueur n'est demandée, en ajouter une inventerait une règle.
  - `[low]` `[reject]` (blind) `UUID.randomUUID()` dans l'application sans port — `java.*` est autorisé dans l'application ; la règle de port ne vise que le temps ; un port générateur est une nouvelle surface pour un gain de test seul.
  - `[false]` `[reject]` (blind) `executerIndependamment` sans appelant — carried : exigé par AD-1 et la section Always.
  - `[low]` `[reject]` (blind) Nom du topic écrit en trois endroits — une divergence ferait échouer `FluxTemoinTest` (lecture réelle du topic) ; partager la constante exigerait de la rendre publique hors de son paquet.
  - `[high]` `[patch]` (blind) Le POST bloque quand Kafka est indisponible — même cause que le constat de vérification ci-dessous : vérifié, environ 65 s. Correctif ci-dessous.
  - `[low]` `[defer]` (blind) Documentation : TODO d'AGENTS.md ; ligne « à confirmer à la story 1.4 » de `versions-verifiees.md` — carried pour AGENTS.md (déjà reporté) ; la ligne suivante de `versions-verifiees.md` consigne explicitement la confirmation : pas de lecteur trompé, rejet.
  - `[false]` `[reject]` (blind) Code copié entre services ; `ControleMotDePasseBase` ne vérifie que le mot de passe — carried : duplication voulue (AD-17) ; le spec ne demande que le contrôle du mot de passe.
  - `[low]` `[reject]` (edge) Erreur base dans `publier` après validation → 500 — carried.
  - `[low]` `[reject]` (edge) Relais et requête lisent la même ligne → double publication — carried.
  - `[low]` `[reject]` (edge) Enveloppe sans `donnees` → NPE, abandon — carried.
  - `[low]` `[reject]` (edge) Base de notifications indisponible plus de 10 essais → événement perdu — carried : risque résiduel, politique de reprise à décider.
  - `[low]` `[reject]` (edge) `openssl` absent → `.env` à mots de passe vides — carried.
  - `[low]` `[patch]` (edge) `Makefile` : clé privée présente mais clé publique absente → « conservé », clé publique jamais régénérée — condition étendue aux deux fichiers (`[ -f prive ] && [ -f public ]`), message « paire RSA de .secrets/ existe déjà : conservée ».
  - `[low]` `[reject]` (edge) `.env` recréé alors que les volumes `pgdata-*` existent → mots de passe divergents — `make secrets` n'écrase jamais `.env` : il faut le supprimer à la main ; cas rare, et le correctif (gestion des volumes) dépasse une correction directe.
  - `[low]` `[reject]` (edge) Ports 8080/16686 sans variable de repli — carried.
  - `[low]` `[patch]` (edge) 415/405 renvoyés avec le corps d'erreur par défaut — carried : Javadoc corrigée à la passe précédente (seule une demande refusée en 400 répond sans corps).
  - `[false]` `[reject]` (edge) `LecteurTopic` : topic absent → `IllegalStateException` — utilitaire de test, le topic est toujours créé par `NewTopic` ; un échec bruyant y est le bon comportement.
  - `[low]` `[reject]` (edge) Ligne récente publiée avant une ligne plus ancienne en échec — carried.
  - `[high]` `[patch]` (verification-gap) Kafka réellement injoignable seulement simulé par un bouchon ; la borne `max.block.ms` n'est pas vérifiée — test `KafkaInjoignableTest` ajouté (vrai `KafkaTemplate`, broker `localhost:1`, POST HTTP). Il a révélé un défaut réel : l'observation de l'envoi demande l'identifiant du cluster à `KafkaAdmin` dans le fil de la requête (30 s d'`operationTimeout`, puis environ 30 s de fermeture du client, puis 5 s de `max.block.ms`). Le POST répondait 201 en environ 65 s, soit au-delà des 60 s de nginx : un 504 pour une écriture validée. Correctif : `spring.kafka.admin.operation-timeout=3s`, `spring.kafka.admin.properties.request.timeout.ms=3000` et `default.api.timeout.ms=3000` dans coeur-bancaire. Le POST répond en environ 8 s ; le test borne la réponse à 12 s et vérifie `publie = false`.
  - `[low]` `[defer]` (verification-gap) Routage nginx et proxy Vite sans vérification automatique — carried : déjà reporté.
  - `[false]` `[reject]` (intent) La preuve de propagation n'existe que dans `trace-smoke.sh`, aucun test du build — l'intention place explicitement la vérification « dans Jaeger par `trace-smoke.sh` ».
  - `[low]` `[reject]` (intent) Liens parent-enfant et service porteur du span JDBC non lus (lectures L2 et L3) — carried : le script vérifie ce que demande le critère.
  - `[low]` `[reject]` (intent) Le script fournit son propre `traceparent` ; `web` n'est pas tracé — carried.
  - `[low]` `[reject]` (intent) Un événement republié par le relais part hors de la trace HTTP — même cause et même motif que le `traceparent` absent de l'outbox.
  - `[high]` `[patch]` (intent) Cas « Kafka indisponible » vérifié au cas d'usage avec un bouchon, pas au point HTTP ni avec un vrai Kafka — même cause que le constat de vérification : couvert par `KafkaInjoignableTest` et par le correctif des délais.
  - `[false]` `[reject]` (intent) « Transaction annulée » vérifiée au port seulement — le cas d'usage fait ses deux écritures dans `transaction.executer` (test unitaire `enregistre_le_temoin_et_l_outbox_dans_la_transaction_puis_publie_apres_validation`) ; le port est vérifié contre une vraie base.
  - `[false]` `[reject]` (intent) Doublon vérifié indirectement, « second ignoré » visible seulement au journal — le test prouve un seul effet pour deux livraisons ; la matrice ne demande pas de journal.
  - `[low]` `[reject]` (intent) Mot de passe vide côté compose vérifié seulement à la main — démarrage refusé testé par Spring ; l'automatiser côté pile demanderait une porte sous `scripts/gates/` (protégé).
  - `[false]` `[reject]` (intent) `management.otlp.metrics.export.enabled=false` attrapé par une recherche large — le critère cherche `management.otlp.tracing` (code 0, rejoué) ; la propriété de métriques n'est pas interdite.
  - `[low]` `[patch]` (intent) `testcontainers-junit-jupiter` déclaré et inutilisé — retiré des deux POM (aucun `@Testcontainers` ni `@Container`).
  - `[false]` `[reject]` (intent) Diff plus large que l'intention (paire RSA, proxy Vite, liste nginx, versions) — ce sont des tâches du spec (AD-17) ; aucune nuisance nommée.

### 2026-10-05 — Review pass (revue complémentaire 2)
- verdicts: 39 findings — high 0, medium 0, low 34, false 5, maybe-false 0
- findings:
  - `[low]` `[reject]` (blind) Publication directe hors ordre quand le relais est bloqué sur une ligne plus ancienne — carried : voulu par AD-18 et par la matrice ; l'ordre par clé tient.
  - `[low]` `[reject]` (blind) Requête et relais peuvent envoyer la même ligne, sans `for update skip locked` — carried : doublon absorbé par `evenements_traites` (AD-18).
  - `[low]` `[reject]` (blind) Message illisible, `donnees` absent ou `temoinId <= 0` : gestionnaire par défaut, sans file de rebut — carried : politique transverse à décider.
  - `[low]` `[reject]` (blind) `@ExceptionHandler(IllegalArgumentException)` transforme une erreur interne en 400 — carried : Javadoc corrigée, cas non atteignable (identité SQL > 0).
  - `[low]` `[reject]` (blind) Point témoin ouvert à tous par `web`, libellé sans borne — l'intention exige le POST par `web` ; l'authentification (JWT) est exclue jusqu'à l'epic 2 ; aucune borne de longueur demandée (carried).
  - `[low]` `[reject]` (blind) `outbox` jamais purgée — carried : une purge est un nouveau traitement.
  - `[low]` `[reject]` (blind) Tests de contrat sans validateur JSON Schema — carried : formats vérifiés un à un ; un validateur est une nouvelle dépendance.
  - `[low]` `[reject]` (blind) `trace-smoke.sh` valide une pile périmée, laisse la pile démarrée, ports figés — carried pour la pile périmée et les ports ; laisser la pile tourner est voulu (démarrer la pile si besoin, critère).
  - `[low]` `[reject]` (blind) `trace-smoke.sh` ne vérifie ni le service porteur de chaque span ni la filiation, nom de span en minuscules — carried : le script vérifie ce que demande le critère.
  - `[low]` `[reject]` (blind) Healthcheck du collecteur par `otelcol validate` — carried : l'image n'a ni shell ni client HTTP.
  - `[low]` `[reject]` (blind) Le serveur Vite sert la SPA (200) pour `/api/audit/**` là où nginx répond 404 — le spec demande le proxy « sauf audit » et seul le conteneur `web` porte le 404 (AD-17) ; aligner Vite demanderait une règle de proxy nouvelle, pas une correction directe ; la vérification est déjà reportée (DW-4).
  - `[low]` `[reject]` (blind) `.env` absent masqué par `${VAR:-}` ; `.env.example` sans les variables de port et de sous-réseau ; `make` et `openssl` non vérifiés par `trace-smoke.sh` — carried pour `${VAR:?}` (interdit par AD-6) ; les variables de port ont une valeur par défaut valide ; `make secrets` échoue nommément (« make secrets a échoué »).
  - `[low]` `[reject]` (blind) Nom du topic écrit en trois endroits — carried : une divergence ferait échouer `FluxTemoinTest`.
  - `[false]` `[reject]` (blind) `executerIndependamment` sans appelant, code copié entre services, propriétés producteur dans notifications — carried : exigé par AD-1 ; duplication voulue (AD-17).
  - `[low]` `[defer]` (blind) Registre : colonne commit, note « /16 » ; TODO d'AGENTS.md — carried : rejet pour le registre, AGENTS.md déjà reporté (DW-5), aucun nouveau report.
  - `[low]` `[reject]` (edge) Erreur base dans `publier` après validation → 500 — carried.
  - `[low]` `[reject]` (edge) `IllegalArgumentException` d'une couche profonde → 400 sans corps — carried.
  - `[low]` `[reject]` (edge) Lignes publiées hors ordre d'identifiant — carried.
  - `[low]` `[reject]` (edge) Une ligne en échec permanent bloque toutes les suivantes — carried : comportement exigé par la matrice (« relais s'arrête à la première ligne en échec (ordre) »).
  - `[low]` `[reject]` (edge) Même ligne lue par plusieurs instances ou par la requête et le relais — carried.
  - `[low]` `[reject]` (edge) Base de notifications indisponible : 10 essais sans délai puis événement perdu — carried : risque résiduel, politique de reprise à décider.
  - `[low]` `[reject]` (edge) Enveloppe illisible ou sans `donnees` → NPE, abandon — carried.
  - `[low]` `[reject]` (edge) Conteneur Kafka recréé seul (sans volume) : topic perdu, `NewTopic` non rejoué — réel mais rare : `up --build` ne recrée Kafka que si sa configuration change, et `down` puis `up` recrée aussi coeur-bancaire, donc le topic ; un volume et une politique de redémarrage dépassent une correction directe.
  - `[low]` `[patch]` (edge) `Makefile` : clé privée présente et clé publique absente → la paire est régénérée et la clé privée écrasée, contre « Rien n'est écrasé » (défaut introduit par le correctif de la passe précédente) — clé privée et clé publique traitées séparément : une clé publique manquante est dérivée de la clé privée conservée ; une clé privée régénérée entraîne sa clé publique (paire toujours cohérente) ; commentaire d'en-tête précisé. Quatre cas rejoués dans un répertoire temporaire.
  - `[low]` `[reject]` (edge) `openssl` absent → `.env` à mots de passe vides — carried.
  - `[low]` `[reject]` (edge) Ports 8080 et 16686 sans variable de repli — carried.
  - `[low]` `[reject]` (edge) `trace-smoke.sh` interroge les ports par défaut — même cause et même motif que les ports figés.
  - `[false]` `[reject]` (edge) `LecteurTopic` : topic absent → NPE, test instable — carried : le topic est toujours créé par `NewTopic` ; un échec bruyant y est le bon comportement.
  - `[low]` `[defer]` (verification-gap) Routage nginx et proxy Vite sans vérification automatique — carried : déjà reporté (DW-4), aucun nouveau report ; curl rejoués dans cette passe (200 et 404).
  - `[low]` `[reject]` (verification-gap) `trace-smoke.sh` évalue une pile déjà démarrée sans la reconstruire — carried : ligne d'information ajoutée ; reconstruire systématiquement casserait le test négatif « notifications arrêté » ; fichier protégé.
  - `[low]` `[reject]` (intent) Span JDBC de notifications non exigé : celui de coeur-bancaire suffit — carried (lectures L2 et L3).
  - `[low]` `[reject]` (intent) Consommation non rattachée à notifications ni à l'émission par filiation — carried.
  - `[false]` `[reject]` (intent) Aucun test Java n'observe le tracing — carried : l'intention place la vérification dans Jaeger par `trace-smoke.sh`.
  - `[low]` `[reject]` (intent) « Kafka indisponible » vérifié en deux moitiés (vrai template sans republication, bouchon avec republication), « erreur journalisée » non affirmée — les deux moitiés passent par le même `publier` ; l'erreur est journalisée (`EvenementsSortantsKafka`, `JOURNAL.error` lignes 72 et 75) ; affirmer un journal n'est pas demandé par les critères.
  - `[low]` `[reject]` (intent) Ordre global (lecture D2) non tenu par la publication directe — carried : lecture D1 voulue (AD-18).
  - `[low]` `[reject]` (intent) Pile sans mot de passe : PostgreSQL échoue avant le service, refus du service non observé sous compose — carried : refus testé par Spring ; l'automatiser côté pile demanderait une porte protégée.
  - `[low]` `[reject]` (intent) 400 vérifié en appel direct, pas par nginx — nginx transmet le statut tel quel ; le passage par `web` est prouvé par le POST de `trace-smoke.sh`.
  - `[false]` `[reject]` (intent) Doublon vérifié indirectement — carried.
  - `[false]` `[reject]` (intent) Transaction annulée vérifiée aux ports — carried.

## Design Notes

- Publication après validation : le cas d'usage reçoit l'identifiant de la ligne `outbox` en sortie de `transaction.executer(...)`, puis appelle `evenements.publier(id)` hors transaction, dans le fil de la requête (le span d'émission reste sous la trace HTTP). `publier` attrape l'échec et le journalise : la ligne reste au relais. Doubles publications possibles (relais concurrent) : absorbées par `evenements_traites`.
- Idempotence : `insert into evenements_traites ... on conflict do nothing` ; 0 ligne insérée → événement ignoré. `temoins_recus` sans clé unique sur `temoin_id`, pour que le test de doublon soit significatif.
- Démarrage refusé : `BeanFactoryPostProcessor` statique dans `infrastructure`, qui lit `spring.datasource.password` dans l'`Environment` et lève avant la création de toute `DataSource`.
- `trace-smoke.sh` : horodatage avant le POST, puis interrogation répétée (≤ 60 s) de `GET :16686/api/traces?service=coeur-bancaire&start=…` ; spans repérés par `span.kind` (`producer`/`consumer`) et par les spans de datasource-micrometer (balises `jdbc.*`). « Pile démarrée » = au moins un conteneur du projet en marche ; sinon `make secrets` si `.env` manque, puis `up -d --build --wait`.
- nginx : `location /api/audit/` → 404 ; regex sur la liste fermée des identifiants (AD-14) → `proxy_pass http://$service:8080` avec `resolver 127.0.0.11` (URI transmise telle quelle) ; `location /` → `try_files ... /index.html`.

## Verification

**Commands:**
- `scripts/gates/build.sh` -- expected: code 0.
- `make secrets && docker compose -f compose.yaml up -d --build --wait && scripts/gates/trace-smoke.sh` -- expected: code 0, lignes `OK`.
- `scripts/gates/check-gates-exist.sh` -- expected: lignes compose et trace-smoke en `OK` (KO restants : 1.5, 1.6).
- `scripts/gates/golden-master.sh` -- expected: code 0.
- Commandes `docker compose config` / `jq` / `awk` des critères -- expected: comme ci-dessus.
- `git diff --name-only 16e4e37352c977677b3dfcd1d0688e05223d4a93 -- scripts/gates .bmad-loop .claude docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` -- expected: seulement `scripts/gates/trace-smoke.sh` (non suivi avant commit : `git status`).
- Tests négatifs (temporaires puis retirés) -- expected: code non nul.

## Auto Run Result

Status: done

**Résumé (revue complémentaire 2) :** nouvelle passe de revue sur le flux témoin tracé de la story 1.4 : `POST /api/coeur-bancaire/temoin`, puis la table `temoins` et l'outbox, puis Kafka, puis `temoins_recus` dans `notifications`, sous `compose.yaml`, vérifié dans Jaeger par `trace-smoke.sh`. Le seul défaut nouveau est dans `make secrets`, introduit par le correctif de la passe précédente. Quand seule la clé publique manquait, la cible régénérait la paire RSA entière et écrasait la clé privée. Défaut corrigé.

**Fichiers modifiés (cette passe) :**
- `Makefile` : clé privée et clé publique traitées séparément. Une clé publique manquante est dérivée de la clé privée conservée. Une clé privée régénérée entraîne sa clé publique. Commentaire d'en-tête précisé.

**Revue :** 39 constats, dont 34 reprennent en tout ou partie des lignes déjà triées aux passes précédentes (`carried`). Répartition : high 0, medium 0, low 34, false 5.
- **Correctif :** un low, la paire RSA du `Makefile`.
- **Aucun nouveau report.** Les deux reports existants sont repris tels quels :
  - DW-4 : le routage nginx et le proxy Vite n'ont aucune vérification automatique ;
  - DW-5 : le TODO d'AGENTS.md demande d'ajouter `trace-smoke.sh` à la liste des contrôles.
- **Rejets :** motifs au journal de tri. Ce sont surtout des comportements voulus (AD-17, AD-18, la matrice). Trois rejets sont nouveaux :
  - Kafka recréé seul, sans volume : rare, et le correctif dépasse une correction directe ;
  - Vite sert la SPA sur `/api/audit/**` : c'est le proxy « sauf audit » demandé ;
  - le point témoin est ouvert par `web` : le JWT relève de l'epic 2.
- **Correctifs par verdict :** high 0, medium 0, low 1.
- **Nouvelle revue recommandée : non.** Cette passe complémentaire n'a corrigé aucun high : le travail a convergé.

**Vérification (cette passe, après le correctif) :**
- `make secrets` dans un répertoire temporaire, quatre cas, code 0 pour chacun :
  - aucun fichier : tout est généré ;
  - clé publique absente : la clé privée est conservée (même sha256), la clé publique est dérivée et cohérente ;
  - clé privée absente : une paire cohérente est régénérée ;
  - tout présent : rien ne change.
- `scripts/gates/build.sh` : code 0, couverture front à 100 %.
- `scripts/gates/golden-master.sh` : code 0.
- `scripts/gates/check-gates-exist.sh` : `OK` pour les quatre lignes compose et pour `trace-smoke.sh exécutable`. Les `KO` restants relèvent de la mutation (1.5) et des CVE (1.6).
- Pile lancée par `make secrets` puis `docker compose up -d --build --wait` (avec `NB_PORT_COEUR_BANCAIRE=18082` et `NB_SOUS_RESEAU=172.31.0.0/24`, propres à ce poste). `trace-smoke.sh` : code 0, 6 lignes `OK`. La pile a ensuite été arrêtée par `docker compose down`.
- curl : `/tableau-de-bord` renvoie 200, `/api/audit/` renvoie 404.

**Risques résiduels :**
- Ceux des passes précédentes restent valables :
  - reprise du consommateur sans délai ni file de rebut ;
  - healthcheck du collecteur limité à la validation de sa configuration ;
  - pile déjà démarrée non reconstruite par `trace-smoke.sh` ;
  - environ 8 s de POST quand Kafka est injoignable ;
  - un événement republié par le relais ouvre une trace distincte.
- Kafka n'a pas de volume. Si son conteneur est recréé seul, le topic disparaît jusqu'au redémarrage de `coeur-bancaire`.
