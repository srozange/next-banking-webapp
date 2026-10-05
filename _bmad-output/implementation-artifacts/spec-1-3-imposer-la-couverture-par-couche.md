---
title: 'Story 1.3 — Imposer la couverture par couche'
type: 'chore'
created: '2026-10-05'
status: 'done'
baseline_commit: '88a2facb805ac44af396cf9eaf6bdd1105d72a4c'
baseline_revision: '88a2facb805ac44af396cf9eaf6bdd1105d72a4c'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/quality-gates.md'
warnings: []
deferred:
  - summary: >-
      Aucun canari automatique ne prouve que les seuils JaCoCo et Vitest mordent encore : une faute dans un include ou un glob les désactiverait sans alerte.
    evidence: |-
      check-gates-exist.sh ne fait que des grep (jacoco-maven-plugin, <goal>check</goal>, <minimum>…, thresholds, src/domain) ; `<include>*.domaine*</include>` ou `'src/domain/*.tsx'` laissent tout vert. Les tests négatifs de la story sont manuels. Correctif : un canary-coverage.sh calqué sur canary-arch.sh, sous scripts/gates/ (protégé) : décision humaine avant l'empreinte (étape 10 du pilote).
    location: >-
      scripts/gates/
    severity: medium
  - summary: >-
      Un fichier du front hors des quatre dossiers de l'hexagone (ex. src/outils.ts) est compté par coverage.include mais aucun seuil ne s'y applique.
    evidence: |-
      Les seuils Vitest ne portent que sur les quatre globs de couche. Côté back, ensureAllClassesAreContainedInArchitecture refuse déjà un paquet hors couche ; côté front, rien n'interdit un dossier hors couche (dependency-cruiser, protégé). À trancher : règle dependency-cruiser ou seuil global de repli.
    location: >-
      frontend/vite.config.ts
    severity: low
  - summary: >-
      Si un futur POM fixe `argLine` dans Surefire sans `@{argLine}`, l'agent JaCoCo disparaît, `jacoco.exec` n'est plus écrit et `check` saute en restant vert.
    evidence: |-
      prepare-agent passe par la propriété argLine ; aucun POM ne la fixe aujourd'hui (grep argLine vide), donc pas de défaut actuel. Mais check-gates-exist.sh ne lit que le POM effectif (plugin, goal check, minimum) et resterait OK ; JaCoCo journalise « Skipping JaCoCo execution due to missing execution data file » et sort en 0. À couvrir par le canari de couverture déjà reporté (scripts/gates/, protégé) : vérifier aussi la présence de target/jacoco.exec.
    location: >-
      pom.xml (jacoco-maven-plugin, prepare-agent)
    severity: medium
---

<intent-contract>

## Intent

**Problem:** Aucun contrôle ne refuse aujourd'hui le build quand une couche perd sa couverture : un `if` non testé dans `domain` ou une fonction non testée dans `src/application` passe tous les contrôles.

**Approach:** Configurer JaCoCo une seule fois dans le POM parent (`prepare-agent`, `report`, `check` en `verify`, règles par paquet) et `test.coverage` (provider `v8`) dans `frontend/vite.config.ts` avec des seuils par glob ; écrire les tests qui manquent pour atteindre les seuils.

## Boundaries & Constraints

**Always:**
- `jacoco-maven-plugin` 0.8.15 (propriété `jacoco.version`) dans `<build><plugins>` du seul `pom.xml` racine. Deux règles `<element>PACKAGE</element>` : `<include>*.domain*</include>` + `<include>*.application*</include>` à `<minimum>1.00</minimum>` en `LINE` et `BRANCH` (`COVEREDRATIO`) ; `*.adapter*` + `*.infrastructure*` à `<minimum>0.80</minimum>`, mêmes compteurs.
- `frontend/vite.config.ts` : `test.coverage.provider: 'v8'`, `include: ['src/**/*.{ts,tsx}']` (un fichier jamais importé par un test compte, à 0 %), `thresholds` : `src/domain/**` et `src/application/**` à 100, `src/adapters/**` et `src/infrastructure/**` à 80, chacun en `lines`, `branches`, `functions`, `statements`.
- Les seuils s'atteignent par des tests : `main` de chaque service et `frontend/src/infrastructure/main.tsx` sont testés.

