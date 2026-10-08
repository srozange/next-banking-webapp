---
title: 'Confiner le jeton dans le décorateur fetch du front'
type: 'refactor'
created: '2026-10-08'
status: 'done'
baseline_revision: 'd3f44036c97a0d33519aeb9de94f44e69534f285'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** Le jeton d'authentification circule dans `frontend/src/domain`/`application` (ports `lire(jeton)`, `StockageJeton`, `DecodeurJeton`, `ExpirationJeton`, `jetonOuFin`) et dans les écrans ; un client HTTP peut donc être câblé sans jeton, comme sur le tableau de bord, le virement, le relevé et la revue (correctif `d23bf7d`).

**Approach:** Le jeton ne vit plus que dans les adaptateurs : `fetchAuthentifie` est seul à poser `Authorization`, l'application ne parle que d'une session (rôles, fin de session, secondes restantes) sans jamais voir le jeton, et des tests de garde échouent si la composition ou une signature régresse. Aucun comportement visible ne change (aucun écran, point d'API ni scénario golden master).

## Boundaries & Constraints

**Always:**
- Rien dans `src/domain` ni `src/application` (hors fichiers de test) ne contient, sans tenir compte de la casse, `jeton`, `Authorization`, `Bearer` ni `nb.jeton` : ni type, ni port, ni paramètre, ni commentaire.
- `frontend/src/adapters/session/fetchAuthentifie.ts` est le seul fichier source (hors tests) de `frontend/src` à écrire `Authorization`.
- Dans `src/infrastructure/main.tsx`, tout client HTTP appelant un service authentifié reçoit `fetchAuthentifie`. Seul le client de connexion (non authentifié) reçoit `fetch`.
- Comportement inchangé : même clé `nb.jeton`, mêmes chemins, mêmes 401 → jeton supprimé + signal d'expiration + retour `/login`, même seuil de renouvellement (600 s), mêmes textes, mêmes `data-testid`.
- Ports de l'application nouvellement introduits : un seul port de session (ex. `SessionLocale` : `roles()`, `terminer()`, `secondesRestantes()`), implémenté en adaptateur au-dessus de `StockageJetonLocal`, `decodeurJetonJwt` et `ExpirationJetonJwt` (qui peuvent rester des détails d'adaptateur, mais hors `application`).
- La connexion et le renouvellement enregistrent le jeton dans l'adaptateur (le client HTTP reçoit le stockage), l'application ne reçoit que les rôles / un statut.
- Déconnexion : l'appel `client.deconnecter()` est lancé avant `session.terminer()` pour que `fetchAuthentifie` lise encore le jeton de façon synchrone.
- Textes, tests existants adaptés aux nouvelles signatures sans en supprimer ni désactiver aucun ; couverture 100 %/branches sur domain et application, seuils inchangés, aucune exclusion ajoutée.

