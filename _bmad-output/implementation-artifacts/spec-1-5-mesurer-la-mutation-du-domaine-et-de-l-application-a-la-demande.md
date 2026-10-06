---
title: 'Mesurer la mutation du domaine et de l''application à la demande'
type: 'chore'
created: '2026-10-05'
status: 'done'
baseline_revision: '57255e61d93ce52897405dc67cfe8af96beed9c3'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** Rien ne détecte aujourd'hui un test qui couvre le code de `domain` ou d'`application` sans rien vérifier : la couverture à 100 % (story 1.3) passe même sans assertion. `scripts/gates/mutation.sh` (protégé, déjà présent) appelle PIT et StrykerJS, qui ne sont pas encore configurés.

**Approach:** Déclarer PIT (1.30.0 + `pitest-junit5-plugin` 1.2.3) uniquement dans un profil Maven `mutation` du POM parent, et StrykerJS 10.0.0 (core + vitest-runner) dans `frontend/` avec `frontend/stryker.config.json`, pour que `mutation.sh` sorte en 0 et que deux violations volontaires (assertions retirées) le fassent échouer.

## Boundaries & Constraints

**Always:**
- Versions exactes : `pitest-maven` 1.22.1, `pitest-junit5-plugin` 1.2.3, `@stryker-mutator/core` et `@stryker-mutator/vitest-runner` 10.0.0 (épinglées, sans `^`), `package-lock.json` mis à jour par `npm install`.
- PIT : configuration au niveau du plugin (pas d'une exécution, car `mutation.sh` appelle le goal directement), `targetClasses` = `fr.nextbanking.*.domain.*` et `fr.nextbanking.*.application.*`, `<mutationThreshold>85</mutationThreshold>`, `historyInputFile` et `historyOutputFile` = `${user.home}/.cache/next-banking-webapp/pit/${project.artifactId}.history`.
- Stryker : `testRunner` `vitest`, `mutate` sur `src/domain` et `src/application` hors fichiers `*.test.*`, `thresholds` `{ "high": 90, "low": 85, "break": 85 }`, `incrementalFile` `reports/stryker-incremental.json`.
- Un score sous 85 se corrige en renforçant les assertions des tests de domain/application (code métier en français, style AssertJ / Vitest existant).
- Les violations volontaires des AC sont faites puis annulées ; elles ne sont jamais committées.

**Never:**
- Modifier `scripts/gates/**`, `docs/quality-gates.md`, `.bmad-loop/**`, `.claude/settings.json`, `.claude/hooks/**`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, golden master (`e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`), ni `sprint-status.yaml`.
- Mentionner `pitest-maven` hors du profil `mutation` (ni `pluginManagement`, ni propriété nommée ainsi) : `check-gates-exist.sh` le détecterait dans le POM effectif par défaut.
- Toute exclusion de mutation (`excludedClasses`, `excludedMethods`, `avoidCallsTo`, `excludedTestClasses`, `ignoreStatic`, mutateur désactivé, `// Stryker disable`), tout seuil abaissé, tout `vitest.config.*`.
- Rejouer le golden master.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Nominal | dépôt propre, `scripts/gates/mutation.sh` | code 0 ; `~/.cache/next-banking-webapp/pit/{coeur-bancaire,notifications}.history` créés ; `frontend/reports/stryker-incremental.json` créé et ignoré par git | — |
| Cache absent | `~/.cache/next-banking-webapp/pit/` inexistant | PIT crée le fichier d'historique (si PIT ne crée pas le dossier, le faire créer par le build Maven du profil, jamais en modifiant `mutation.sh`) | — |
| Violation Java | assertions retirées de `TemoinTest` (coeur-bancaire, test conservé) | `mutation.sh` code non nul | annuler la violation |
| Violation front | assertions retirées de `src/domain/montant.test.ts` | `cd frontend && npx stryker run --incremental` code non nul | annuler la violation |
| Build par défaut | `mvn -B -q verify -DfailIfNoTests=true` | PIT ne s'exécute pas | — |

</intent-contract>

## Code Map

- `pom.xml` -- POM parent (Boot 4.1.1, packaging `pom`, JaCoCo dans `build/plugins`) ; y ajouter `<profiles><profile><id>mutation</id>` portant `pitest-maven` + dépendance `pitest-junit5-plugin`. PIT saute de lui-même les modules `pom`. JaCoCo `prepare-agent` pose `argLine` : vérifier que PIT tourne malgré tout (sinon configurer PIT dans le profil, sans toucher JaCoCo hors profil).
- `services/coeur-bancaire/` (artifactId `coeur-bancaire`) -- cibles : `domain/Temoin.java`, `domain/TemoinEcrit.java`, `application/EcrireTemoinCasUsage.java`, ports (interfaces, sans mutant). Tests : `src/test/java/fr/nextbanking/coeurbancaire/{domain,application}/*Test.java`.
- `services/notifications/` (artifactId `notifications`) -- cibles : `domain/TemoinRecu.java`, `application/RecevoirTemoinCasUsage.java`. Tests homologues.
- `frontend/package.json` -- ajouter les deux paquets Stryker en `devDependencies`, version exacte 10.0.0 ; `frontend/package-lock.json` régénéré.
- `frontend/stryker.config.json` -- à créer.
- `frontend/vite.config.ts` -- config Vitest unique (coverage thresholds par glob) ; ne pas dupliquer en `vitest.config.*`.
- `frontend/src/domain/montant.ts` + `montant.test.ts` ; `frontend/src/application/lireMontant.ts` + test -- cibles Stryker.
- `.gitignore` -- `reports/` (sans `/` initial) couvre déjà `frontend/reports/` ; ajouter `.stryker-tmp/` (bac à sable de Stryker).
- `scripts/gates/mutation.sh` (lecture seule) -- `mvn -B -q -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage` puis `(cd frontend && npx stryker run --incremental)`.
- `scripts/gates/check-gates-exist.sh:22-56` (lecture seule) -- `help:effective-pom` par service avec/sans `-Pmutation` ; greps `pitest-maven`, `<mutationThreshold>85</mutationThreshold>`, `<historyInputFile>` ; front : `"@stryker-mutator/core"` dans `package.json`, `break["']?: *85` dans `stryker.config.*`.

## Tasks & Acceptance

**Execution:**
- `pom.xml` -- ajouter le profil `mutation` (PIT 1.22.1, `pitest-junit5-plugin` 1.2.3, configuration ci-dessus, `timestampedReports` à `false`) -- seul endroit où PIT apparaît.
- `frontend/package.json`, `frontend/package-lock.json` -- `npm install --save-dev --save-exact @stryker-mutator/core@10.0.0 @stryker-mutator/vitest-runner@10.0.0`.
- `frontend/stryker.config.json` -- créer avec `testRunner`, `mutate`, `thresholds`, `incrementalFile`, `reporters` (`clear-text`, `progress`, `html`), `coverageAnalysis` `perTest`.
- `.gitignore` -- ajouter `.stryker-tmp/`.
- tests `domain`/`application` (Java et front) -- renforcer les assertions seulement si un score est sous 85.
- vérifier les deux violations volontaires, puis les annuler (`git diff` vide sur les tests).

**Acceptance Criteria:**
- Given le POM parent, when on lit le POM effectif de chaque service sans profil, then `pitest-maven` n'y figure pas ; avec `-Pmutation`, PIT, le seuil 85, `historyInputFile`/`historyOutputFile` et les `targetClasses` y figurent, sans exclusion.
- Given `frontend/stryker.config.json` et `frontend/package.json`, when on les lit, then ils portent la configuration et les versions exigées.
- Given le dépôt, when on lance `scripts/gates/mutation.sh`, then code 0, les deux fichiers `.history` existent, `frontend/reports/stryker-incremental.json` existe et `git check-ignore frontend/reports/stryker-incremental.json` sort en 0.
- Given une violation volontaire (Java ou front), when on lance la commande correspondante, then code non nul.
- Given la story terminée, when on lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh`, then codes 0 (hors `KO` de la story 1.6 dans `check-gates-exist.sh`) et les lignes `OK` mutation/Stryker attendues présentes, aucune `PIT actif hors profil mutation`.
- Given la story terminée, when `git diff --name-only` sur les fichiers protégés listés dans l'AC de l'epic, then vide.

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `scripts/gates/mutation.sh; echo $?` -- expected: 0
- `test -f ~/.cache/next-banking-webapp/pit/coeur-bancaire.history && test -f ~/.cache/next-banking-webapp/pit/notifications.history && git check-ignore frontend/reports/stryker-incremental.json` -- expected: 0
- `mvn -B -q verify -DfailIfNoTests=true 2>&1 | grep -i pitest` -- expected: aucune sortie
- `scripts/gates/build.sh && scripts/gates/golden-master.sh && scripts/gates/trace-smoke.sh` -- expected: 0
- `scripts/gates/check-gates-exist.sh | grep -Ei 'mutation|PIT|Stryker'` -- expected: 8 lignes `OK`, aucune `KO`
- `git diff --name-only HEAD -- scripts/gates .bmad-loop .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` -- expected: vide

## Auto Run Result

Status: blocked
Blocking condition: implementation verification failed — seuil de mutation inatteignable avec les versions imposées ; deux décisions humaines requises.

**Commande en échec :** `scripts/gates/mutation.sh` (code non nul).

**Blocage 1 — l'historique PIT n'existe plus en version libre (PIT 1.22.1).**
- Avec `historyInputFile` configuré, PIT s'arrête : `History has been enabled but no history plugin has been installed/activated. If you are using https://www.arcmutate.com remember to activate the history plugin with +arcmutate_history`.
- Preuve : `pitest-entry-1.30.0.jar` ne contient plus que `org/pitest/mutationtest/incremental/ErroringHistoryFactory` comme fabrique d'historique (l'historique est passé dans le plugin payant Arcmutate).
- Contradiction : l'AC et `scripts/gates/check-gates-exist.sh` exigent `<historyInputFile>` et les fichiers `.history`, alors que `mutation.sh` doit sortir en 0.
- Sans historique (essai annulé), PIT obtient 100 % sur `coeur-bancaire` et `notifications` : le seuil 85 n'est pas en cause.
- Options (humain) : (a) retirer l'exigence d'historique de `check-gates-exist.sh`, `docs/quality-gates.md`, de l'epic et de l'AC ; (b) inscrire dans `versions-verifiees.md` une version de PIT qui embarque encore l'historique libre ; (c) adopter Arcmutate.

