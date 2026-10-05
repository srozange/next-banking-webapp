# Reconcile inputs : quality-gates.md, scripts/gates, formes.md, prompts d'epics

Spine relue : ARCHITECTURE-SPINE.md (508 lignes). Une ligne par écart. Gravité : H (casse un contrôle ou contredit le pilote), M (ambigu ou incomplet), B (mineur).

## A. Ordre des stories de l'epic 1 (prompts-bmad.md)

| # | Gravité | Source | Écart | Correction proposée |
| --- | --- | --- | --- | --- |
| 1 | H | Spine AD-11 (Preuve), Open questions ; prompts-bmad.md « Epics et stories, avec corpus » | La spine propose une story 1.7 (données de référence, `compose.rejeu.yaml`, `make reset-data`, `make rejeu-up`) dans l'epic 1. Le prompt du pilote place la story « Socle du rejeu final du golden master » (Playwright, données de référence, remise à zéro, comptes de test, bouchons) dans le DERNIER epic, et la porte `bmad-sprint-planning` vérifie « la story socle est dans le dernier epic ». De plus l'epic 1 n'a que 2 services (AD-15) alors que `reference-data.sql` et `reset-data` visent les 8 bases : impossible à livrer en epic 1. Une 1.7 après 1.6 ne casse pas l'ordre 1.1 à 1.6, mais son contenu est incompatible avec « epic 1 = fondations + flux témoin, rien de métier ». | Ne pas créer de 1.7 pour AD-11. Mettre dans AD-11 : « livré par la story socle du rejeu final, dernier epic ». Si le profil de rejeu doit être testé tôt, limiter une éventuelle 1.7 au Makefile, `compose.rejeu.yaml` et au réglage de l'Horloge sur les 2 services de l'epic 1 (sans données de référence, sans Playwright), et la signaler comme écart au pilote. |
| 2 | H | Spine AD-3 et AD-6 (Preuve : « Livré par la story 1.1 ») ; prompts-bmad.md story 1.1 et 1.4 | La spine fait livrer `compose.yaml` par la 1.1. Le pilote : 1.1 = POM parent, deux services, front minimal, build vert ; `compose.yaml`, collecteur et Jaeger = story 1.4. | AD-3 et AD-6 : « `compose.yaml` livré par la story 1.4 » (ou nommer explicitement un écart assumé de la 1.1). Le build vert de la 1.1 ne dépend pas de compose. |
| 3 | M | Spine AD-13, AD-17 (livrés « story 1.1 ») ; prompts-bmad.md story 1.1 | La 1.1 du pilote ne mentionne ni l'export des tokens DESIGN.md, ni le proxy Vite, ni le conteneur `web` (Dockerfile du front, renvoi `/api/**`). La spine charge la 1.1 de ces trois sujets. Le conteneur `web` est dans compose, donc en 1.4. | AD-13 et proxy Vite : conserver en 1.1 en le disant comme extension du périmètre « front minimal ». Conteneur `web` et `frontend/Dockerfile` : story 1.4. |
| 4 | M | Spine AD-19 (Preuve), AD-16 (Preuve) ; prompts-bmad.md story 1.2 | `HorlogeArchitectureTest` (1.2) et la vérification du jeton dans les deux services (1.1) ne figurent pas dans les critères du pilote pour ces stories. Le port `application.port.out.Horloge` doit exister dès la 1.1 pour que le test de la 1.2 ne soit pas vide (`failOnEmptyShould`). | Marquer ces affectations comme ajouts au pilote (déjà [ASSUMPTION]) et prévoir le port Horloge dans le squelette 1.1. |
| 5 | B | Spine AD-1 | La spine n'indique pas ce que la 1.1 doit poser pour que 1.2 passe : `onionArchitecture()` d'ArchUnit échoue sur une couche vide (sauf `withOptionalLayers(true)`, que formes.md n'emploie pas) ; `canary-arch.sh` exige au moins un type `public` sous `adapter`. | Ajouter à AD-1 : « les 5 paquets contiennent chacun au moins une classe dès la story 1.1, dont un type public sous `adapter` » (à vérifier à la compilation du test en 1.2). |

## B. Commandes de preuve contre les scripts réels

| # | Gravité | Source | Écart | Correction proposée |
| --- | --- | --- | --- | --- |
| 6 | H | Spine AD-7 (vitest.config.ts), Structural Seed, AD-17 (proxy Vite) ; check-gates-exist.sh : `cfg=$(ls vitest.config.* vite.config.* \| head -1)` | `ls` trie ses arguments : si `frontend/vite.config.ts` (proxy `/api`, requis par AD-17) ET `frontend/vitest.config.ts` coexistent, le script choisit `vite.config.ts`, qui n'a pas `thresholds` : lignes « seuils de couverture Vitest » et « seuil dédié au domaine » en KO. | Décider : soit un seul fichier `frontend/vite.config.ts` portant le bloc `test.coverage.thresholds` (proxy compris) et pas de `vitest.config.ts` ; soit pas de `vite.config.ts` et le proxy dans `vitest.config.ts`. À trancher avec le pilote (formes.md dit `vitest.config.ts`, donc le script devra être ajusté par l'humain si on garde deux fichiers). |
| 7 | H | Spine AD-5, AD-2, AD-9 vs AD-10 ; quality-gates.md « Fichiers interdits » ; check-gates-exist.sh | `scripts/gates/trace-smoke.sh` (story 1.4), `frontend/.dependency-cruiser.cjs` (1.2), `.trivyignore` et `config/dependency-check-suppressions.xml` (1.6) sont des livrables de stories ET des fichiers interdits de modification aux agents ; `gates.sha256` les couvrira probablement. La spine ne dit pas qui les crée. De plus `.bmad-loop/` n'existe pas dans le dépôt alors que AD-10 le suppose « livré avant l'epic 1 ». | Ajouter à AD-10 : création de ces fichiers par l'humain (ou par le pilote hors boucle) avec mise à jour de `.bmad-loop/gates.sha256`, l'agent ne faisant que les respecter. Sinon la 1.2, 1.4 et 1.6 sont bloquées par la politique. |
| 8 | M | Spine AD-1 (Preuve) ; canary-arch.sh | Une seule invocation est donnée : `services/coeur-bancaire fr/nextbanking/coeurbancaire`. L'epic 1 a deux services et AD-1 vaut « dans chaque service ». Arguments corrects (service, paquet avec `/`, sortie `OK  ArchUnit détecte la dépendance domaine -> adaptateur`). | Ajouter `scripts/gates/canary-arch.sh services/notifications fr/nextbanking/notifications`. |
| 9 | M | Spine AD-9 (Preuve) ; cve.sh | `cve.sh` exige `trivy`, `jq`, des bases Trivy et Java déjà téléchargées (`--skip-db-update --skip-java-db-update`) et la base Dependency-Check déjà mise à jour (autoUpdate false). La spine ne dit ni comment ni quand ces bases sont mises à jour, ni que `jq` est requis. | Ajouter à AD-9 : prérequis (trivy 0.75.0, jq), commande de mise à jour des bases avant le run (hors script, par l'humain), et `dc.autoUpdate` (propriété à `false` par défaut, cf. formes.md). |
| 10 | M | Spine AD-9 ; check-gates-exist.sh | Les lignes de preuve citent `.trivyignore présent` mais pas `config/dependency-check-suppressions.xml présent` (également vérifié). Le Structural Seed liste le second, pas `.trivyignore`. | Ajouter la ligne manquante à AD-9 et `.trivyignore` au Structural Seed. |
| 11 | M | Spine AD-6 ; check-gates-exist.sh (`docker compose config --services`) | Les secrets viennent « de variables d'environnement » (NFR-SEC-06). Une interpolation `${VAR:?}` sans valeur fait échouer `docker compose config`, donc la ligne « compose : fichier absent ou invalide ». Le script teste aussi les services par simple sous-chaîne (`coeur-bancaire` est trouvé dans `postgres-coeur-bancaire`) : contrôle faible. | Préciser dans AD-6 : valeurs par défaut non secrètes pour la config (`${VAR:-...}`) ou `.env` non versionné, de sorte que `config --services` sorte en 0 sans secret ; nommer le service applicatif `<identifiant>` exact. |
| 12 | B | Spine AD-1, AD-7, AD-2 ; build.sh | `build.sh` lance `npm ci` : `frontend/package-lock.json` doit être versionné par la 1.1. `mvn test -Dtest='*ArchitectureTest'` échoue sur un module sans test correspondant : tous les modules `services/*` doivent en porter un (cas de `HorlogeArchitectureTest` aussi). Non dit dans la spine. | Ajouter ces deux contraintes aux livrables de la 1.1 / 1.2. |
| 13 | B | Spine AD-19 ; quality-gates.md « avec deux règles » | `HorlogeArchitectureTest` est une 2e classe `*ArchitectureTest` : aucune contradiction avec le script (`find ... | grep -q .`) mais la mention « deux règles » doit rester vraie pour la classe hexagonale. | Nommer la classe hexagonale (par ex. `HexagonalArchitectureTest`) dans AD-1 et préciser que la règle Horloge est un test à part. |

## C. Chemins et noms (spine, scripts, formes.md)

Concordent : `frontend/.dependency-cruiser.cjs` (règles `domain-ne-depend-de-rien`, `application-ne-depend-pas-des-adaptateurs`, `hexagone-sans-paquet-npm`, `tsPreCompilationDeps`), profils Maven `mutation` et `security` dans le POM parent, `reports/cve-all.json` et `reports/cve-unfixed.tsv`, `reports/stryker-incremental.json`, `config/dependency-check-suppressions.xml`, `.trivyignore`, `*ArchitectureTest`, `services/<nom>/pom.xml`, seuils JaCoCo `1.00` et `0.80` (regex du script satisfaites), `<mutationThreshold>85`, `<historyInputFile>`, `failBuildOnCVSS 7`, `nvdApiServerId nvd`, `autoUpdate false`, `ossIndexAnalyzerEnabled false`, Stryker `break 85`, versions (ArchUnit 1.5.1, JaCoCo 0.8.15, PIT 1.30.0 + junit5 1.2.3, Dependency-Check 13.0.0, dependency-cruiser 18.5.0, Vitest 5, Stryker 10). Les intitulés de lignes cités en Preuve (AD-6, AD-7, AD-8, AD-9) correspondent aux libellés de check-gates-exist.sh.

| # | Gravité | Source | Écart | Correction proposée |
| --- | --- | --- | --- | --- |
| 14 | M | Spine AD-8 vs formes.md 1.5 | Adaptation légitime (`fr.nextbanking` au lieu de `com.exemple`, `next-banking-webapp` au lieu de `monprojet`) mais les 2 `.history` (input et output) doivent rester identiques : AD-8 le dit. Spine AD-8 ajoute `low` 85 (formes.md aussi) : OK. Pas d'écart réel ; noter seulement que `mutation.sh` ajoute `--incremental` alors que le JSON a déjà `incremental: true` (redondant, sans effet). | Aucune. |
| 15 | B | Spine AD-9 vs formes.md 1.6 | formes.md ajoute `nodeAuditAnalyzerEnabled false` (la spine : « Node Audit désactivé », ok) et `assemblyAnalyzerEnabled false`, absent de la spine et de quality-gates ; `check-gates-exist.sh` ne teste pas Node Audit. | Facultatif : mentionner `assemblyAnalyzerEnabled false` ou l'omettre ; ne pas en faire un contrôle. |

## D. Chaque ligne de docs/quality-gates.md reprise dans une AD

| Ligne de quality-gates.md | AD | Fidélité |
| --- | --- | --- |
| Microservices Java 21, Spring Boot 4, Maven ; module par service sous services/<nom>/ ; POM parent à la racine | Aucune AD ; Stack, Design Paradigm, Structural Seed, Conventions | Écart (#16) |
| Hexagone back, packages, domain/application Java pur | AD-1 + Design Paradigm | Fidèle |
| Une base PostgreSQL par service | AD-3 | Fidèle |
| Bus Kafka | AD-4 | Fidèle |
| Front React + TypeScript sous frontend/, hexagonal, TypeScript pur | AD-2 + Design Paradigm | Fidèle ; la phrase « Tout ce qui est React vit dans src/adapters » n'est que dans le Design Paradigm (#17) |
| Tracing (starter, OTLP, Jaeger v2, aucun span manuel, datasource-micrometer 2.x, HTTP et Kafka, 1.0 local) | AD-5 | Fidèle |
| compose.yaml | AD-6 | Fidèle (étend à 8 services, `web`) |
| Contrôle hexagone back / front | AD-1 / AD-2 | Fidèle |
| Couverture back / front | AD-7 | Fidèle |
| Mutation back / front | AD-8 | Fidèle |
| CVE Dependency-Check | AD-9 | Fidèle |
| CVE Trivy | AD-9 | Fidèle |
| Tracing : trace-smoke.sh | AD-5 (Preuve), AD-15 | Fidèle (script absent, story 1.4) |
| Règles pour les agents (4 puces) | AD-10 | Fidèle, liste des 9 fichiers identique |

| # | Gravité | Source | Écart | Correction proposée |
| --- | --- | --- | --- | --- |
| 16 | M | quality-gates.md ligne 1 de « Architecture » | Aucune AD ne reprend « Microservices Java 21, Spring Boot 4, Maven ; un module par service sous services/<nom>/, POM parent à la racine » : la phrase n'existe que dans Stack, Seed et Conventions. Le prompt d'architecture exige que la liste des lignes non reprises soit vide. | Ajouter ces deux phrases à la Rule d'AD-14 (ou d'AD-1). |
| 17 | B | quality-gates.md « Front ... Tout ce qui est React vit dans src/adapters » | Repris seulement dans le Design Paradigm, pas dans la Rule d'AD-2. | Ajouter la phrase à AD-2, avec l'ajout « hors fichiers de test ». |
| 18 | B | Spine AD-4 « Preuve » | AD-4 renvoie à `trace-smoke.sh` et à un `grep` sur les URL `http://<service>` ; ce grep ne couvre ni les noms de conteneur avec port ni les clients configurés par propriété. Ce n'est pas une commande des scripts, c'est une preuve additionnelle fragile. | Marquer comme complément non bloquant ; la preuve ferme reste `trace-smoke.sh`. |
