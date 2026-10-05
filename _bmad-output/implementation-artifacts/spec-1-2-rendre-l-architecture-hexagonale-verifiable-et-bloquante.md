---
title: 'Story 1.2 — Rendre l''architecture hexagonale vérifiable et bloquante'
type: 'chore'
created: '2026-10-05'
status: 'done'
baseline_commit: '7d002a6c2004bd0882535bc79ec42305475915de'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/quality-gates.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Rien n'empêche aujourd'hui un framework, un adaptateur ou une lecture de l'heure système d'entrer dans `domain`/`application` (back) ou `src/domain`/`src/application` (front).

**Approach:** Ajouter dans chaque service trois tests ArchUnit bloquants (hexagone, horloge, clients HTTP) et le port `Horloge` avec son adaptateur système ; côté front, créer `.dependency-cruiser.cjs` (trois règles en `error`) et un test Vitest qui interdit la lecture de l'heure du navigateur dans le métier.

## Boundaries & Constraints

**Always:**
- Par service, `HexagonalArchitectureTest` avec exactement deux règles : `Architectures.onionArchitecture()` (`domainModels("..domain..")`, `applicationServices("..application..")`, adaptateurs `in` = `..adapter.in..`, `out` = `..adapter.out..`, `infrastructure` = `..infrastructure..`) et `noClasses()` de `..domain..`/`..application..` ne dépendant de classes hors de `java..`, `<racine>.domain..`, `<racine>.application..`.
- `HorlogeArchitectureTest` : hors `..adapter.out.horloge..` et `..adapter.out.jeton..`, aucun appel à une méthode `now` d'un type de `java.time`, aucune dépendance à `java.time.Clock`, aucun appel à `System.currentTimeMillis`. `ClientsHttpArchitectureTest` : hors `..adapter.out.fournisseur..`, aucune dépendance à `RestClient`, `WebClient`, `RestTemplate`, `java.net.http.HttpClient` (désignés par nom qualifié).
- `archRule.failOnEmptyShould` reste `true` ; `archunit-junit5` 1.5.1 en test ; classes de test exclues de l'import ArchUnit.
- Port `application.port.out.Horloge` (`instant()`, `aujourdhui()` en `LocalDate` du fuseau métier, `fuseau()`), adaptateur `adapter.out.horloge` en horloge système lisant `nb.fuseau` (obligatoire, `ZoneId` valide, sinon échec au démarrage), `nb.fuseau=Europe/Paris` dans `application.properties` des deux services.
- `frontend/.dependency-cruiser.cjs` : `domain-ne-depend-de-rien`, `application-ne-depend-pas-des-adaptateurs`, `hexagone-sans-paquet-npm` (types `npm`, `npm-dev`, `npm-optional`, `npm-peer`, `npm-bundled`, `npm-no-pkg`, `npm-unknown`, `core` ; `*.test.ts(x)` exclus), toutes `severity: "error"`, `options.tsPreCompilationDeps: true` ; `dependency-cruiser` 18.5.0 exact en dev.
- `frontend/src/architecture-temps.test.ts` : parcourt `src/domain` et `src/application` (hors tests) et échoue sur `Date.now()`, `new Date()` sans argument, `Temporal.Now`.

**Never:**
- Modifier `scripts/gates/**`, `docs/quality-gates.md`, `.trivyignore`, `config/**`, `.claude/**`, `.bmad-loop/**`, ni le golden master. Seul fichier protégé créé : `frontend/.dependency-cruiser.cjs`.
- Ajouter une règle à `HexagonalArchitectureTest`, désactiver un test, ou anticiper JaCoCo, seuils, proxy, Kafka, web (stories 1.3 et suivantes), profil `rejeu`.

</frozen-after-approval>

## Code Map

