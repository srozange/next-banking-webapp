---
title: 'Naviguer dans son espace selon son rôle'
type: 'feature'
created: '2026-10-06'
baseline_revision: 'e4a55d696f1d10e64ac31ad83e443b31aaef4e93'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** Après la connexion (story 2.3), les pages authentifiées sont vides : ni en-tête, ni salutation, ni liens, et un CLIENT qui ouvre `/administration` ou `/espace-conseiller` y reste. Le golden master n'a en outre aucun moyen de vérifier statiquement qu'un écran livré porte sa route et ses `data-testid`.

**Approach:** Poser le gabarit authentifié (en-tête 72px à liens filtrés par rôle, badge de rôle, « Bonjour, {nomComplet} » lu par `GET /api/identite-clients/moi`, sous-en-tête 48px sur les deux espaces réservés, zone de contenu `aria-busy`), renvoyer tout rôle insuffisant vers `/tableau-de-bord` sans rendre la page, rappeler les motifs interdits dans un README du front, et livrer `scripts/golden-master/verifier-scenario.sh`.

## Boundaries & Constraints

**Always:**
- Textes exacts (legacy, `docs/retro/00-global/MESSAGES.md` §1.2-1.4, FEAT-015, FEAT-016) : « DB », « Demo Bank », les 12 liens dans cet ordre « Tableau de bord », « Virement », « Virements programmes », « Releve », « Budget », « Prets », « Documents », « Bourse », « Mon Conseiller », « Produits », « Comptes Tiers », « Beneficiaires », puis « Espace Conseiller » (CONSEILLER, ADMIN), « Administration » (ADMIN) ; « Bonjour, {nomComplet} » ; « Deconnexion » ; badges « Conseiller » / « ADMIN » ; sous-en-têtes « Mon portefeuille » · « Conseiller IA » et « Administration » · « Espace Conseiller » · « Mon tableau de bord ». Aucun autre texte visible, aucun texte de chargement.
- Routes cibles (golden-master-architecture.md) : `/tableau-de-bord`, `/virement`, `/virements-programmes`, `/releve`, `/budget`, `/prets`, `/documents`, `/bourse`, `/mon-conseiller`, `/produits`, `/comptes-tiers`, `/beneficiaires`, `/espace-conseiller`, `/administration`. « Mon portefeuille » → `/espace-conseiller`, « Conseiller IA » → `/mon-conseiller`, « Mon tableau de bord » → `/tableau-de-bord`. Seules les trois routes déjà déclarées le restent ; un lien vers une route non déclarée suit la redirection existante.
- `data-testid` tous préfixés `nav-`, écrits en littéraux dans le source (jamais composés), uniques sur la page : `nav-deconnexion` figé (SC-001-05) ; liens d'en-tête `nav-{route sans /}` (ex. `nav-tableau-de-bord`, `nav-virements-programmes`) ; sous-en-tête `nav-sous-{élément}` ; `nav-salutation`, `nav-badge-role`.
- Rôles d'affichage et de garde lus dans le jeton (`DecodeurJeton`, priorité ADMIN > CONSEILLER > CLIENT) ; `/moi` ne fournit que `nomComplet`. Garde : `/espace-conseiller` pour CONSEILLER et ADMIN, `/administration` pour ADMIN ; refus → `Navigate` remplacement vers `/tableau-de-bord`, contenu jamais rendu.
- Hexagone : règles de navigation (liens par rôle, badge, sous-navigation, garde) dans `src/domain`, chargement du profil (port `ClientProfil`) dans `src/application`, 100 % couverts ; React, `fetch`, `NavLink` dans `src/adapters`. Styles uniquement par `var(--…)` de `tokens.css`, éléments natifs (`a`, `button`) stylés seulement dans `src/adapters/ui/`.
- Le script ne lit que `docs/retro/golden-master/derived/` (aucune liste recopiée), n'écrit que `reports/golden-master/textes-non-trouves.md`, vit dans `scripts/golden-master/`.

