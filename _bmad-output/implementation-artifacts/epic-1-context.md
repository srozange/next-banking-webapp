# Epic 1 Context: Fondations contrôlées

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Livrer un squelette à deux services hexagonaux (`coeur-bancaire`, `notifications`) et un front hexagonal, sur lequel tous les contrôles qualité du projet sont en place : bloquants (architecture, couverture, tracing, CVE) verts, à la demande (mutation) exécutables et verts. Un flux témoin traverse les deux services par Kafka et sa trace est vérifiée dans Jaeger. Aucun besoin fonctionnel ni scénario golden master dans cet epic : il prouve que les contrôles mordent sur du code réel avant toute story métier. À la fin, `scripts/gates/check-gates-exist.sh` sort en code 0 sans aucune ligne `KO`, et l'humain peut figer l'empreinte des contrôles.

## Stories

- Story 1.1 : Poser le squelette des deux services et du front
- Story 1.2 : Rendre l'architecture hexagonale vérifiable et bloquante
- Story 1.3 : Imposer la couverture par couche
- Story 1.4 : Faire traverser les deux services par un flux témoin tracé
- Story 1.5 : Mesurer la mutation du domaine et de l'application à la demande
- Story 1.6 : Bloquer les dépendances vulnérables

## Requirements & Constraints

- Six stories exactement, ordre strict 1.1 → 1.6 ; pas de story 1.7. Seuls deux services sous `services/` pendant l'epic (deux conteneurs `postgres-`).
- Chaque contrôle se prouve par un test négatif : une violation introduite volontairement doit faire sortir l'outil en code non nul.
- Un contrôle rouge se corrige dans le code ou les tests, jamais dans le contrôle. Interdit : abaisser un seuil, ajouter une exclusion de couverture ou de mutation, désactiver un test, mettre `haltOnFailure` à `false`, écrire `<nvdApiKey>` dans un POM.
- Fichiers protégés : `scripts/gates/**`, `.bmad-loop/policy.toml`, `.bmad-loop/gates.sha256`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `.trivyignore`, `config/dependency-check-suppressions.xml`, golden master (`e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`). Seules exceptions : la 1.2 crée `frontend/.dependency-cruiser.cjs`, la 1.4 crée `scripts/gates/trace-smoke.sh`. Chaque story vérifie par `git diff` que rien d'autre n'a bougé.
- Fin de chaque story : `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh` (lignes de la story en `OK`) et `scripts/gates/golden-master.sh` (statique, ne rejoue rien) en code 0 ; à partir de la 1.4, `trace-smoke.sh` aussi.
- Seuils : couverture 100 % lignes et branches sur domain/application, 80 % sur adapter/infrastructure (Java et front) ; mutation 85 (PIT `mutationThreshold`, Stryker break 85 / high 90 / low 85) ; CVE : CVSS ≥ 7 bloque, Trivy HIGH/CRITICAL corrigeables.
- Mutation et CVE hors build par défaut (profils Maven `mutation` et `security`). `cve.sh` ne met pas à jour les bases Trivy/NVD : si elles manquent ou ont expiré, s'arrêter et demander à l'humain.
- Secrets uniquement en variables d'environnement (`make secrets` génère `.env` et `.secrets/`, non versionnés) ; ne jamais lire ni afficher la clé NVD de `~/.m2/settings.xml`.
- Ne jamais rejouer le golden master dans cet epic.

## Technical Decisions

**Versions retenues** (seules autorisées, jamais « la dernière » ni celles du poste) :

- Java 21 (JDK Temurin 21.0.12.1+1 ; images `eclipse-temurin:21` → `eclipse-temurin:21-jre`) ; Maven 3.9.16 ; Spring Boot 4.1.1 GA (pas 4.2.0-M2) ; surefire géré par Boot (3.5.6).
- Gérés par le BOM Boot : Spring Kafka 4.1.1 / kafka-clients 4.2.1, OpenTelemetry 1.62.0, micrometer-tracing 1.7.1, Testcontainers 2.0.5 (artefacts `testcontainers-postgresql`, `testcontainers-kafka`), JDBC PostgreSQL 42.7.13, Flyway 12.4.0.
- `spring-boot-starter-opentelemetry` 4.1.1 ; `datasource-micrometer-spring-boot` 2.3.0.
- Plugins de contrôle : `archunit-junit5` 1.5.1 ; `jacoco-maven-plugin` 0.8.15 ; `pitest-maven` 1.30.0 + `pitest-junit5-plugin` 1.2.3 ; `dependency-check-maven` 13.0.0 ; Trivy 0.75.0 (jamais 0.69.4 à 0.69.6).
- Front : Node 24.21.0 ; TypeScript 5.9.3 (pas 7.x) ; Vite 8.3.2 ; `@vitejs/plugin-react` 6.1.2 ; React 19.3.0, `react-dom`, `@types/react` et `@types/react-dom` 19.3.0 (ajoutés par la story 1.1) ; `vitest` et `@vitest/coverage-v8` exactement 5.0.3 ; dependency-cruiser 18.5.0 ; `@stryker-mutator/core` et `@stryker-mutator/vitest-runner` 10.0.0.
- Conteneurs : `postgres:18.6`, `apache/kafka:4.2.2` (KRaft), `otel/opentelemetry-collector:0.162.0`, `cr.jaegertracing.io/jaegertracing/jaeger:2.21.0` (jamais `all-in-one`), `nginx:1.30.5-alpine`, construction du front `node:24.21.0-alpine` (tag à vérifier).

**Structure et nommage**