- `services/<s>/src/main/java/fr/nextbanking/<id>/{domain,application,adapter/in,adapter/out,infrastructure}` -- squelette de la 1.1 (Temoin/TemoinRecu, ports, adaptateurs en mémoire, `Application*` avec `scanBasePackages`).
- `services/<s>/src/test/.../infrastructure/Application*Test.java` -- dépend d'adaptateurs : exclure les tests de l'import ArchUnit (`ImportOption.DoNotIncludeTests`).
- `scripts/gates/canary-arch.sh` -- écrit `domain/CanaryViolation.java` qui référence le premier type `public` trouvé sous `adapter` ; attend `Architecture Violation` dans `target/surefire-reports`.
- `scripts/gates/check-gates-exist.sh` -- motifs : `*ArchitectureTest.java`, `archunit-junit5` dans le POM effectif, `severity: *["']error` et `dependencyTypes` dans `.dependency-cruiser.cjs`, `"dependency-cruiser"` dans `package.json`.
- `scripts/gates/build.sh` -- `mvn verify -DfailIfNoTests=true`, `mvn test -Dtest='*ArchitectureTest'`, puis `npm ci`, depcruise, `vitest run --coverage`.

## Tasks & Acceptance

**Execution:**
- [x] `pom.xml` -- déclarer `archunit-junit5` 1.5.1 (propriété de version + `dependencyManagement`) -- version unique.
- [x] `services/<s>/pom.xml` -- dépendance `archunit-junit5` en `test`.
- [x] `services/<s>/src/test/java/fr/nextbanking/<id>/{Hexagonal,Horloge,ClientsHttp}ArchitectureTest.java` -- les trois classes, une par fichier, `@AnalyzeClasses(packages = "fr.nextbanking.<id>", importOptions = DoNotIncludeTests.class)`.
- [x] `services/<s>/src/main/java/fr/nextbanking/<id>/application/port/out/Horloge.java` et `.../adapter/out/horloge/HorlogeSysteme.java` -- port + adaptateur `@Component` (constructeur public avec `nb.fuseau`, constructeur de test avec une `Clock` fixe) ; tests de l'adaptateur (instant, date dans le fuseau, fuseau invalide refusé).
- [x] `services/<s>/src/main/resources/application.properties` -- `nb.fuseau=Europe/Paris`.
- [x] `frontend/.dependency-cruiser.cjs`, `frontend/package.json`, `frontend/package-lock.json` -- configuration et dépendance.
- [x] `frontend/src/architecture-temps.test.ts` -- test du temps métier.

**Acceptance Criteria:**
- Given le dépôt, when je lance `mvn -B -q test -Dtest='*ArchitectureTest'`, then code 0 ; un import `org.springframework.stereotype.Component` dans `domain` de `notifications`, un `Instant.now()` dans `domain` de `coeur-bancaire`, une dépendance à `org.springframework.web.client.RestClient` dans `adapter.in` de `coeur-bancaire` donnent chacun un code non nul.
- Given le dépôt, when je lance `scripts/gates/canary-arch.sh services/coeur-bancaire fr/nextbanking/coeurbancaire` puis la même chose pour `notifications`, then chacun sort en 0 avec `OK  ArchUnit détecte la dépendance domaine -> adaptateur`, sans fichier résiduel.
- Given le dépôt, when je lance `cd frontend && npx depcruise --config .dependency-cruiser.cjs src`, then code 0 ; `import { useState } from "react"` dans `src/application`, ou un import de `src/adapters` depuis `src/domain`, donnent un code non nul.
- Given le dépôt, when je lance `cd frontend && npx vitest run src/architecture-temps.test.ts`, then code 0 ; `const d = new Date();` dans `src/domain` donne un code non nul.
- Given la story terminée, when je lance `scripts/gates/build.sh`, then code 0 ; `scripts/gates/check-gates-exist.sh` affiche en `OK` les lignes ArchUnit des deux services et les trois lignes dependency-cruiser du front ; `scripts/gates/golden-master.sh` sort en 0.

## Implementation Notes