**Never:**
- Ne pas modifier `scripts/gates/**`, `frontend/.dependency-cruiser.cjs`, `docs/quality-gates.md`, les seuils de `vite.config.ts` ; aucune modification sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; ne jamais rejouer le golden master.
- Pas d'action de déconnexion, de renouvellement ni d'intercepteur 401 global (story 2.5) : `nav-deconnexion` est un `<button type="button">` sans effet ; pas de contenu d'écran pour les trois routes ; pas de menu repliable, pas de nouvelle dépendance npm.
- Jamais de jeton ni de mot de passe dans la console ; jamais de détail technique affiché.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| CLIENT | jeton `roles:[CLIENT]`, `/tableau-de-bord`, `/moi` 200 `{"nomComplet":"Jean Dupont"}` | 12 liens, pas de badge, « Bonjour, Jean Dupont », `nav-tableau-de-bord` `aria-current="page"`, aucun sous-en-tête | — |
| CONSEILLER | `roles:[CONSEILLER]`, `/espace-conseiller` | 13 liens (+ Espace Conseiller), badge « Conseiller », sous-en-tête Mon portefeuille · Conseiller IA | — |
| ADMIN | `roles:[ADMIN]`, `/administration` | 14 liens, badge « ADMIN », sous-en-tête Administration · Espace Conseiller · Mon tableau de bord ; `/espace-conseiller` ouvert sans redirection | — |
| Rôle insuffisant | CLIENT sur `/administration` ou `/espace-conseiller` ; CONSEILLER sur `/administration` | Remplacement vers `/tableau-de-bord`, sans message | Contenu demandé jamais rendu |
| Profil en attente | `/moi` sans réponse | `main` `aria-busy="true"`, pas de salutation | — |
| Profil refusé | `/moi` 401 | jeton supprimé, remplacement vers `/login` | sans message |
| Profil en panne | `/moi` 5xx, réseau, JSON sans `nomComplet` | `aria-busy` levé, en-tête sans salutation | aucun texte affiché |

</intent-contract>

## Code Map