**Never:**
- Modifier `scripts/gates/**`, `.bmad-loop/**`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`, ni `sprint-status.yaml`.
- Abaisser un seuil, ajouter une exclusion de couverture, désactiver un test, créer `vitest.config.*`.
- Rejouer le golden master. Introduire un texte, un `data-testid` ou une route nouveaux.
- Faire passer le jeton par un contexte React, une prop d'écran ou un cas d'usage.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Appel authentifié | jeton stocké, client HTTP câblé avec `fetchAuthentifie` | requête avec `Authorization: Bearer <jeton>` posé par le décorateur seul | 401 : jeton supprimé, signal déclenché |
| Client mal câblé | un client de `main.tsx` reçoit `fetch` | `npx vitest run` échoue, message nommant le client fautif | — |
| Signature fautive | un fichier de `src/application` mentionne `jeton`/`Authorization`/`Bearer`/`nb.jeton` | `npx vitest run` échoue, nomme le fichier | — |
| Connexion acceptée | identifiants valides, rôle connu | jeton enregistré par l'adaptateur, l'application reçoit les rôles et rend la route d'accueil | rôle inconnu : technique, rien n'est enregistré |
| Déconnexion | session ouverte | requête de déconnexion portant le jeton, puis jeton supprimé, route `/login` | panne réseau ignorée |
| Renouvellement | secondes restantes < 600 | jeton neuf enregistré par l'adaptateur, résultat `renouvelee` | 401 : session terminée (`deconnecte`) ; autre : `echec` |
| Session illisible | jeton absent, illisible ou sans rôle connu | `roles()` rend null, jeton supprimé si présent | — |

</intent-contract>

## Code Map

- `frontend/src/adapters/session/fetchAuthentifie.ts` -- décorateur existant, seul à poser `Authorization` ; reste la référence (règle 401).
- `frontend/src/infrastructure/main.tsx`, `main.test.ts` -- composition (13 clients, tous sauf `creerClientConnexionHttp` avec `fetchAuthentifie`) ; le test existant n'en vérifie que 2.
- `frontend/src/application/` -- porteurs de jeton : `seConnecter` (`StockageJeton`, `DecodeurJeton`, `ReponseConnexion.jeton`), `seDeconnecter`, `renouvelerSession` (`ExpirationJeton`, `ReponseRenouvellement.jeton`), `chargerProfil` (`rolesDeLaSession`), `consulterBourse` (`jetonOuFin`), `consulterProduits`, `dialoguerAvecSophie`, `gererWatchlist`, `comptesTiers`, `prets`, `virement`, `revue`, `consulterReleve`, `consulterTableauDeBord` + tests.
- `frontend/src/adapters/http/Client*Http.ts` (+ tests) -- clients ; retirent le paramètre `jeton` (le décorateur pose l'en-tête). `ClientConnexionHttp` et `ClientSessionHttp` reçoivent le stockage.
- `frontend/src/adapters/{routes,gabarit,ecrans/**,test/rendreApplication.tsx}` -- passent `stockage`/`decodeur`/`jeton` aux cas d'usage ; passent la session à la place.
- `frontend/src/adapters/{stockage/StockageJetonLocal,jeton/DecodeurJetonJwt,session/ExpirationJetonJwt,session/session}.ts` -- briques existantes à composer en adaptateur de session.
- `frontend/src/architecture-temps.test.ts`, `motifs-interdits.test.ts` -- modèle (lecture des sources par `fs`) pour le nouveau test de garde.
- Lecture seule : `frontend/.dependency-cruiser.cjs`, `docs/quality-gates.md`.

## Tasks & Acceptance

**Execution:**
- `frontend/src/application/**` -- remplacer `StockageJeton`/`DecodeurJeton`/`ExpirationJeton`/`jetonOuFin` par un port de session sans jeton ; retirer `jeton` des signatures de tous les ports sortants et cas d'usage ; `ReponseConnexion`/`ReponseRenouvellement` sans jeton -- le jeton quitte l'application.
- `frontend/src/adapters/**` (clients HTTP, session, routes, gabarit, écrans, `test/rendreApplication.tsx`) -- adaptateur de session ; clients sans paramètre jeton ; connexion et renouvellement enregistrent le jeton dans l'adaptateur ; écrans et routes reçoivent la session.
- `frontend/src/infrastructure/main.tsx` -- composition adaptée, tous les clients authentifiés avec `fetchAuthentifie`.
- `frontend/src/infrastructure/main.test.ts` -- test de composition : pour chaque client authentifié de `main.tsx`, un appel avec jeton stocké doit porter `Authorization`, le message d'échec nomme le client ; tout client câblé avec `fetch` fait échouer le test.
- `frontend/src/confinement-jeton.test.ts` (nouveau) -- garde : aucune mention (insensible à la casse) de `jeton`/`Authorization`/`Bearer`/`nb.jeton` dans les sources de `domain` et `application` ; `fetchAuthentifie.ts` seul fichier source de `src` à contenir `Authorization` ; chaque détecteur est lui-même testé sur des exemples fautifs.
- tests existants -- adaptés aux nouvelles signatures, mêmes cas conservés.

**Acceptance Criteria:**
- Given le dépôt, when je cherche `jeton`, `Authorization`, `Bearer`, `nb.jeton` dans `frontend/src/domain` et `frontend/src/application` (hors tests), then aucune occurrence.
- Given le dépôt, when je cherche `Authorization` dans les sources de `frontend/src` (hors tests), then seul `adapters/session/fetchAuthentifie.ts` apparaît.
- Given `main.tsx`, when un client authentifié est câblé avec `fetch`, then `cd frontend && npx vitest run` sort en code non nul et nomme le client.
- Given un cas d'usage de `src/application` dont la signature reçoit le jeton, when `npx vitest run`, then code non nul.
- Given la story terminée, when `cd frontend && npx vitest run --coverage` puis `npx tsc --noEmit`, then code 0, seuils tenus sans exclusion.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh`, then les quatre sortent en code 0 et `git diff --name-only` sur les chemins interdits est vide.

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 3, false 2, maybe-false 0
- findings:
  - `[low]` `reject` Critère 1 interdit le mot « jeton » en plus des trois motifs cités — c'est le choix explicite de la spec (« sans tenir compte de la casse »), cohérent avec « aucun type ne transporte le jeton » ; aucun préjudice nommé.
  - `[low]` `reject` Critère 5 gardé par motif lexical : un paramètre nommé autrement que `jeton` passerait — convention du dépôt en français, tsc et revue couvrent le reste ; une garde structurelle ajouterait de la complexité disproportionnée.
  - `[low]` `reject` Liste codée en dur de 11 clients dans main.test.ts — elle égale l'ensemble des clients de main.tsx (vérifié : 11 hors connexion) et le second test lit main.tsx lui-même, donc un client ajouté câblé avec `fetch` est détecté.
  - `[false]` `reject` Regex `[^)]*` contournable — les appels de fabrique de main.tsx n'ont pas d'arguments imbriqués ; aucun scénario atteignable montré.
  - `[false]` `reject` Écart de portée (70 fichiers) et dérivation depuis la spec — la story impose de retirer le jeton des signatures ; critères 6 et 7 vérifiés par exécution (vitest 1203 tests verts, tsc 0, scripts de contrôle 0, fichiers protégés inchangés).

## Auto Run Result

Status: done

- Résumé : le jeton ne vit plus que dans les adaptateurs. Nouveau port `SessionLocale` (application) sans jeton ; tous les ports sortants et cas d'usage perdent le paramètre jeton ; connexion et renouvellement enregistrent le jeton dans l'adaptateur ; `fetchAuthentifie` est seul à poser `Authorization`.
- Fichiers : 72 sous `frontend/src` (application, adapters http/session/routes/gabarit/écrans, `infrastructure/main.tsx`) ; nouveaux `application/session.ts`, `adapters/session/SessionLocaleJwt.ts`, `confinement-jeton.test.ts` ; `main.test.ts` vérifie chaque client authentifié.
- Revue : patches 0, reports 0, 5 constats rejetés (raisons ci-dessus). Relecture de suivi recommandée : false.
- Vérification : `npx vitest run --coverage` (1203 tests verts, seuils tenus), `npx tsc --noEmit` = 0, `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` = 0 (rapportés par l'implémenteur) ; grep du spec vide ; diff des chemins protégés vide.
- Risques résiduels : un jeton illisible est désormais traité comme « pas de session » par bourse, produits, prêts et comptes tiers (avant, il était envoyé au serveur) — impact visible nul.

## Design Notes

Esquisse du port (application) :

```ts
export interface SessionLocale {
  roles(): readonly string[] | null; // null : aucune session ; jeton illisible/sans rôle connu supprimé par l'adaptateur
  terminer(): void;
  secondesRestantes(): number | null;
}
```
`seConnecter(identifiant, mdp, client)` rend `{ type:'accepte', route }` ; `ClientConnexion.connecter` rend `{ statut:'acceptee', roles }` après avoir enregistré le jeton lui-même (à ne faire qu'après vérification d'un rôle connu : l'adaptateur de connexion n'enregistre que si `accueilDuRole(roles)` est connu — ou le cas d'usage appelle un `session.ouvrir()` sans jeton ; au choix du dev, tant que le jeton n'est enregistré qu'avec un rôle connu).

## Verification

**Commands:**
- `cd frontend && npx vitest run --coverage` -- expected: code 0, seuils tenus
- `cd frontend && npx tsc --noEmit` -- expected: code 0
- `scripts/gates/build.sh` -- expected: code 0
- `scripts/gates/check-gates-exist.sh` -- expected: code 0
- `scripts/gates/golden-master.sh` -- expected: code 0
- `scripts/gates/trace-smoke.sh` -- expected: code 0
- `grep -rniE "jeton|Authorization|Bearer" frontend/src/domain frontend/src/application --include=*.ts --exclude=*.test.ts` -- expected: aucune ligne