**Blocage 2 — `@stryker-mutator/vitest-runner` 10.0.0 incompatible avec vitest 5.0.3.**
- Le runner filtre les tests par nom joint avec une espace (`node_modules/@stryker-mutator/vitest-runner/dist/src/test-helpers.js:11`, `nameParts.join(' ')`), vitest 5 nomme avec ` > ` (`node_modules/vitest/dist/task-utils.js:43`). Tout test sous `describe` est sauté pendant les passes de mutants : score 25,93 % (7 tués, 20 survivants), aussi avec `--coverageAnalysis off`.
- Sur une copie, sans `describe`, le score monte à 96,30 % : contournement non appliqué au dépôt.
- Options (humain) : (a) convention « pas de `describe` » dans les tests de `src/domain` et `src/application` ; (b) revenir à vitest 4.x (dernière 4.1.11, à vérifier avec Stryker 10) ; (c) attendre un runner Stryker compatible.

**Travail fait, non commité (arbre de travail) :** `pom.xml` (profil `mutation` : PIT 1.22.1, `pitest-junit5-plugin` 1.2.3, `targetClasses`, seuil 85, historique, aucune exclusion), `frontend/package.json` et `package-lock.json` (Stryker core et vitest-runner 10.0.0 exacts), `frontend/stryker.config.json`, `.gitignore` (`.stryker-tmp/`), `epic-1-context.md` recompilé.