**Never:**
- Redéclarer JaCoCo dans un POM de service ; `haltOnFailure` à `false` ; tout `<exclude>`, `coverage.exclude`, `/* v8 ignore */`, `@Generated` ou autre exclusion de couverture ; créer un `vitest.config.*`.
- Modifier un fichier protégé (`scripts/gates/**`, `docs/quality-gates.md`, `.bmad-loop/**`, `.claude/**`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/**`, golden master).
- Anticiper la 1.4 (proxy Vite, Kafka, compose) ou la 1.5 (PIT, Stryker).

</intent-contract>

## Code Map

- `pom.xml` -- POM parent (parent Boot 4.1.1, `archunit.version`) ; aucun `<build>` aujourd'hui : y ajouter le plugin JaCoCo. Le module parent est `pom` : `report`/`check` s'y sautent faute de `jacoco.exec`.
- `services/<s>/pom.xml` -- ne déclarent que `spring-boot-maven-plugin` : rien à y changer.
- `services/<s>/src/main/java/fr/nextbanking/<id>/infrastructure/Application*.java` -- `main` jamais appelé par les tests : `infrastructure` à 60 % en lignes (mesuré sur coeur-bancaire), seul paquet sous le seuil.
- `services/<s>/src/test/java/fr/nextbanking/<id>/infrastructure/Application*Test.java` -- tests de contexte existants ; y ajouter le test de `main`.
- `frontend/vite.config.ts` -- plugin React et `test.environment: 'node'` ; y ajouter `test.coverage`.
- `frontend/src/infrastructure/main.tsx` -- point d'entrée (lecture de `#root`, erreur s'il manque, `createRoot(...).render`) ; à 0 % dès que `include` le fait compter.
- `scripts/gates/check-gates-exist.sh` (lecture seule) -- lit le POM effectif : `jacoco-maven-plugin`, `<goal>check</goal>`, `<minimum>1(\.0+)?</minimum>`, `<minimum>0\.80?</minimum>`, `<haltOnFailure>false` ; front : `thresholds` et `src/domain` dans `vite.config.*`.
- `scripts/gates/build.sh` (lecture seule) -- `mvn clean verify`, ArchUnit, `npm ci`, depcruise, `tsc --noEmit`, `vitest run --coverage`.

## Tasks & Acceptance

**Execution:**
- [x] `pom.xml` -- propriété `jacoco.version` 0.8.15 et plugin JaCoCo avec ses trois exécutions et deux règles -- contrôle unique hérité par les deux services.
- [x] `services/<s>/src/test/java/fr/nextbanking/<id>/infrastructure/Application*Test.java` -- test qui appelle `main` (démarrage non web) et ferme le contexte démarré -- couvrir `infrastructure` sans exclusion.
- [x] `frontend/vite.config.ts` -- bloc `test.coverage` -- seuils par couche.
- [x] `frontend/src/infrastructure/main.test.tsx` -- deux cas : `#root` présent (rendu demandé), `#root` absent (erreur levée), avec `document` et `react-dom/client` simulés -- couvrir l'infrastructure du front.
- [x] `.gitignore` -- ignorer `frontend/coverage/` s'il ne l'est pas -- rapport généré.

**Acceptance Criteria:**
- Given le dépôt, when je lis le POM parent, then JaCoCo 0.8.15 y figure une fois avec `prepare-agent`, `report`, `check` (verify) et les deux règles ; aucun POM de service ne le redéclare, aucun `haltOnFailure` à `false`, aucune exclusion.
- Given le dépôt, when je lis `frontend/vite.config.ts`, then `test.coverage` (v8) porte les seuils par glob, et `ls frontend/vitest.config.* 2>/dev/null` est vide.
- Given le dépôt, when je lance `mvn -B -q verify -DfailIfNoTests=true`, then code 0 ; avec un `if` non couvert ajouté à `TemoinRecu` (domain de notifications), code non nul et le journal nomme `fr.nextbanking.notifications.domain`.
- Given le dépôt, when je lance `cd frontend && npx vitest run --coverage`, then code 0 ; avec une fonction non testée ajoutée dans `src/application`, code non nul.
- Given `<haltOnFailure>false</haltOnFailure>` ajouté temporairement à la configuration JaCoCo, when je lance `scripts/gates/check-gates-exist.sh`, then la sortie contient `KO  coeur-bancaire : haltOnFailure désactivé` et `KO  notifications : haltOnFailure désactivé`, code non nul.
- Given la story terminée, when je lance `scripts/gates/build.sh` et `scripts/gates/golden-master.sh`, then code 0 ; `check-gates-exist.sh` affiche pour chaque service les quatre lignes JaCoCo en `OK`, aucune ligne `haltOnFailure désactivé`, et `OK  front : seuils de couverture Vitest`, `OK  front : seuil dédié au domaine`.

## Spec Change Log

## Review Triage Log

### 2026-10-05 — Review pass
- verdicts: 26 findings — high 0, medium 3, low 15, false 8, maybe-false 0
- findings:
  - `[low]` `[defer]` (blind) Aucune règle par défaut hors des quatre couches — back réfuté (ensureAllClassesAreContainedInArchitecture refuse tout paquet hors couche) ; front réel : un src/outils.ts n'a aucun seuil ; cause antérieure (structure du front non imposée, dependency-cruiser protégé), reporté.
  - `[false]` `[reject]` (blind) Seuil 80 % agrégé et non par fichier — la story impose `<element>PACKAGE</element>` et des seuils « par couche » ; c'est le comportement voulu.
  - `[low]` `[reject]` (blind) Le spec cite `--context.listener.classes`, le code `--spring.main.sources` — correctif = éditer le spec ; écart consigné dans Auto Run Result (la propriété n'est plus lue par Boot 4.1.1).
  - `[low]` `[reject]` (blind) Double transtypage de `node:fs` dans architecture-temps.test.ts — @types/node exigerait une version vérifiée ; une signature fausse ferait échouer le test bruyamment.
  - `[false]` `[reject]` (blind) Dossier absent → ENOENT, extensions .mts/.js ignorées — l'échec bruyant est correct ; extensions déjà rejetées en 1.2 (aucun tel fichier).
  - `[low]` `[patch]` (blind) main.test ne vérifie pas l'élément rendu — corrigé : assertion StrictMode > Application, fichier renommé main.test.ts.
  - `[low]` `[reject]` (blind) ContexteDemarre dupliqué, référence statique périmée — duplication par service voulue (comme les tests ArchUnit) ; cas d'échec improbable et le test échoue alors.
  - `[low]` `[reject]` (blind) Le test de main dépend de la configuration réelle — même dépendance que le @SpringBootTest existant ; la 1.4 adaptera les deux.
  - `[false]` `[reject]` (blind) JaCoCo sans METHOD/INSTRUCTION — la story et docs/quality-gates.md fixent « lignes et branches » côté back.
  - `[medium]` `[defer]` (blind) Aucun canari ne garde les règles de couverture — groupé avec verification-gap ; scripts/gates protégé.
  - `[false]` `[reject]` (edge) Dossier domain/application manquant → ENOENT — échec bruyant au chargement, comportement correct.
  - `[low]` `[reject]` (edge) Nom de fichier avec '#', '?' ou '%' — improbable dans un projet TypeScript.
  - `[low]` `[reject]` (edge) Dossier dont le nom finit par .ts — improbable.
  - `[low]` `[defer]` (edge) Fichier src/ hors des quatre globs sans seuil — même cause que la première ligne.
  - `[false]` `[reject]` (edge) 80 % agrégé par glob — voulu (par couche).
  - `[low]` `[reject]` (edge) `*.application*` attrape un paquet `adapter.out.applicationevents` — motifs imposés littéralement par la story ; l'erreur va dans le sens strict.
  - `[false]` `[reject]` (edge) Paquet back hors couche sans règle — HexagonalArchitectureTest l'interdit (ensureAllClassesAreContainedInArchitecture).
  - `[low]` `[reject]` (edge) coeur-bancaire : contexte non fermé si l'événement prêt n'arrive pas — le test échoue alors, cas non atteignable aujourd'hui.
  - `[low]` `[reject]` (edge) notifications : même constat — idem.
  - `[medium]` `[defer]` (verification-gap) Seuils sans vérification automatique (include/glob modifiables sans alerte) — canari à ajouter sous scripts/gates (protégé).
  - `[low]` `[reject]` (verification-gap) Écart spec/code sur l'écouteur — correctif = éditer le spec ; consigné dans Auto Run Result.
  - `[false]` `[reject]` (intent) Travail non commité — le commit a lieu à la finalisation.
  - `[false]` `[reject]` (intent) Tâche .gitignore cochée sans changement — la tâche était conditionnelle, `frontend/coverage/` déjà ignoré (.gitignore:11).
  - `[low]` `[reject]` (intent) Mécanisme de l'écouteur différent du spec — même constat que ci-dessus.
  - `[low]` `[reject]` (intent) architecture-temps.test.ts modifié hors liste des tâches — nécessaire : `?raw` masquait un fichier non testé à la couverture ; contrôles du temps inchangés.
  - `[medium]` `[defer]` (intent) Tests négatifs seulement manuels — même cause que le canari manquant.

### 2026-10-05 — Review pass (suivi)
- verdicts: 23 findings — high 0, medium 3, low 9, false 11, maybe-false 0 ; 9 reconduits (`carried`)
- findings:
  - `[false]` `[reject]` (blind) Statut du spec (`in-review`) contraire à sprint-status.yaml (`done`) — état transitoire de cette passe de revue ; le spec repasse à `done` à la finalisation, sprint-status.yaml appartient à l'orchestrateur.
  - `[false]` `[reject]` (blind) Section Auto Run Result absente alors que le journal y renvoie — réécrite à la finalisation de cette passe.
  - `[low]` `[reject]` (blind) Design Notes citent encore `--context.listener.classes` — carried : correctif = éditer le spec ; écart consigné dans Auto Run Result.
  - `[low]` `[reject]` (blind) La tâche nomme `main.test.tsx`, le fichier livré est `main.test.ts` — correctif = éditer le spec ; renommage consigné dans le journal de la première passe.
  - `[false]` `[reject]` (blind) Tâche `.gitignore` cochée sans changement — carried : tâche conditionnelle, `frontend/coverage/` déjà ignoré (.gitignore:11).
  - `[low]` `[reject]` (blind) Critère JaCoCo invérifiable avec `-q` (le nom du paquet n'apparaît qu'en avertissement) — vrai, mais correctif = éditer le spec ; le build échoue bien (code non nul), seul le libellé du journal dépend de `-q`.
  - `[false]` `[reject]` (blind) `review_loop_iteration: 0` malgré une passe de revue — ce compteur ne compte que les retours bad_spec, aucun n'a eu lieu.
  - `[false]` `[reject]` (blind) Les entrées DW-1/DW-2 du registre n'ont pas le format des anciennes — c'est le format du registre de bmad-loop, écrit par l'orchestrateur ; les anciennes entrées relèvent de sa migration.
  - `[low]` `[reject]` (blind) DW-1 recoupe le report de canaris de la story 1.2 sans lien — correctif = éditer le registre, réservé à l'orchestrateur ; décision humaine de l'étape 10 dans les deux cas.
  - `[low]` `[reject]` (blind) Le test de `main` ne vérifie que le contexte actif — l'intention demande que `main` soit testé pour la couverture ; le comportement (assemblage, fuseau) est déjà vérifié par les tests @SpringBootTest du même fichier.
  - `[false]` `[reject]` (blind) Contexte non fermé si `main` lève après l'événement prêt — le seul écouteur de l'événement est `ContexteDemarre`, qui ne lève pas ; un seul test par service appelle `main`.
  - `[medium]` `[defer]` (blind) Un `argLine` futur sans `@{argLine}` retire l'agent JaCoCo, `check` saute en vert — groupé avec le constat verification-gap ci-dessous ; aucun `argLine` aujourd'hui ; reporté (canari sous scripts/gates/, protégé).
  - `[false]` `[reject]` (blind) architecture-temps.test.ts dépend de l'environnement `node` — sous jsdom ou happy-dom, Vitest tourne toujours dans Node et `process.getBuiltinModule` reste disponible ; une absence ferait échouer le test bruyamment.
  - `[false]` `[reject]` (edge) Tests sautés (`-DskipTests`) → `jacoco.exec` absent, `check` saute — `scripts/gates/build.sh` lance `mvn clean verify -DfailIfNoTests=true` sans saut de tests ; sauter les tests est un choix explicite hors contrôle.
  - `[medium]` `[defer]` (verification-gap) Aucun contrôle ne prouve que la porte JaCoCo s'exécute encore (cas `jacoco.exec` manquant non nommé par DW-1) — pré-vérifié ; reporté dans `deferred` (canari à étendre à la présence de `target/jacoco.exec`).
  - `[medium]` `[defer]` (intent) Seuls les cas verts sont exercés, aucun test automatique de régression de couverture — carried : même cause que le canari manquant, déjà reporté.
  - `[false]` `[reject]` (intent) « Par couche » : agrégé par glob au front, par paquet au back — carried : voulu (par couche).
  - `[low]` `[defer]` (intent) Fichier front hors des quatre couches mesuré sans seuil — carried : déjà reporté (DW-2).
  - `[low]` `[reject]` (intent) `*.application*` attrape `adapter.out.applicationevents` — carried : motifs imposés par la story, l'erreur va dans le sens strict.
  - `[low]` `[reject]` (intent) `main` testé sous des arguments modifiés, mécanisme différent des Design Notes — carried : même constat que l'écart d'écouteur.
  - `[low]` `[reject]` (intent) architecture-temps.test.ts modifié hors de l'approche — carried : nécessaire, `?raw` masquait un fichier non testé.
  - `[false]` `[reject]` (intent) sprint-status.yaml à `done`, spec à `in-review` — même réfutation que le premier constat.
  - `[false]` `[reject]` (intent) Tâche `.gitignore` cochée sans changement — carried, comme ci-dessus.

## Design Notes

- Les noms de paquet de JaCoCo `check` sont en notation pointée (`fr.nextbanking.coeurbancaire.infrastructure`) : `*.domain*` couvre `domain` et ses sous-paquets, `*.application*` couvre `application.port.*` (paquets d'interfaces sans ligne : ratio indéfini, ignoré par JaCoCo).
- Fermer le contexte lancé par `main` sans changer sa signature : passer `--spring.main.web-application-type=none` et `--context.listener.classes=<écouteur de test>` ; l'écouteur (classe de test, `ApplicationListener<ApplicationReadyEvent>`) garde le contexte prêt, que le test ferme ensuite.
- Vitest 5 ne compte que les fichiers chargés par un test sauf `coverage.include` : sans lui, un fichier non testé ajouté échapperait aux seuils. Les fichiers de test sont exclus d'office.

## Verification

**Commands:**
- `mvn -B -q verify -DfailIfNoTests=true` -- expected: code 0.
- `cd frontend && npx vitest run --coverage` -- expected: code 0.
- `scripts/gates/build.sh` -- expected: code 0.
- `scripts/gates/check-gates-exist.sh | grep -E 'JaCoCo|seuil|haltOnFailure|couverture'` -- expected: 8 lignes `OK` back, 2 lignes `OK` front, aucune `haltOnFailure`.
- `scripts/gates/golden-master.sh` -- expected: code 0.
- `git diff --name-only 88a2facb805ac44af396cf9eaf6bdd1105d72a4c -- scripts/gates .bmad-loop .claude docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` -- expected: vide.
- Tests négatifs des critères (temporaires, puis retirés) -- expected: code non nul à chaque fois.

## Auto Run Result

Status: done

**Résumé :** passe de revue de suivi sur la story 1.3 (couverture imposée par couche). Code inchangé depuis cd37780 : JaCoCo 0.8.15 dans le POM parent (`prepare-agent`, `report`, `check` en `verify`, règles PACKAGE 1.00 sur domain/application, 0.80 sur adapter/infrastructure, lignes et branches) ; Vitest `test.coverage` (v8, `include: src/**/*.{ts,tsx}`, seuils par glob 100/100/80/80) dans `frontend/vite.config.ts` ; tests de `main` des deux services et de `frontend/src/infrastructure/main.tsx`.

**Fichiers modifiés (story, commit cd37780) :**
- `pom.xml` -- propriété `jacoco.version` et plugin JaCoCo.
- `frontend/vite.config.ts` -- bloc `test.coverage`.
- `frontend/src/infrastructure/main.test.ts` -- tests du point d'entrée (`#root` présent : rendu StrictMode > Application ; absent : erreur).
- `frontend/src/architecture-temps.test.ts` -- lecture des sources par le système de fichiers au lieu d'`import.meta.glob('?raw')`.
- `services/<s>/src/test/.../infrastructure/Application*Test.java` -- test de `main` (démarrage non web, contexte fermé).
- `services/<s>/src/test/.../infrastructure/ContexteDemarre.java` -- écouteur de test (sans annotation) qui garde le contexte démarré par `main`.
- Cette passe : seul ce spec (journal de tri, report, résultat).

**Écart au spec (rappel) :** l'écouteur passe par `--spring.main.sources` et non `--context.listener.classes` (Boot 4.1.1 ne lit plus cette propriété).

**Revue (suivi) :** 23 constats — 0 correctif, 4 reports (dont 1 nouveau, inscrit dans `deferred` : un `argLine` futur sans `@{argLine}` ferait sauter `check` en vert ; 3 reconduits, déjà au registre), 19 rejetés (motifs dans le Review Triage Log : 11 faux, 8 bas — correctif limité à une édition du spec ou du registre, ou comportement voulu). Correctifs par verdict : high 0, medium 0, low 0. Nouvelle revue recommandée : non (passe de suivi sans correctif high : le travail a convergé).

**Vérification :**
- `scripts/gates/build.sh` : code 0 (couverture front 100 % sur 4 fichiers, paquets back au-dessus des seuils).
- `scripts/gates/check-gates-exist.sh` : 4 lignes JaCoCo `OK` par service, `OK  front : seuils de couverture Vitest`, `OK  front : seuil dédié au domaine`, aucune ligne `haltOnFailure` ; `KO` restants : stories suivantes (seuil de mutation : 1.5).
- `scripts/gates/golden-master.sh` : code 0.
- Diff des fichiers protégés depuis la base 88a2facb805ac44af396cf9eaf6bdd1105d72a4c : vide.

**Risques résiduels :** aucun canari automatique des seuils (reporté, DW-1, et nouveau report sur `jacoco.exec` manquant) ; `ContexteDemarre` doit rester sans annotation ; un futur test chargeant des sources en `?raw` masquerait un fichier non couvert ; critère JaCoCo du spec rédigé avec `-q` alors que le nom du paquet ne s'affiche que sans `-q`.