- `frontend/src/adapters/routes/routes.tsx` -- `creerRoutes(Dependances)`, `ROUTES_PROTEGEES` (littéraux lus par le script) : ajouter `clientProfil` aux dépendances et brancher le gabarit sous `RouteProtegee`.
- `frontend/src/adapters/routes/RouteProtegee.tsx` -- garde actuelle via `accueilDeLaSession` ; à étendre : rôles de session puis `routeAutorisee(pathname, roles)`.
- `frontend/src/adapters/Application.tsx` -- lien d'évitement (premier enfant du conteneur, testé) + `<main id="contenu-principal" tabIndex={-1} data-testid="application-racine">` ; l'en-tête doit précéder `main` (landmark `banner`), donc l'en-tête ne peut pas vivre dans l'`Outlet` de `main`.
- `frontend/src/adapters/ui/LienEvitement.tsx` -- focalise `cible.current` (ref) ; à garder fonctionnel si `main` change d'emplacement.
- `frontend/src/application/seConnecter.ts` -- ports `StockageJeton`, `DecodeurJeton`, `accueilDeLaSession` (purge un jeton illisible) : réutiliser pour obtenir les rôles de session sans dupliquer.
- `frontend/src/domain/connexion.ts` -- `ACCUEILS` (priorité des rôles), `ROUTE_CONNEXION` : modèle des règles pures.
- `frontend/src/adapters/http/ClientConnexionHttp.ts` -- motif d'adaptateur `fetch` injectable (`appeler`) à reprendre pour `ClientProfilHttp`.
- `frontend/src/adapters/textes.ts` -- `TEXTES` : y ajouter une section `navigation`.
- `frontend/src/adapters/test/rendreApplication.tsx` -- rend l'application en mémoire ; ajouter un `clientProfil` simulé (défaut : profil « Jean Dupont »).
- `frontend/src/infrastructure/main.tsx` + `main.test.ts` -- assemblage ; y ajouter `creerClientProfilHttp`.
- `frontend/src/adapters/routes/routes.test.tsx:29-36` -- attend `application-racine` vide sur les routes protégées : reste vrai (le gabarit est hors du contenu de la route).
- `services/identite-clients/src/main/java/fr/nextbanking/identiteclients/adapter/in/http/PointEntreeProfil.java:24-41` -- `GET /api/identite-clients/moi` → `{"identifiant","nomComplet","roles"}`, 401 si sujet absent (lecture seule).
- `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/DESIGN.md` l.270-284 (`header`, `sub-header`), l.329 (`tag`), l.495-509 (`app-nav` : `item-gap` 24px, soulignement 2px) ; tokens `--color-primary`, `--color-on-primary`, `--color-primary-container`, `--color-on-primary-container`, `--color-ink`, `--color-surface`.
- `docs/retro/golden-master/derived/FEAT-001/SC-001-04/{input-actions.json,expected-dom.txt}` (start_url `/tableau-de-bord`, 3 lignes dont « MES COMPTES DEMO BANK »), `SC-001-05/input-actions.json` (sélecteur `[data-testid='nav-deconnexion']`, valeur `wait_for.text`), `FEAT-003/SC-003-01` (start_url `/virement`), `FEAT-001/SC-001-03` (HTTP : `input-request.json`, pas d'`input-actions.json`) -- lecture seule ; dossier = `FEAT-{NNN}` tiré de `SC-{NNN}-{MM}`.
- `services/*/reference-data/reference-data.sql` -- seconde source de recherche des textes (« Jean Dupont »).
- `.gitignore` -- `reports/` ignoré : le rapport n'est pas versionné. `jq` et `python3` disponibles.

## Tasks & Acceptance

**Execution:**
- `frontend/src/domain/navigation.ts` (+ test) -- `liensDuRole(roles)` (12 liens + conditionnels, clé et route), `badgeDuRole(roles)`, `sousNavigation(chemin)`, `routeAutorisee(chemin, roles)` (préfixe `route` ou `route/`), `ROUTE_TABLEAU_DE_BORD` -- règles FR-030-04, FR-001-11, FR-016-01, UX-DR70/71.
- `frontend/src/application/chargerProfil.ts` (+ test) -- port `ClientProfil.lire(jeton)` → `profil(nomComplet)` / `nonAuthentifie` / `technique` ; cas d'usage : sans jeton ou 401 → supprime le jeton, `deconnecte` ; panne → `indisponible` ; et `rolesDeLaSession(stockage, decodeur)` (même purge que `accueilDeLaSession`, qui s'appuie dessus).
- `frontend/src/adapters/http/ClientProfilHttp.ts` (+ test) -- `GET /api/identite-clients/moi`, `Authorization: Bearer`, `Accept: application/json`.
- `frontend/src/adapters/Application.tsx`, `ui/LienEvitement.tsx` -- `Application` ne rend plus `main` : lien d'évitement + `Outlet` ; nouveau `ui/ContenuPrincipal.tsx` (le `main` figé, `aria-busy` optionnel) ; le lien focalise l'élément d'`id` cible.
- `frontend/src/adapters/gabarit/` -- `GabaritAuthentifie` (en-tête, sous-en-tête, `ContenuPrincipal` busy, chargement du profil, `Navigate` vers `/login` si `deconnecte`), `EnTete`, `SousEnTete` (+ CSS par composant, `flex-wrap`, sans media query de repli) ; liens `NavLink` (`end`) ; mise en forme `tag` du badge dans `ui/Etiquette.tsx`.
- `frontend/src/adapters/routes/RouteProtegee.tsx`, `routes.tsx` -- route `/login` enveloppée dans `ContenuPrincipal` ; garde par rôle puis `GabaritAuthentifie` ; `clientProfil` dans `Dependances`.
- `frontend/src/adapters/textes.ts`, `test/rendreApplication.tsx`, `infrastructure/main.tsx` (+ `main.test.ts`) -- textes, outil de test, assemblage.
- Tests DOM (`jsdom`, tagués) -- `[FR-030-04]` liens et badge par rôle, ordre et orthographe ; `[FR-001-05]` salutation `/moi` ; `[FR-001-11][FR-016-01][FR-030-02]` redirections sans rendu ; `[FR-030-03]` `/` par rôle ; `aria-current`, sous-en-tête, absence de bouton repliable, `aria-busy`, 401 et panne de `/moi` ; `axe` sur `/tableau-de-bord` et `/administration` ; lien d'évitement toujours premier et fonctionnel.
- `frontend/src/motifs-interdits.test.ts` -- lit `src/adapters/**/*.tsx` hors tests : aucun `draggable`, `onDrag*`, `IntersectionObserver`, `onMouseEnter`/`:hover` portant seul une action, `<dialog` ; vérifie que le README cite les six motifs -- UX-DR110 rendu contrôlable.
- `frontend/README.md` -- rôle du front, commandes (`npm run tokens`, `npx vitest run`), conventions (`data-testid`, textes, tokens) et la liste des six motifs interdits (UX-DR110).
- `scripts/golden-master/verifier-scenario.sh` -- `bash`, `set -euo pipefail`, `jq` ; échecs (code 1, message nommant le scénario et l'élément) : identifiant invalide ou inconnu, scénario sans `input-actions.json`, `start_url` absente des littéraux de `frontend/src/adapters/routes/*.tsx` (hors `*.test.*`), `data-testid` absent de `frontend/src` (hors `*.test.*` et `adapters/test/`) ; textes : `grep -rF` dans `frontend/src`, puis `services/*/reference-data/reference-data.sql`, puis les `value` des actions ; non trouvés → section `## SC-…` (remplacée, autres sections conservées) de `reports/golden-master/textes-non-trouves.md` ; code 0.

**Acceptance Criteria:**
- Given la story terminée, when `cd frontend && npx vitest run -t 'FR-001-05|FR-001-11|FR-016-01|FR-030-02|FR-030-03|FR-030-04'`, puis `-t 'axe'` et `-t 'UX-DR110'`, then code 0.
- Given le golden master dérivé, when `scripts/golden-master/verifier-scenario.sh SC-001-04` puis `SC-001-05`, then code 0, et `reports/golden-master/textes-non-trouves.md` contient une section « SC-001-04 » listant « MES COMPTES DEMO BANK ».
- Given `data-testid="nav-deconnexion"` retiré de `frontend/src`, when le script sur `SC-001-05`, then code non nul nommant `SC-001-05` et `nav-deconnexion` ; `SC-003-01` → non nul nommant `/virement` ; `SC-001-03`, `SC-999-99`, `toto` → non nul nommant le scénario.
- Given le script lancé sur SC-001-04 et SC-001-05, when `git status --porcelain docs/retro/golden-master/derived e2e/golden-master replay-mapping.json`, then rien n'est affiché.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 7 findings — high 0, medium 0, low 4, false 3, maybe-false 0
- findings:
  - `[false]` `[reject]` (Intent Alignment) La chaîne réelle `user1` → « Jean Dupont » par le vrai `/moi` n'est exercée que contre un client simulé — le contrat est identique des deux côtés : `PointEntreeProfil.Profil(identifiant, nomComplet, roles)` sérialise `nomComplet`, que `ClientProfilHttp` lit ; sur la pile, `GET /api/identite-clients/moi` sans jeton répond 401 `{"message":"Authentification requise"}`, traduit en `nonAuthentifie`. Connexion réelle impossible faute de mots de passe `RETRO_*` dans `.env` (risque résiduel déjà noté en 2.3).
  - `[low]` `[reject]` (Intent Alignment) 72px, 48px, soulignement 2px et passage sur deux lignes sous 1 280 px non testés (jsdom ne calcule aucune mise en page) — valeurs portées par les CSS en tokens et constatées dans un navigateur à 1 200 px par l'implémentation (en-tête ADMIN sur deux lignes, sous-en-tête, badge) ; un test visuel ajouterait un outillage nouveau pour un cas déjà constaté.
  - `[low]` `[reject]` (Intent Alignment) UX-DR110 : trois motifs (deux niveaux de navigation, tooltip interactif, plus d'une modale) ne sont contrôlés que par le README — le critère demande une relecture : relue, aucun tooltip ni modale dans `src/adapters`, et `sousNavigation` borne la navigation à deux niveaux ; le test automatique couvre en plus les trois motifs détectables par le source.
  - `[low]` `[reject]` (Intent Alignment) Les branches négatives de `verifier-scenario.sh` n'ont pas de test automatique — exécutées à la main (SC-003-01, SC-001-03, SC-999-99, `toto`, `nav-deconnexion` retiré puis restauré : codes non nuls nommant l'élément) ; un banc de test shell serait de la complexité nouvelle pour un script de vérification.
  - `[low]` `[reject]` (Intent Alignment) Aucun test nouveau ne cible `/virement`, désormais lien d'en-tête — même chemin de code que `/route-inconnue` (route `*` → `RedirectionAccueil`), couvert par `routes.test.tsx` pour les trois rôles ; `verifier-scenario.sh SC-003-01` confirme que `/virement` n'est pas déclarée.
  - `[false]` `[reject]` (Intent Alignment) Les contrôles de fin ne sont pas prouvés par le diff — exécutés : `build.sh` 0, `check-gates-exist.sh` 0, `golden-master.sh` 0, `trace-smoke.sh` 0 (pile recréée).
  - `[false]` `[reject]` (Intent Alignment) Filtrage par rôle seulement côté client — la vérification par chaque service est la story 2.6 (dépendances de l'epic 2) ; la story 2.4 ne demande que le comportement de la SPA.

### 2026-10-06 — Review pass
- verdicts: 11 findings — high 0, medium 0, low 6, false 5, maybe-false 0
- findings:
  - `[false]` `[reject]` (Intent Alignment) La chaîne réelle `/moi` → « Bonjour, Jean Dupont » n'est exercée que contre un client simulé — carried : contrat `PointEntreeProfil.Profil` identique à ce que lit `ClientProfilHttp` ; connexion réelle impossible faute de mots de passe `RETRO_*`.
  - `[low]` `[reject]` (Intent Alignment) 72px, 48px et passage sur deux lignes sous 1 280 px non mesurés — carried : valeurs portées par les CSS en tokens, constatées dans un navigateur ; un test visuel ajouterait un outillage nouveau.
  - `[false]` `[reject]` (Intent Alignment) « JSON sans `nomComplet` » n'est couvert qu'au niveau de l'adaptateur — `ClientProfilHttp` traduit ce corps en `technique`, et les tests du gabarit vérifient que `technique` lève `aria-busy` sans salutation : aucune salutation n'apparaît par composition.
  - `[low]` `[reject]` (Intent Alignment) Les titres d'onglet « BankApp Demo - … » sont des textes hors de la liste « Always » — titres du legacy (FEAT-002, FEAT-015, FEAT-016), hissés dans `<head>`, absents de la page, exigés par `axe` (`document-title`) ; le lien d'évitement vient de la story 2.3.
  - `[low]` `[reject]` (Intent Alignment) Aucun clic depuis l'en-tête vers une route non déclarée (`/virement`) n'est testé — carried : même chemin que `/route-inconnue` (route `*` → `RedirectionAccueil`), couvert par `routes.test.tsx` pour les trois rôles.
  - `[low]` `[reject]` (Intent Alignment) La route `*` est hors de `RouteProtegee` : suivre un lien vers une route non déclarée démonte le gabarit et rappelle `/moi` — vérifié dans `frontend/src/adapters/routes/routes.tsx:51-52` ; effet réduit à un appel réseau et à un bref `aria-busy`, qui disparaît quand chaque écran déclare sa route (epics suivants) ; la correction demanderait de restructurer le routeur.
  - `[low]` `[reject]` (Intent Alignment) Le script vérifie la présence de littéraux dans le source, pas qu'un écran les rende — lecture retenue par la spec (vérification statique) ; `TITRES_ONGLET` n'a pour clés que des routes déclarées, donc pas de faux positif aujourd'hui ; une analyse par écran serait une complexité nouvelle.
  - `[low]` `[reject]` (Intent Alignment) Les cas d'échec du script n'ont pas de test automatique — carried : exécutés à la main, codes non nuls nommant l'élément (rejoués dans cette passe : `SC-003-01`, `SC-001-03`, `SC-999-99`).
  - `[false]` `[reject]` (Intent Alignment) `/login` n'est pas un littéral des fichiers du routeur — aucun scénario dérivé n'a `start_url` `/login` (décompte des `start_url` sous `docs/retro/golden-master/derived/`) : aucune vérification n'échoue à tort.
  - `[false]` `[reject]` (Intent Alignment) Couverture à 100 % et contrôles de fin non prouvés par le diff — carried : `build.sh` 0 (seuils de `vite.config.ts` et JaCoCo appliqués), `check-gates-exist.sh` 0, `golden-master.sh` 0, `trace-smoke.sh` 0 dans cette passe.
  - `[false]` `[reject]` (Intent Alignment) Les modifications de `Conteneurs.java` (quatre services) sortent du périmètre de l'intention — correctif de la vérification en échec (Kafka de test qui ne démarrait pas, `ContainerLaunchException`) : code de test uniquement, aucun chemin interdit, aucun contrôle ni seuil touché.

## Design Notes

- **`main` hors de l'`Application`** : l'en-tête (`<header>`, landmark `banner`) doit précéder `main` et suivre le lien d'évitement. `Application` devient « lien d'évitement + `Outlet` » ; chaque branche (connexion, gabarit authentifié) rend `ContenuPrincipal`. Le lien focalise `document.getElementById(id)` : toujours un focus par script, comme en 2.3.
- **Rôles du jeton, nom de `/moi`** : la garde doit décider avant tout rendu, sans attendre le réseau ; le service revérifie (story 2.6). `/moi` sert seulement la salutation et l'état `aria-busy`.
- **401 de `/moi`** : traité localement (purge + `/login`), conforme à AR-34 ; la généralisation à tout service reste à la story 2.5.
- **« Bonjour, Jean Dupont »** est composé à l'exécution : le script ne le trouvera pas tel quel et le listera dans le rapport, sans échec (attendu).
- **Script** : `start_url` cherchée en littéral `'…'` ou `"…"` dans les seuls fichiers du routeur, car `navigation.ts` contient `/virement` (lien d'en-tête) sans que la route soit déclarée.

## Verification

**Commands:**
- `cd frontend && npx vitest run -t 'FR-001-05|FR-001-11|FR-016-01|FR-030-02|FR-030-03|FR-030-04'`, `-t 'axe'`, `-t 'UX-DR110'` -- expected: code 0.
- `scripts/golden-master/verifier-scenario.sh SC-001-04; echo $?` et `SC-001-05` -- expected: 0 ; puis les cas négatifs (`SC-003-01`, `SC-001-03`, `SC-999-99`, `nav-deconnexion` retiré puis restauré) -- expected: non nul avec le nom attendu.
- `git status --porcelain docs/retro/golden-master/derived e2e/golden-master replay-mapping.json` -- expected: vide.
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` -- expected: code 0 ; `docker compose -f compose.yaml up -d --build --wait && scripts/gates/trace-smoke.sh` -- expected: code 0.

**Manual checks (if no CLI):**
- Sur la pile, avec un jeton CLIENT puis ADMIN (Playwright MCP) : en-tête, badge, sous-en-tête, redirection de `/administration` pour un CLIENT, liens sur deux lignes sous 1 280 px.

## Auto Run Result

Status: done

**Résumé :** reprise après l'échec de vérification de l'orchestrateur. La story 2.4 était déjà implémentée et committée (`5308d0283998199dd18055dd4796761abc6d3cd7`) : gabarit authentifié, garde par rôle, salutation par `/moi`, README des motifs interdits, `scripts/golden-master/verifier-scenario.sh`. L'échec de `scripts/gates/build.sh` ne venait pas du code de la story : dans `identite-clients`, le conteneur Testcontainers `apache/kafka:4.2.2` n'a pas démarré (`ContainerLaunchException`), alors que la pile compose tournait sur le même poste. Tous les tests d'intégration du module sont tombés en cascade (`NoClassDefFoundError: Conteneurs`). Sans aucun changement, le même build repasse à 0. Pour éviter que l'incident se répète, le Kafka de test accepte désormais trois tentatives de démarrage.

**Fichiers (cette passe) :**
- `services/{audit,coeur-bancaire,identite-clients,notifications}/src/test/java/fr/nextbanking/*/infrastructure/Conteneurs.java` — `KafkaContainer` avec `withStartupAttempts(3)`.
- Fichiers de la story : voir le commit `5308d0283998199dd18055dd4796761abc6d3cd7` (domaine `navigation.ts`, application `chargerProfil.ts`, adaptateurs `gabarit/`, `ui/`, `routes/`, `http/ClientProfilHttp.ts`, `frontend/README.md`, `motifs-interdits.test.ts`, `scripts/golden-master/verifier-scenario.sh`, tests).

**Revue :** 11 constats (Intent Alignment). Corrigés : aucun. Reportés : aucun. Rejetés : 11.
- 5 réfutés (`false`) : contrat `/moi` identique ; JSON sans `nomComplet` couvert par composition ; aucun scénario sur `/login` ; contrôles de fin exécutés ; `Conteneurs.java` est le correctif de la vérification.
- 6 `low` jugés sans intérêt à corriger : mise en page non mesurable en jsdom ; titres d'onglet repris du legacy et exigés par `axe` ; clic vers `/virement` couvert par le même chemin de code ; nouvel appel à `/moi` après une route non déclarée, qui disparaît quand les écrans arrivent ; vérification par littéraux, lecture retenue par la spec ; cas d'échec du script vérifiés à la main.

**Relecture complémentaire recommandée :** non. Aucun correctif issu de la revue (high 0, medium 0, low 0 corrigés).

**Vérification :**
- `scripts/gates/build.sh` 0, deux fois (avant et après le correctif ; 233 tests front, couverture tenue) ; `scripts/gates/check-gates-exist.sh` 0 ; `scripts/gates/golden-master.sh` 0 ; `scripts/gates/trace-smoke.sh` 0 (pile déjà démarrée : spans JDBC, émission et consommation Kafka).
- `npx vitest run -t 'FR-001-05|FR-001-11|FR-016-01|FR-030-02|FR-030-03|FR-030-04'` : 0 (60 tests) ; `-t 'axe'` : 0 (6) ; `-t 'UX-DR110'` : 0 (35).
- `verifier-scenario.sh SC-001-04` et `SC-001-05` : 0 ; `SC-003-01`, `SC-001-03` et `SC-999-99` : KO, avec le scénario et l'élément nommés.
- `git status --porcelain docs/retro/golden-master/derived e2e/golden-master replay-mapping.json` : vide.

**Risques résiduels :**
- La cause première du démarrage raté de Kafka est inconnue : le journal de l'orchestrateur est tronqué à 256 Ko. Hypothèse : machine chargée (pile compose et Testcontainers en même temps, 15 Go de mémoire). Les tentatives répétées réduisent le risque sans le supprimer.
- Risques de la première passe toujours valables : salutation jamais vue avec un vrai compte (mots de passe `RETRO_*` absents) ; `nav-deconnexion` sans effet avant la story 2.5.
- `sprint-status.yaml` est modifié par l'orchestrateur : non committé, non touché.
