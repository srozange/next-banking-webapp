# Epic 1 Context: Fondations contrôlées

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Livrer un squelette à deux services hexagonaux (`coeur-bancaire`, `notifications`) et un front hexagonal, sur lequel tous les contrôles qualité du projet sont en place : les contrôles bloquants (architecture, couverture, tracing, CVE) sont verts, et le contrôle à la demande (mutation) est exécutable et vert. Un flux témoin traverse les deux services par Kafka et sa trace est vérifiée dans Jaeger. L'epic ne porte aucun besoin fonctionnel ni scénario golden master : il prouve que les contrôles mordent sur du code réel avant toute story métier. À la fin, `scripts/gates/check-gates-exist.sh` sort en code 0 sans aucune ligne `KO`, et l'humain peut figer l'empreinte des contrôles.

## Stories

- Story 1.1 : Poser le squelette des deux services et du front
- Story 1.2 : Rendre l'architecture hexagonale vérifiable et bloquante
- Story 1.3 : Imposer la couverture par couche
- Story 1.4 : Faire traverser les deux services par un flux témoin tracé
- Story 1.5 : Mesurer la mutation du domaine et de l'application à la demande
- Story 1.6 : Bloquer les dépendances vulnérables

## Requirements & Constraints

- Six stories exactement, dans l'ordre strict 1.1 → 1.6, sans story 1.7. Seuls deux services sous `services/` pendant l'epic.
- Chaque contrôle se prouve par un test négatif : une violation introduite volontairement doit faire sortir l'outil en code non nul. Pour la mutation : un test de `domain` de `coeur-bancaire` vidé de ses assertions fait échouer `mutation.sh` ; un test de `frontend/src/domain` vidé de ses assertions fait échouer `npx stryker run --incremental`.
- Un contrôle rouge se corrige dans le code ou dans les tests, jamais dans le contrôle. Il est interdit d'abaisser un seuil, d'ajouter une exclusion de couverture ou de mutation, de désactiver un test, de mettre `haltOnFailure` à `false` ou d'écrire `<nvdApiKey>` dans un POM. Si un seuil paraît inatteignable, s'arrêter et le signaler.
- Fichiers protégés : `scripts/gates/**`, `.bmad-loop/policy.toml`, `.bmad-loop/gates.sha256`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `.trivyignore`, `config/dependency-check-suppressions.xml`, plus le golden master (`e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`). Deux exceptions seulement : la 1.2 crée `frontend/.dependency-cruiser.cjs` et la 1.4 crée `scripts/gates/trace-smoke.sh`. Chaque story vérifie par `git diff` que rien d'autre n'a bougé.
- Fin de chaque story : `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh` (les lignes de la story en `OK`), `scripts/gates/golden-master.sh` (statique) et, depuis la 1.4, `scripts/gates/trace-smoke.sh`, tous en code 0. Depuis la 1.5, `scripts/gates/mutation.sh` aussi.
- Seuils :
  - couverture : 100 % des lignes et des branches sur domain et application, 80 % sur adapter et infrastructure (Java et front) ;
  - mutation : 85 (`mutationThreshold` de PIT ; Stryker `break` 85, `high` 90, `low` 85) ;
  - CVE : un CVSS ≥ 7 bloque, ainsi que les vulnérabilités Trivy HIGH ou CRITICAL corrigeables.
- La mutation et les CVE ne tournent pas dans le build par défaut : elles passent par les profils Maven `mutation` et `security`. `mvn -B -q verify -DfailIfNoTests=true` ne doit pas lancer PIT. `cve.sh` ne met pas à jour les bases Trivy et NVD : si elles manquent ou ont expiré, s'arrêter et demander à l'humain.
- Secrets uniquement en variables d'environnement (`make secrets`). Ne jamais lire ni afficher la clé NVD de `~/.m2/settings.xml`. Ne jamais rejouer le golden master dans cet epic.

## Technical Decisions

**Versions retenues** (seules autorisées, jamais « la dernière » ni celles du poste) :

