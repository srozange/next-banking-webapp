---
title: 'Story 1.1 — Poser le squelette des deux services et du front'
type: 'chore'
created: '2026-10-05'
status: 'done'
baseline_commit: 'f6afb5de9d1e0c9038aea7faf509e0c2ed0dc8f8'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/quality-gates.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Le dépôt ne contient aucun code : les contrôles des stories 1.2 à 1.6 (ArchUnit, dependency-cruiser, JaCoCo, Vitest, PIT, Stryker) n'ont rien sur quoi s'appliquer.

**Approach:** Poser un POM parent Spring Boot 4.1.1 / Java 21 avec deux modules hexagonaux minimaux (`coeur-bancaire`, `notifications`) et un front React/TypeScript hexagonal minimal sous Vite, chacun avec au moins un test réel, build vert.

## Boundaries & Constraints

**Always:**
- Versions uniquement de `versions-verifiees.md` : Spring Boot 4.1.1, Java 21, React 19.3.0, TypeScript 5.9.3, Vite 8.3.2, `@vitejs/plugin-react` 6.1.2, `vitest` et `@vitest/coverage-v8` 5.0.3, versions exactes (sans `^`) dans `package.json`.
- Paquets racine `fr.nextbanking.coeurbancaire` et `fr.nextbanking.notifications` ; cinq paquets `domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure`, chacun avec au moins une classe ; au moins un type `public` sous `adapter`.
- `domain` et `application` en Java pur (`java.*` et code du service seulement) ; `src/domain` et `src/application` sans import npm hors tests. Tout React sous `src/adapters`, point d'entrée sous `src/infrastructure`.
- Code métier et noms en français ; commit `type : description` en français.

**Never:**
- Toucher aux fichiers protégés (`scripts/gates/**`, `docs/quality-gates.md`, `.trivyignore`, `config/dependency-check-suppressions.xml`, `.claude/**`, `.bmad-loop/**`, golden master : `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`).
- Anticiper les stories suivantes : pas d'ArchUnit, JaCoCo, dependency-cruiser, seuil de couverture, proxy, Kafka, base de données, web, `compose.yaml`, thème (`npm run tokens`), profils `mutation`/`security`.
- Créer un `frontend/vitest.config.*` ; utiliser `now()`, `Clock`, `Date.now()` ou `new Date()` dans le code produit.

</frozen-after-approval>

## Code Map

- `scripts/gates/build.sh` -- protégé ; lance aussi `*ArchitectureTest` et depcruise (story 1.2) : il ne peut pas être vert en 1.1, ne pas le modifier.
- `scripts/gates/canary-arch.sh` -- protégé ; suppose `src/main/java/<package>/domain` et un type `public` sous `adapter` : la structure doit le permettre dès maintenant.
- `scripts/gates/check-gates-exist.sh` -- protégé ; lit `ls vitest.config.* vite.config.*` : seul `vite.config.ts` doit exister.
- `scripts/gates/golden-master.sh` -- contrôle statique, doit rester à 0.
- `.gitignore` -- à compléter (`target/`, `node_modules/`, `frontend/dist/`, `frontend/coverage/`).
- Outillage local constaté : JDK 21.0.10, Maven 3.9.14, Node 24.14.0, npm 11.9.0.

## Tasks & Acceptance

**Execution:**
- [x] `pom.xml` -- POM parent `fr.nextbanking:next-banking-webapp` (packaging `pom`), parent `spring-boot-starter-parent` 4.1.1, `java.version` 21, modules `services/coeur-bancaire` et `services/notifications`, encodage UTF-8 -- socle commun des services.
- [x] `services/coeur-bancaire/pom.xml`, `services/notifications/pom.xml` -- héritent du parent (`relativePath ../../pom.xml`), dépendances `spring-boot-starter` et `spring-boot-starter-test` (test), plugin `spring-boot-maven-plugin` -- modules minimaux.
- [x] `services/<service>/src/main/java/fr/nextbanking/<id>/...` -- par service, un mini-hexagone cohérent : objet-valeur du domaine (`domain`), port entrant + cas d'usage + port sortant (`application`, `application.port.in`, `application.port.out`), adaptateur entrant public (`adapter.in`), adaptateur sortant public en mémoire (`adapter.out`), classe `@SpringBootApplication` (scan de la racine du service) et configuration d'assemblage du cas d'usage (`infrastructure`) -- chaque paquet non vide, sans framework dans domain/application.
- [x] `services/<service>/src/test/java/...` -- tests JUnit 5 : règle de l'objet-valeur (cas valide + cas refusé), cas d'usage avec un double du port sortant, et chargement du contexte Spring -- au moins un test par module.
- [x] `frontend/package.json`, `frontend/package-lock.json` -- privé, `type: module`, scripts `dev`, `build`, `test` (`vitest run`), `typecheck` (`tsc --noEmit`) ; dépendances exactes : `react`, `react-dom` 19.3.0 ; dev : `typescript` 5.9.3, `vite` 8.3.2, `@vitejs/plugin-react` 6.1.2, `vitest` 5.0.3, `@vitest/coverage-v8` 5.0.3, `@types/react` 19.3.0, `@types/react-dom` 19.3.0 -- lock versionné.
- [x] `frontend/vite.config.ts`, `frontend/tsconfig.json`, `frontend/index.html` -- plugin React seul, environnement de test `node`, aucun seuil ni proxy.
- [x] `frontend/src/{domain,application,adapters,infrastructure}/` -- règle pure de domaine + test ; cas d'usage avec port + test ; composant React racine dans `adapters` ; `main.tsx` (création de la racine React) dans `infrastructure`.
- [x] `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md` -- ajouter `react-dom` 19.3.0, `@types/react` 19.3.0, `@types/react-dom` 19.3.0, vérifiés sur le registre npm, avec source et date.

