# Revue des versions et des faits techniques — spine next-banking-webapp

Date de la revue : 2026-10-05. Relecteur : reviewer gate (versions). Cible : `ARCHITECTURE-SPINE.md` ; preuves : `versions-verifiees.md`.

## Verdict

**PASS avec réserves** : aucun critical. Les versions de la pile principale sont bien couvertes par `versions-verifiees.md` (sources par ligne, 2026-10-05). Deux points high : la version de Node et l'image du conteneur web sont absentes des preuves (déjà marquées [OPEN] dans la spine) ; je les ai vérifiés ci-dessous, donc il reste à les reporter dans la spine et dans `versions-verifiees.md` avant la story 1.1. Le point « tâches planifiées observées » est levé.

## Findings

### Critical
Aucun.

### High
- **H1 — Version de Node absente** (spine, Open questions) : valeur vérifiée : Node **22.23.3** (22 LTS, fin de vie 2027-04-30) ou **24.21.0** (LTS, fin de vie 2028-04-30). Contraintes réelles : react-router 8.4.0 exige `>=22.22.0`, vitest 5.0.3 `^22.12.0 || ^24.0.0 || >=26.0.0`, Stryker 10.0.0 `>=22.0.0`, dependency-cruiser 18.5.0 `^22||^24||>=26`, Vite 8.3.2 `^20.19.0 || >=22.12.0`. Recommandation : Node 24 (22 sort du support dans 7 mois). Source : https://endoflife.date/api/nodejs.json ; registre npm (champ `engines`).
- **H2 — Image du serveur web du front absente** (AD-6, AD-17, Open questions) : valeur vérifiée : `nginx:1.30.5-alpine` (ou `nginx:stable-alpine`) existe sur Docker Hub ; cycle 1.30 non EOL ; 1.31.6 en mainline. Image de build front : `node:24-alpine` à confirmer individuellement (`node:22.23.3-alpine` et `node:26.10.0-alpine` existent). Sources : https://hub.docker.com/v2/repositories/library/nginx/tags ; https://endoflife.date/api/nginx.json. Le renvoi `/api/{identifiant}/**` et le repli SPA relèvent de la configuration nginx, à écrire à la story 1.1.