- `onionArchitecture()` exige par défaut une couche « domain service » non vide ; le spec ne déclare que `domainModels`, d'où `.withOptionalLayers(true)` sur la règle oignon (pas de règle ajoutée ; `failOnEmptyShould` reste `true` et la seconde règle exige toujours des classes dans `..domain..`/`..application..`).
- `HorlogeSysteme` : constructeur public `@Autowired` lisant `${nb.fuseau}` (propriété absente : échec de résolution Spring ; vide ou invalide : `IllegalStateException`), constructeur package-private avec une `Clock` pour les tests. Vérifié : `-Dnb.fuseau=Mars/Olympus` fait échouer le démarrage du contexte.
- Après revue : `HorlogeArchitectureTest` utilise `accessTargetWhere` (appels et références de méthode `Instant::now`, `System::currentTimeMillis`), paquet propriétaire `java.time*` (inclut `java.time.chrono`) ; la règle oignon porte `.ensureAllClassesAreContainedInArchitecture()` (une classe dans `<racine>.config` est refusée) ; `Application*Test` vérifie le fuseau chargé (`Europe/Paris`) et l'échec du démarrage avec `--nb.fuseau=Mars/Olympus` (argument de ligne de commande, car `properties(...)` ne passe pas devant `application.properties`).
- `ClientsHttpArchitectureTest` vise aussi les types imbriqués (`RestClient$Builder`…) par préfixe `nom$`.
- `.dependency-cruiser.cjs` : `domain-ne-depend-de-rien` porte sur le code du front hors `src/domain` ; les paquets npm et modules Node sont couverts par `hexagone-sans-paquet-npm` (tests exclus, ce qui laisse `vitest` aux tests du domaine).
- `architecture-temps.test.ts` lit les sources par `import.meta.glob(..., { query: '?raw' })` (typé par `vite/client`, pas besoin de `@types/node`) ; il refuse aussi `new Date` sans parenthèses et vérifie qu'il trouve bien des fichiers dans les deux dossiers.
- Tests négatifs joués puis retirés : Spring dans `domain` (notifications), `Instant.now()` dans `domain` (coeur-bancaire), `Clock` et `System.currentTimeMillis` dans `infrastructure`, `RestClient` dans `adapter.in` avec `spring-web` temporaire (et admis dans `adapter.out.fournisseur`) ; front : `react` (valeur et `import type`) dans `application`, `adapters` et `application` depuis `domain`, `adapters` depuis `application`, `node:fs` dans `domain`, `new Date()` dans `domain`. Tous en code non nul.

## Spec Change Log

## Review Triage Log