**Acceptance Criteria:**
- Given le dépôt, when je liste les fichiers, then `pom.xml` déclare exactement les deux modules et `mvn -q -f services/<service>/pom.xml help:effective-pom` sort en 0 pour chacun.
- Given le dépôt, when je lance `mvn -B -q verify -DfailIfNoTests=true`, then code 0, chaque module exécutant au moins un test ; un module sans test ou un test faux dans `domain` de `coeur-bancaire` donne un code non nul.
- Given le dépôt, when je lance `cd frontend && npm ci --silent && npx vitest run`, then code 0 avec au moins un test ; un test faux dans `src/domain` donne un code non nul.
- Given le dépôt, when je vérifie `frontend/`, then les quatre dossiers contiennent du TypeScript, `package-lock.json` est suivi par git, `vite.config.ts` existe et aucun `vitest.config.*`.
- Given la story terminée, when je lance `scripts/gates/golden-master.sh`, then code 0, et le `git diff --name-only` depuis `origin/main` sur les fichiers protégés est vide.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Constat | Verdict | Preuve | Suite |
|---|---|---|---|
| `-0.00` rend `-0` (montant.ts) — 3 relecteurs | low | `negatif ? -centimes : centimes` avec centimes = 0 | patch |
| Ancres `^`/`$` du format non testées (verification-gap) | low | aucun test de refus n'échoue sans `$` ('100.555') | patch |
| Composant React et `data-testid` racine jamais chargés par un test (verification-gap, blind) | medium | aucun test n'importe `Application` ni `main.tsx` | patch |
| `build.sh` ne lance pas `tsc` (verification-gap) | medium | `scripts/gates/build.sh` : npm ci, depcruise, vitest seulement | defer (script protégé, décision humaine) |
| Pas de garde d'entier sûr (edge, blind) | low | dépasse `MAX_SAFE_INTEGER` au-delà de ~9e13 €, improbable ; ajoute une garde | rejeté |
| Zéros de tête acceptés (edge, blind) | low | `007.50` accepté ; aucun producteur ne l'émet, format non spécifié | rejeté |
| Idempotence de la réception (edge) | false | adaptateur en mémoire, aucun rejeu possible avant Kafka (story 1.4, `evenements_traites`) | rejeté |
| Pas de champ `engines` / version Node (edge, blind) | low | poste 24.14.0, retenue 24.21.0 ; même écart que Maven, signalé | defer |
| `package-lock.json` absent du diff (edge, blind) | false | exclu volontairement du diff de revue ; présent et commité avec la story | rejeté |
| Titre `<title>next-banking-webapp</title>` inventé (blind) | low | MESSAGES.md : `BankApp Demo - {titre de page}` ; aucune page en 1.1 | defer (première story d'écran) |
| Tests négatifs sans message (blind) | low | assertions sur le type seul ; affaire de la mutation (1.5) | rejeté |
| `Temoin` sans identifiant côté producteur (blind) | maybe-false | le contrat du témoin est défini par la story 1.4 | rejeté (hors 1.1, conçu en 1.4) |
| `tous()` publics hors des ports (blind) | low | échafaudage de test, remplacé par la persistance en 1.4 | rejeté |
| `.env` et `.secrets/` non ignorés (blind) | medium | AGENTS.md : non versionnés ; `.gitignore` ne les couvre pas ; préexistant | defer |
| TODO d'AGENTS.md « commandes par module » (blind) | low | fichier de contexte d'agent | defer (refresh bmad-project-context) |
| Messages d'exception inventés (blind) | false | exceptions techniques internes, pas des textes d'interface | rejeté |
| `noUncheckedIndexedAccess` absent (blind) | low | réglage de style, aucun défaut constaté | rejeté |

## Design Notes

- `build.sh` exige `*ArchitectureTest` et `.dependency-cruiser.cjs` (story 1.2) : en 1.1, la preuve est faite par les commandes des critères, pas par `build.sh`.
- `react-dom` et les types React sont indispensables (`createRoot`, compilation TSX) mais absents de `versions-verifiees.md` : versions alignées sur React 19.3.0, vérifiées le 2026-10-05 sur le registre npm, consignées dans ce fichier.
- Maven 3.9.16 est retenu dans `versions-verifiees.md` ; le poste a 3.9.14 : aucun wrapper ni enforcer ajouté dans cette story (non demandé), écart signalé.
- Le `@SpringBootApplication` vit dans `infrastructure` : déclarer `scanBasePackages` sur la racine du service pour que les adaptateurs soient trouvés.

## Verification

**Commands:**
- `mvn -B -q verify -DfailIfNoTests=true` -- expected: code 0.
- `for s in coeur-bancaire notifications; do mvn -q -f services/$s/pom.xml help:effective-pom >/dev/null; done` -- expected: code 0.
- `cd frontend && npm ci --silent && npx vitest run && npm run typecheck` -- expected: code 0.
- `scripts/gates/golden-master.sh` -- expected: code 0.
- `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` -- expected: vide.
- Tests négatifs (temporaires, puis restaurés) : test faux dans `domain` de `coeur-bancaire` ; module sans test ; test Vitest faux dans `src/domain` -- expected: code non nul à chaque fois.