### Medium
- **M1 — TypeScript 5.9.3 (Stack) sans justification** : la dernière est 7.0.2 (6.0.3 en 6.x) ; `versions-verifiees.md` (écart 3) demande de trancher, la spine fige 5.9.3 sans dire pourquoi. Aucune compatibilité vérifiée avec Vite 8.3.2, Vitest 5, Stryker 10 ni dependency-cruiser 18.5.0 (qui n'a pas de peer typescript). Décision à consigner.
- **M2 — Tailwind v4 (AD-13)** : l'export `css-tailwind` de `@google/design.md` cible Tailwind v4 ; **tailwindcss 4.3.3** et `@tailwindcss/vite` 4.3.3 existent (https://endoflife.date/api/tailwind-css.json ; https://registry.npmjs.org/tailwindcss/latest). Reste [OPEN] dans la spine : adopter ou convertir en `:root`. `@google/design.md` 0.4.0 : `engines.node >=18`, OK.
- **M3 — `@vitejs/plugin-react` absent** : dernière **6.1.2**, peer `vite ^8.0.0` (compatible avec Vite 8.3.2). Non cité dans la spine, à ajouter à la pile.
- **M4 — Tags d'images non vérifiés individuellement** : `eclipse-temurin:21` (le tag `21-jre` existe sur Docker Hub ; la spine utilise `21` sans préciser JDK ou JRE, un JRE suffit pour l'exécution), `apache/kafka:4.2.2` (cité dans les preuves via la liste des tags, sans appel dédié), `otel/opentelemetry-collector:0.162.0` (vérifié). Le collecteur « core » n'a pas de récepteurs/exporteurs contrib : OTLP vers OTLP suffit, mais à valider avec `config/otelcol/config.yaml`.
- **M5 — Proxy Vite** : option `server.proxy` de Vite, API stable depuis Vite 2 ; non confirmée pour Vite 8.3.2 dans les preuves. Vérifier à la story 1.1 (https://vite.dev/config/server-options).
- **M6 — Options d'outils citées sans preuve** : `onionArchitecture()` / `failOnEmptyShould` (ArchUnit 1.5.1), `nvdApiServerId` et `failBuildOnCVSS` (dependency-check-maven 13.0.0), `historyInputFile`/`historyOutputFile` (pitest-maven 1.30.0), `incrementalFile` (Stryker 10), `coverage.thresholds` par glob (Vitest 5). Noms stables dans les versions précédentes, mais jamais confirmés contre les versions retenues. Les scripts de la story 1.x les exerceront (`check-gates-exist.sh`).

### Low
- **L1 — `Statement#setQueryTimeout` et `SET LOCAL lock_timeout`** : API JDBC standard et commande PostgreSQL de longue date, présentes sur postgres 18 et pilote 42.7.13 ; absentes des preuves mais non à risque. Note : `SET LOCAL` n'agit que dans une transaction ouverte, ce que l'adaptateur doit garantir (le port `Transaction` d'AD-1 le permet).
- **L2 — Version du JDK Temurin 21.0.12.1+1** : vérifiée via l'API Adoptium ; l'image Docker n'est pas épinglée à cette version.
- **L3 — Spring Boot 4.1** : fin du support OSS 2027-07-31 (preuve : endoflife.date) ; acceptable pour un démonstrateur.
- **L4 — Flyway 12.4.0 et pilote JDBC 42.7.13** : gérés par le BOM, donc cohérents ; non revérifiés en dehors du BOM.

## Éléments vérifiés par cette revue

| Élément | Valeur | Source | Date |
| --- | --- | --- | --- |
| Instrumentation automatique de `@Scheduled` dans Spring Boot 4.1.1 (lève l'[OPEN] « Observation des tâches planifiées ») | **Oui** : le jar `spring-boot-micrometer-observation` 4.1.1 contient `ScheduledTasksObservationAutoConfiguration` (déclarée dans `AutoConfiguration.imports`) avec un bean `ObservabilitySchedulingConfigurer`, conditionné à la présence d'un `ObservationRegistry` et de `ThreadPoolTaskScheduler`. Une observation `tasks.scheduled.execution` est créée par exécution (Spring Framework 7.1). Condition : `@EnableScheduling` et un `ObservationRegistry` présents. Attention : la page de référence Boot ne le mentionne pas (la lecture de la doc seule donnerait « non ») ; la preuve est le jar. | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-micrometer-observation/4.1.1/spring-boot-micrometer-observation-4.1.1.jar ; https://docs.spring.io/spring-framework/reference/7.1/integration/observability.html | 2026-10-05 |
| Node LTS | 24.21.0 (EOL 2028-04-30) ; 22.23.3 (EOL 2027-04-30) ; 26.10.0 (LTS à partir du 2026-10-28) | https://endoflife.date/api/nodejs.json | 2026-10-05 |
| Engines react-router 8.4.0 | `node >=22.22.0` | https://registry.npmjs.org/react-router/8.4.0 | 2026-10-05 |
| Engines Vite 8.3.2 | `node ^20.19.0 \|\| >=22.12.0` | https://registry.npmjs.org/vite/8.3.2 | 2026-10-05 |
| Engines Vitest 5.0.3 | `node ^22.12.0 \|\| ^24.0.0 \|\| >=26.0.0` ; peer vite `^6.4 \|\| ^7 \|\| ^8` | https://registry.npmjs.org/vitest/5.0.3 | 2026-10-05 |
| Engines Stryker core 10.0.0 | `node >=22.0.0` | https://registry.npmjs.org/@stryker-mutator/core/10.0.0 | 2026-10-05 |
| Engines dependency-cruiser 18.5.0 | `node ^22\|\|^24\|\|>=26` | https://registry.npmjs.org/dependency-cruiser/18.5.0 | 2026-10-05 |
| Engines @playwright/test 1.63.0 | `node >=20` | https://registry.npmjs.org/@playwright/test/1.63.0 | 2026-10-05 |
| Engines @google/design.md 0.4.0 | `node >=18` | https://registry.npmjs.org/@google/design.md/0.4.0 | 2026-10-05 |
| nginx | cycles 1.31 (mainline, 1.31.6) et 1.30 (1.30.5) supportés ; tags `1.30.5-alpine`, `stable-alpine` présents | https://endoflife.date/api/nginx.json ; https://hub.docker.com/v2/repositories/library/nginx/tags | 2026-10-05 |
| Images node | `node:22.23.3-alpine`, `node:26.10.0-alpine` présents | https://hub.docker.com/v2/repositories/library/node/tags | 2026-10-05 |
| Tailwind CSS | 4.3.3 (2026-07-16) ; `@tailwindcss/vite` 4.3.3 | https://endoflife.date/api/tailwind-css.json ; https://registry.npmjs.org/tailwindcss/latest | 2026-10-05 |
| `@vitejs/plugin-react` | 6.1.2, peer `vite ^8.0.0` | https://registry.npmjs.org/@vitejs/plugin-react/latest | 2026-10-05 |
| Image `eclipse-temurin:21-jre` | présente | https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags/21-jre | 2026-10-05 |

## Éléments de la spine déjà couverts par `versions-verifiees.md` (non revérifiés)

Spring Boot 4.1.1, starter OpenTelemetry et propriétés `management.opentelemetry.tracing.export.otlp.endpoint`, `management.tracing.sampling.probability` (0.1), `spring.kafka.{template,listener}.observation-enabled` (false), Spring Security 7.1.1 / `NimbusJwtEncoder` (nimbus-jose-jwt 10.9.1), starter resource server, datasource-micrometer 2.3.0, Kafka 4.2.2 / clients 4.2.1, PostgreSQL 18.6, collecteur 0.162.0, Jaeger 2.21.0, Testcontainers 2.0.5, ArchUnit, JaCoCo, PIT, Dependency-Check, Trivy, React 19.3.0, Vite, React Router 8.4.0, Vitest, Stryker, dependency-cruiser, Playwright 1.63.0 et `storageState` (localStorage persisté, sessionStorage non), `@google/design.md` 0.4.0 et `export --format css-tailwind`.

## Actions demandées

1. Reporter Node (recommandé 24) et l'image web (`nginx:1.30.5-alpine` ou équivalent épinglé) dans la Stack et dans `versions-verifiees.md` ; fermer l'[OPEN] correspondant.
2. Fermer l'[OPEN] « Observation des tâches planifiées » avec la preuve du jar ; garder la contrainte `@EnableScheduling` + `ObservationRegistry`.
3. Consigner la décision TypeScript 5.9.3 contre 7.0.2 et trancher Tailwind v4 ou conversion `:root`.
4. Épingler les tags d'images (temurin JDK/JRE, node de build).