**Ce qui passe :** `scripts/gates/build.sh` et `scripts/gates/golden-master.sh` en 0 ; `check-gates-exist.sh` affiche les 8 lignes `OK` mutation/Stryker, aucune `PIT actif hors profil mutation` (seuls `KO` restants : story 1.6) ; le build par défaut ne lance pas PIT ; aucun fichier protégé modifié. Non lancés : `trace-smoke.sh`, violations volontaires (pas de vert de référence).

## Résolution (2026-10-06, arbitrage délégué par le propriétaire)

- Blocage 1 : PIT **1.22.1**, dernière version à historique gratuit (`DefaultHistoryFactory` dans `pitest-entry` 1.22.1, absent à partir de 1.23.0). Aucune exigence d'historique retirée, aucun contrôle modifié.
- Blocage 2 : `vitest` et `@vitest/coverage-v8` **4.1.11** au lieu de 5.0.3 (incompatibilité du runner Stryker 10 avec la notation « > » de vitest 5). Pas de convention « sans `describe` ».
- Inscrit dans `versions-verifiees.md`. Vérifié : `mutation.sh` code 0 (PIT 100 % sur les deux services, historique réutilisé au 2e passage ; Stryker 96,30 %), `build.sh` et `golden-master.sh` code 0, `check-gates-exist.sh` sans `KO` hors story 1.6. Négatifs : règle de domaine non testée → PIT 25 % sous 85, code 1 ; tests du domaine front sans assertions → Stryker 62,96 % sous 85, code 1.