- POM parent `pom.xml` à la racine ; modules `services/coeur-bancaire` et `services/notifications`, paquets racine `fr.nextbanking.coeurbancaire` et `fr.nextbanking.notifications` (`fr.nextbanking.<identifiantsanstiret>`).
- Cinq paquets par service : `domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure`, chacun avec au moins une classe dès la 1.1, dont un type `public` sous `adapter` (`failOnEmptyShould` reste `true`).
- Front : `src/domain`, `src/application`, `src/adapters` (tout React), `src/infrastructure` (point d'entrée). Un seul `frontend/vite.config.ts` (plugin React en 1.1, seuils de couverture en 1.3, proxy en 1.4), jamais de `vitest.config.*`. `package-lock.json` versionné dès la 1.1. Aucun thème (`npm run tokens`) dans cet epic.

**Contrôles d'architecture**

- Par service, `HexagonalArchitectureTest` avec exactement deux règles (`Architectures.onionArchitecture()` ; aucun framework dans domain/application). Règles séparées : `HorlogeArchitectureTest` (pas de `now(..)` `java.time` ni `java.time.Clock` hors `adapter.out.horloge` et `adapter.out.jeton`) et `ClientsHttpArchitectureTest` (clients HTTP seulement dans `adapter.out.fournisseur`).
- Port `application.port.out.Horloge` (`instant()`, `aujourdhui()`, `fuseau()`), adaptateur système dans `adapter.out.horloge`, `nb.fuseau=Europe/Paris` validé au démarrage, livré en 1.2 dans les deux services (profil `rejeu` plus tard).
- Front : dependency-cruiser (`domain-ne-depend-de-rien`, `application-ne-depend-pas-des-adaptateurs`, `hexagone-sans-paquet-npm`, sévérité error, `tsPreCompilationDeps: true`) ; test Vitest `src/architecture-temps.test.ts` qui interdit `Date.now()`, `new Date()` sans argument et `Temporal.Now` dans domain/application.
- Transactions par le port `application.port.out.Transaction` (implémenté dans `adapter.out.persistance`) ; aucune annotation transactionnelle dans `application`.

**Flux témoin (1.4)**

- `POST /api/coeur-bancaire/temoin` (chemin public) → écriture en base `coeur-bancaire` + ligne `outbox` dans la même transaction → publication par `KafkaTemplate` après validation, relais planifié chaque seconde → topic `coeur-bancaire.temoin-ecrit.v1` (clé `temoinId`, bean `NewTopic` dans `infrastructure`, 3 partitions, réplication 1) → `notifications` (écouteur unitaire, `group.id` = `notifications`) écrit le témoin et `idEvenement` dans `evenements_traites`, même transaction.
- Enveloppe : `idEvenement` (UUID), `type`, `version`, `horodatage` (ISO 8601 UTC, par l'Horloge), `producteur`, `donnees` ; `StringSerializer`, aucun en-tête `__TypeId__`. Schéma `contrats/evenements/coeur-bancaire.temoin-ecrit.v1.schema.json` avec exemple ; tests `*ContratEvenement*Test` côté producteur et consommateur.
- Tracing : propriétés explicites dans chaque service (`management.opentelemetry.tracing.export.otlp.endpoint=http://otel-collector:4318/v1/traces`, `management.tracing.sampling.probability=1.0`, observations Kafka template et listener à `true`) ; jamais `management.otlp.tracing.export.enabled` ; aucun span créé à la main. Journaux avec `traceId`/`spanId` ; seul `/actuator/health` exposé.
- `compose.yaml` : une base par service (`postgres-<id>`, utilisateur et mot de passe propres, volume `pgdata-<id>`), Kafka, collecteur (`config/otelcol/config.yaml`), Jaeger (16686), `web` (port 8080) ; `healthcheck` partout, `TZ=Europe/Paris`, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, secrets en `${NB_..._MOT_DE_PASSE:-}` (service qui refuse de démarrer sans valeur). Ports de développement des services : `coeur-bancaire` 8082, `notifications` 8085.
- nginx et proxy Vite : `/api/{identifiant}/**` vers le service (chemin tel quel), sauf `/api/audit/**` ; tout autre chemin → `index.html` ; `X-Forwarded-For` écrasé par `$remote_addr`.
- `trace-smoke.sh` exige sous un même identifiant de trace deux services, un span JDBC, un span d'émission et un span de consommation Kafka.

**Mutation et CVE**

- PIT dans le seul profil `mutation` (cibles `fr.nextbanking.*.domain.*` et `*.application.*`, historique `${user.home}/.cache/next-banking-webapp/pit/${project.artifactId}.history`) ; `frontend/stryker.config.json` (runner vitest, `incrementalFile` `reports/stryker-incremental.json`, non versionné).
- Dependency-Check dans le seul profil `security` : `failBuildOnCVSS` 7, `nvdApiServerId` `nvd`, OSS Index et Node Audit désactivés, `autoUpdate` false, suppressions `config/dependency-check-suppressions.xml` (vide, comme `.trivyignore`).

## Cross-Story Dependencies

- 1.1 sert de base à tout ; la 1.2 livre le port Horloge, utilisé par l'enveloppe Kafka de la 1.4 ; `vite.config.ts` évolue en 1.1, 1.3 puis 1.4.
- Les deux fichiers protégés créés par des stories (`.dependency-cruiser.cjs` en 1.2, `trace-smoke.sh` en 1.4) doivent exister avant que l'humain fige `.bmad-loop/gates.sha256` après la 1.6 ; à partir de l'epic 2, `sha256sum -c` devient la première vérification.
- 1.6 : prérequis humains (clé NVD configurée, Trivy 0.75.0 et `jq` installés, bases Trivy et Dependency-Check à jour).
- Après l'epic 1 : la story de connexion (epic 2) apporte la vérification du JWT ; le point témoin restera public pour que `trace-smoke.sh` reste vert.