- Back : Java 21 (Temurin 21.0.12.1+1 ; images `eclipse-temurin:21` pour construire, `eclipse-temurin:21-jre` pour exécuter) ; Maven 3.9.16 ; Spring Boot 4.1.1 GA (pas 4.2.0-M2) ; surefire géré par Boot (3.5.6).
- Gérés par le BOM Boot : Spring Kafka 4.1.1 et kafka-clients 4.2.1, OpenTelemetry 1.62.0, micrometer-tracing 1.7.1, Testcontainers 2.0.5 (artefacts `testcontainers-postgresql` et `testcontainers-kafka`), JDBC PostgreSQL 42.7.13, Flyway 12.4.0 (`spring-boot-starter-flyway` 4.1.1 et `flyway-database-postgresql`).
- `spring-boot-starter-opentelemetry` 4.1.1 ; `datasource-micrometer-spring-boot` 2.3.0.
- Contrôles : `archunit-junit5` 1.5.1 ; `jacoco-maven-plugin` 0.8.15 ; **`pitest-maven` 1.30.0 et `pitest-junit5-plugin` 1.2.3** (c'est Maven Central qui fait foi pour la 1.2.3, pas GitHub) ; `dependency-check-maven` 13.0.0 ; Trivy 0.75.0 (jamais 0.69.4 à 0.69.6).
- Front : Node 24.21.0 ; TypeScript 5.9.3 (pas 7.x) ; Vite 8.3.2 ; `@vitejs/plugin-react` 6.1.2 ; React, `react-dom`, `@types/react` et `@types/react-dom` en 19.3.0 ; `vitest` et `@vitest/coverage-v8` exactement en 5.0.3 ; dependency-cruiser 18.5.0 ; **`@stryker-mutator/core` et `@stryker-mutator/vitest-runner` en 10.0.0** (runner : peer `vitest >=2.0.0` ; core : Node `>=22.0.0`).
- Conteneurs : `postgres:18.6`, `apache/kafka:4.2.2` (KRaft), `otel/opentelemetry-collector:0.162.0`, `cr.jaegertracing.io/jaegertracing/jaeger:2.21.0` (seule l'API de requête v3 répond), `nginx:1.30.5-alpine`, `node:24.21.0-alpine` pour construire le front (tag confirmé).

**Structure**

- POM parent `pom.xml` à la racine : il porte JaCoCo et les profils `mutation` et `security`. Les modules sont `services/coeur-bancaire` et `services/notifications`, avec les paquets `fr.nextbanking.coeurbancaire` et `fr.nextbanking.notifications`. Chaque service a cinq paquets : `domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure`.
- Front : `src/domain`, `src/application`, `src/adapters` et `src/infrastructure`. Un seul `frontend/vite.config.ts`, jamais de `vitest.config.*`. `package-lock.json` est versionné.
- Les règles d'architecture sont en place depuis la 1.2 : ArchUnit (onion, pas de framework dans domain/application, Horloge, clients HTTP) et dependency-cruiser. L'heure passe par le port `application.port.out.Horloge`, les transactions par `application.port.out.Transaction`.
- Flux témoin (en place depuis la 1.4) : `POST /api/coeur-bancaire/temoin` (chemin public) → outbox → topic `coeur-bancaire.temoin-ecrit.v1` → `notifications`, idempotent via `evenements_traites`. Le tracing est vérifié par `trace-smoke.sh`.

**Mutation (story 1.5)**

- PIT figure **uniquement** dans le profil `mutation` du POM parent, et la ligne `PIT actif hors profil mutation` ne doit jamais apparaître. Configuration : `targetClasses` `fr.nextbanking.*.domain.*` et `fr.nextbanking.*.application.*`, `<mutationThreshold>85</mutationThreshold>`, plugin `pitest-junit5-plugin`, aucune exclusion de mutation.
- Historique PIT hors de `target/` : `historyInputFile` et `historyOutputFile` valent `${user.home}/.cache/next-banking-webapp/pit/${project.artifactId}.history`. Après un run, `coeur-bancaire.history` et `notifications.history` doivent exister.
- `frontend/stryker.config.json` : `testRunner` `vitest` ; `mutate` sur `src/domain` et `src/application`, fichiers de test exclus ; `thresholds` `break` 85, `high` 90, `low` 85 ; `incrementalFile` `reports/stryker-incremental.json`. `frontend/reports/` est ignoré par git (`git check-ignore` en code 0). Les deux paquets Stryker sont déclarés dans `frontend/package.json`.
- `scripts/gates/mutation.sh` (protégé) lance PIT par `-Pmutation test-compile` sur `mutationCoverage`, puis `npx stryker run --incremental` depuis `frontend/`.
- Lignes attendues dans `check-gates-exist.sh` : pour chaque service, `profil mutation avec PIT`, `seuil de mutation 85` et `historique PIT configuré` ; côté front, `StrykerJS installé` et `Stryker break 85`.

**CVE (story 1.6)**

- Dependency-Check figure uniquement dans le profil `security`, avec `failBuildOnCVSS` 7, `nvdApiServerId` `nvd`, OSS Index et Node Audit désactivés, et `autoUpdate` à `false` (surchargeable par `-Ddc.autoUpdate=true`, réservé à l'humain). Le fichier de suppressions est `config/dependency-check-suppressions.xml`.
- `.trivyignore` et `config/dependency-check-suppressions.xml` restent vides (aucun `<suppress>`). Les rapports `reports/cve-all.json` et `reports/cve-unfixed.tsv` ne sont pas versionnés.

## Cross-Story Dependencies

- Les stories 1.1 à 1.4 sont livrées. La 1.5 exige que les quatre premières soient terminées, et la 1.6 que les cinq premières le soient.
- La mutation porte sur le code de domain et d'application déjà couvert à 100 % (1.3). Un score sous 85 se corrige en renforçant les assertions des tests, jamais par une exclusion.
- Prérequis humains de la 1.6 : clé NVD dans `~/.m2/settings.xml` (serveur `nvd`), Trivy 0.75.0 et `jq` installés, bases Trivy et Dependency-Check à jour.
- Après la 1.6, l'humain fige `.bmad-loop/gates.sha256`. Cette empreinte couvre `scripts/gates/*.sh`, `.dependency-cruiser.cjs`, `.trivyignore`, les suppressions et `replay-mapping.json`, qui doivent tous exister. À partir de l'epic 2, `sha256sum -c` devient la première vérification.
- Epic 2 : le point d'API témoin reste public, pour que `trace-smoke.sh` reste vert quand la vérification du JWT arrivera.