| Constat | Verdict | Preuve | Suite |
|---|---|---|---|
| Fuseau chargé par Spring jamais vérifié (verification-gap) | medium | tests de l'adaptateur sur littéral ; `nb.fuseau=UTC` passerait | patch |
| Références de méthode `Instant::now` / `System::currentTimeMillis` non vues (edge, blind) | medium | `callMethodWhere` ne couvre que `JavaMethodCall` | patch |
| `java.time.chrono` ignoré (`equals("java.time")`) (edge, blind) | low | correction directe du même prédicat | patch (avec le précédent) |
| Classes hors des cinq couches non vérifiées (blind) | medium | onion ne vérifie que les couches déclarées | patch (`ensureAllClassesAreContainedInArchitecture`, même règle) |
| Échec au démarrage sur fuseau invalide non testé par Spring (blind) | low | constructeur seul testé | patch (test seulement) |
| `withOptionalLayers(true)` (écart signalé par l'implémentation) | low | requis : aucune couche « domain service » ; la règle « aucun framework » exige toujours des classes en domain/application | accepté |
| Pas de canari pour Horloge, clients HTTP, dependency-cruiser (verification-gap) | medium | `canary-arch.sh` ne plante qu'une dépendance domaine → adaptateur | defer (scripts protégés) |
| Liste du temps incomplète : `nanoTime`, `java.util.Date`, `Calendar`, `ZoneId.systemDefault`, `TimeZone.getDefault`, `InstantSource` (edge, blind) | medium | AD-19 fixe la liste ; élargir = décision d'architecture | defer |
| Liste des clients HTTP incomplète : `RestOperations`, `HttpURLConnection`, interfaces HTTP, OkHttp, Apache (edge, blind) | medium | AD-4 fixe la liste ; élargir = décision d'architecture | defer |
| dependency-cruiser : imports non résolus (`unknown`, `undetermined`), `no-circular` (edge, blind) | medium | types fixés par AD-2 ; fichier protégé après l'empreinte | defer |
| Motifs `..domain..` non ancrés à la racine (blind) | low | paquets imbriqués improbables | rejeté |
| `java..` entier admis dans domain/application (blind) | false | règle voulue par `docs/quality-gates.md` (« seules dépendances autorisées, java.* ») | rejeté |
| Fuseau autre que Europe/Paris accepté (edge, blind) | low | la valeur des deux services est désormais vérifiée par le test de contexte | rejeté |
| Contournements et faux positifs de `architecture-temps.test.ts` (alias, `performance.now`, commentaires) (edge, blind) | low | AD-23 fixe les trois motifs ; alias improbables | rejeté |
| `Intl...resolvedOptions().timeZone` côté front (blind) | medium | lit le fuseau du navigateur ; hors des motifs d'AD-23 | defer (avec la liste du temps) |
| Domaine important hors de `src/` (edge) | low | improbable | rejeté |
| Code dupliqué entre services (blind) | low | choix de la spine (tests par service) | rejeté |
| `package-lock.json` absent du diff (blind) | false | exclu volontairement du diff de revue, commité avec la story | rejeté |
| Pas de script npm pour depcruise (blind) | low | `build.sh` l'appelle | rejeté |
| `Clock` nulle dans le constructeur de test (edge) | low | constructeur réservé aux tests | rejeté |
| Extensions `.js`/`.mts` non parcourues (edge) | low | aucun tel fichier, projet TypeScript | rejeté |

## Design Notes

- `RestClient` et `WebClient` ne sont pas sur le classpath : les désigner par nom qualifié (`DescribedPredicate` sur `getName()`), pas par classe. Le test négatif `RestClient` exige `spring-web` : l'ajouter temporairement le temps du test, puis le retirer.
- La règle « aucun framework » utilise `resideOutsideOfPackages("java..", "fr.nextbanking.<id>.domain..", "fr.nextbanking.<id>.application..")` : les types `java.time` du port Horloge sont donc admis.
- `failOnEmptyShould` à `true` : chaque `that()` doit trouver des classes (les cinq paquets ne sont pas vides depuis la 1.1).
- Les fichiers protégés se comparent depuis `baseline_commit` (le `main` local a des commits non poussés, `origin/main` fausse la comparaison).

## Verification

**Commands:**
- `mvn -B -q test -Dtest='*ArchitectureTest'` -- expected: code 0.
- `scripts/gates/canary-arch.sh services/coeur-bancaire fr/nextbanking/coeurbancaire && scripts/gates/canary-arch.sh services/notifications fr/nextbanking/notifications && git status --short services` -- expected: deux `OK`, aucun fichier résiduel.
- `cd frontend && npx depcruise --config .dependency-cruiser.cjs src && npx vitest run src/architecture-temps.test.ts` -- expected: code 0.
- `scripts/gates/build.sh` -- expected: code 0.
- `scripts/gates/check-gates-exist.sh | grep -E 'ArchUnit|archunit|dependency-cruiser|sans paquet npm'` -- expected: 7 lignes `OK`.
- `scripts/gates/golden-master.sh` -- expected: code 0.
- `git diff --name-only 7d002a6c2004bd0882535bc79ec42305475915de -- scripts/gates .bmad-loop .claude docs/quality-gates.md .trivyignore config e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` -- expected: vide.
- Tests négatifs des critères (temporaires, puis restaurés) -- expected: code non nul à chaque fois.
