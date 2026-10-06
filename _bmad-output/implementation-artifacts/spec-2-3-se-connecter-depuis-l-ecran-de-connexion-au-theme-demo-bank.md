---
title: "Se connecter depuis l'écran de connexion au thème Demo Bank"
type: 'feature'
created: '2026-10-06'
baseline_revision: 'ea67c601a91ff9225895f2bd83c50ed6960d6775'
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

**Problem:** Le front n'a ni routeur, ni thème, ni écran : aucun utilisateur ne peut se connecter depuis un navigateur. L'API `POST /api/identite-clients/connexion` (story 2.1) existe mais rien ne la consomme.

**Approach:** Poser le thème généré depuis DESIGN.md (`npm run tokens` → `tokens.css`), la police Inter auto-hébergée, des composants UI accessibles, le routeur `react-router` 8.4.0 avec `/login` et trois routes protégées, l'écran `/login` (validation locale, appel de l'API, jeton en `localStorage` `nb.jeton`, arrivée sur l'accueil du rôle), un lien d'évitement sur toute page, une CSP nginx, et des tests Vitest tagués (FR, `[UX-DR1]`, `axe`).

## Boundaries & Constraints

**Always:**
- Textes exacts : onglet « BankApp Demo - Connexion » ; « DB » ; « Demo Bank » ; « Espace client — Connexion sécurisée » (U+2014) ; « Identifiant » ; « Mot de passe » ; « Me connecter » ; « Identifiant requis » ; « Mot de passe requis » ; « Identifiants incorrects » ; « Vérifiez votre login et mot de passe » ; « Aller au contenu principal » ; erreur technique « Une erreur technique est survenue » (`api.erreur.technique`). Aucun autre texte visible.
- `data-testid` : `login-identifiant`, `login-mot-de-passe`, `login-me-connecter`, `login-message-erreur`, `application-lien-evitement` ; `application-racine` (existant, figé) reste sur l'élément de contenu principal.
- Hexagone : `src/domain` (règles : champs requis, accueil du rôle) et `src/application` (cas d'usage `seConnecter` et ses ports) en TypeScript pur, 100 % de couverture ; React, routeur, `fetch`, `localStorage`, décodage du JWT dans `src/adapters` ; assemblage dans `src/infrastructure`.
- Couleurs, tailles, polices uniquement par les variables de `tokens.css` ; éléments natifs stylés seulement dans `src/adapters/ui/` ; aucune ombre, angles vifs, thème clair.
- Nouvelles dépendances npm consignées dans `versions-verifiees.md` (ligne datée 2026-10-06, URL du registre, story 2.3) avant usage, versions exactes sans `^`.
- Tests DOM en `// @vitest-environment jsdom` par fichier ; l'environnement global reste `node`.

**Never:**
- Ne pas modifier `scripts/gates/**`, `frontend/.dependency-cruiser.cjs`, `docs/quality-gates.md`, ni les seuils de `vite.config.ts` ; pas de `vitest.config.*` ; pas de Tailwind ; pas de Google Fonts ni CDN.
- Pas de contenu pour `/tableau-de-bord`, `/espace-conseiller`, `/administration` (stories suivantes), ni d'en-tête de navigation, de `/moi`, de déconnexion, de renouvellement ou d'intercepteur 401 global (stories 2.4, 2.5).
- Jamais `sessionStorage`, jamais de jeton ni mot de passe dans la console ; jamais de détail technique affiché.
- Ne jamais rejouer le golden master ; ne rien toucher sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Champs vides | Identifiant `""` ou blanc, mot de passe `""`, Entrée | « Identifiant requis » et « Mot de passe requis » sous chaque champ vide, `aria-invalid="true"`, `aria-describedby` ; focus sur le premier champ en erreur | Aucun appel API |
| Un seul vide | Identifiant `user1`, mot de passe `""` | Seul « Mot de passe requis », focus sur le mot de passe | Aucun appel API |
| Refus | API 401 | Reste sur `/login` ; `login-message-erreur` (`role="alert"`) affiche les deux lignes ; valeurs conservées ; focus sur le message | Pas de jeton écrit |
| Panne | API 5xx, 400 inattendu, réseau coupé, JSON illisible | `login-message-erreur` : « Une erreur technique est survenue » | Aucun détail technique |
| Envoi en cours | Réponse en attente, second Entrée/clic | Bouton `disabled`, `aria-busy="true"` ; une seule requête | — |
| Succès | 200 `{"jeton"}`, revendication `roles` = `ADMIN` / `CONSEILLER` / `CLIENT` | `localStorage["nb.jeton"]` = jeton ; navigation (remplacement) vers `/administration` / `/espace-conseiller` / `/tableau-de-bord` | Plusieurs rôles : ADMIN > CONSEILLER > CLIENT ; rôle inconnu ou jeton illisible : erreur technique, rien écrit |
| Sans jeton | `/`, `/tableau-de-bord`, `/espace-conseiller`, `/administration`, route inconnue | `/login` (remplacement) | — |
| Avec jeton | `/` ou route inconnue | Accueil du rôle lu dans le jeton stocké ; jeton illisible → supprimé, `/login` | — |

</intent-contract>

## Code Map

- `frontend/src/adapters/Application.tsx`, `Application.test.tsx` -- racine actuelle (`<main data-testid="application-racine">`) : devient la mise en page commune (lien d'évitement + `<main id="contenu-principal" tabIndex={-1} data-testid="application-racine">` + routes).
- `frontend/src/infrastructure/main.tsx`, `main.test.ts` -- point d'entrée ; y assembler les adaptateurs (client HTTP, stockage) et importer les CSS du thème ; garder le test à jour.
- `frontend/src/application/lireMontant.ts`, `frontend/src/domain/montant.ts` -- motif port + cas d'usage pur à suivre.
- `frontend/src/architecture-temps.test.ts` -- motif de test qui lit les sources par `process.getBuiltinModule('node:fs')` (à reprendre pour `[UX-DR1]`) ; ne pas lire `Date.now()` dans domain/application.
- `frontend/vite.config.ts` -- environnement `node`, seuils par couche (ne pas toucher aux seuils) ; `frontend/tsconfig.json` (`include: src`, `types: vite/client`).
- `frontend/index.html` -- `lang="fr"` déjà ; titre inventé « next-banking-webapp » à remplacer (deferred-work l.10-12).
- `frontend/nginx.conf` -- aucun en-tête de sécurité ; ajouter la CSP au niveau `server` (`add_header … always`).
- `frontend/package.json` -- versions exactes ; `build.sh` lance `npm ci`, `depcruise src`, `tsc --noEmit`, `vitest run --coverage` : régénérer `package-lock.json`.
- `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/DESIGN.md` -- source des tokens (frontmatter l.1-510 : `colors` l.10, `typography` l.36, `rounded`, `spacing`, `components` : `button-primary` l.137, `button-disabled` l.181, `input` l.192, `input-error` l.204, `card` l.243, `alert-error` l.350, `focus-ring` l.399). Porte encore `Graphik, Inter, Arial, sans-serif` (l.38-86) et « Graphik, repli Inter » (l.526) malgré D11 (`epics.md:1913`, `decisions-a-trancher.md:83`).
- `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md` -- format de ligne `composant | version | source | vérifié le | remarque` ; `@google/design.md` 0.4.0 et `react-router` 8.4.0 déjà consignés.
- `services/identite-clients/src/main/java/fr/nextbanking/identiteclients/adapter/in/http/PointEntreeConnexion.java:46,55` -- requête `{"identifiant","motDePasse"}`, réponse `{"jeton"}` seule ; rôles dans la revendication `roles` du JWT (`adapter/out/jeton/EmetteurJetonNimbus.java:63`) ; `GestionErreurs.java:27-29` (400 requis, 401 « Identifiants incorrects ») ; `domain/Identifiants.java` (identifiant blanc refusé, mot de passe seulement vide).
- `docs/retro/features/FEAT-001/{MESSAGES,FIELDS}.md`, `screens/SCR-001-01_vide.png` -- textes et mise en page legacy (carte blanche centrée ~460px, « DB » en disque, libellés au-dessus des champs, bouton pleine largeur).
- `e2e/golden-master/specs/FEAT-001.spec.ts:17-20,47-61` (lecture seule) -- SC-001-01 : `GET /login` → 200 ; SC-001-05 : le texte de `/login` contient « Identifiant », « Mot de passe », « Espace client ».

## Tasks & Acceptance

**Execution:**
- `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/DESIGN.md` -- appliquer D11 : `Graphik, Inter, Arial, sans-serif` → `Inter, Arial, sans-serif` partout, prose « Graphik, repli Inter » → Inter seule auto-hébergée (D11) -- sinon `tokens.css` exporterait une police écartée.
- `_bmad-output/planning-artifacts/architecture/.../versions-verifiees.md` -- consigner `axe-core` 4.14.0, `jsdom` 30.1.2, `@testing-library/react` 16.3.3, `@testing-library/dom` 10.4.2, `@testing-library/user-event` 14.6.7, `@fontsource/inter` 5.3.0 (vérifiés au registre ; engines et peers notés) -- AR-101 et règle des versions.
- `frontend/package.json`, `package-lock.json` -- dépendance `react-router` 8.4.0, `@fontsource/inter` 5.3.0 ; dev : `@google/design.md` 0.4.0 et les paquets de test ci-dessus ; script `tokens` = `design.md export --format css-tailwind <DESIGN.md> | node scripts/tokens-root.mjs > src/adapters/ui/theme/tokens.css` (forme exacte selon la CLI) -- génération reproductible sans réseau.
- `frontend/scripts/tokens-root.mjs` -- convertit le bloc `@theme { … }` en `:root { … }`, sortie déterministe, échoue si aucun bloc trouvé.
- `frontend/src/adapters/ui/theme/tokens.css` -- généré, versionné, jamais retouché ; `frontend/src/adapters/ui/theme/base.css` -- imports Inter (`@fontsource/inter` 400/600/700), `body` (police, couleurs `ink`/`surface`), `:focus-visible` = focus-ring, `prefers-reduced-motion`.
- `frontend/src/adapters/ui/` -- composants : `LienEvitement`, `ChampTexte` (label for, `aria-required`, astérisque en CSS `::after`, erreur sous le champ, `aria-invalid`, `aria-describedby`), `BoutonPrimaire` (48px, `aria-busy`, désactivé), `AlerteErreur` (`role="alert"`, `tabIndex=-1`), `Carte` ; CSS par composant, uniquement `var(--…)` -- styles portés par les composants (UX-DR81).
- `frontend/src/domain/connexion.ts` -- `validerIdentifiants` (identifiant blanc, mot de passe vide) et `accueilDuRole(roles)` ; `frontend/src/application/seConnecter.ts` -- ports `ClientConnexion`, `StockageJeton` ; résultat `champs` / `refus` / `technique` / `accepte(route)` ; tests à 100 %.
- `frontend/src/adapters/http/ClientConnexionHttp.ts` -- `fetch` POST JSON, 200 → jeton + rôles décodés du JWT (base64url), 401 → refus, reste → technique ; `frontend/src/adapters/stockage/StockageJetonLocal.ts` -- `localStorage` `nb.jeton`.
- `frontend/src/adapters/ecrans/connexion/EcranConnexion.tsx` (+ CSS) -- `<form noValidate>`, carte centrée, `<title>` React 19, focus, anti double soumission, `useNavigate(…, { replace: true })`.
- `frontend/src/adapters/routes/` -- routeur : `/login` public ; `/tableau-de-bord`, `/espace-conseiller`, `/administration` protégées (sans jeton → `/login`) ; `/` et `*` → accueil du rôle ou `/login`.
- `frontend/src/adapters/Application.tsx`, `frontend/src/infrastructure/main.tsx`, `frontend/index.html` -- mise en page commune, `RouterProvider`, titre « BankApp Demo - Connexion ».
- `frontend/nginx.conf` -- `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`.
- Tests (`*.test.ts(x)`) -- domain/application (matrice) ; écran en jsdom avec user-event (Tab, Entrée, double appui, 401, 5xx, trois rôles, `nb.jeton` et `sessionStorage` vide) tagués `[FR-001-0x]`, `[FR-030-0x]` ; routes sans jeton ; `[UX-DR1]` couleurs hors tokens dans `frontend/src` (+ cas négatif sur `#ff0000` nommant le fichier) ; `axe` sur `/login` (zéro violation, + cas témoin : formulaire sans `<label for>` → règle `label`) ; nginx (en-tête CSP présent et sans `unsafe-inline`).

**Acceptance Criteria:**
- Given DESIGN.md, when `cd frontend && npm run tokens && git diff --exit-code src/adapters/ui/theme/tokens.css`, then code 0 ; `! grep -n '@theme' frontend/src/adapters/ui/theme/tokens.css`, `! grep -rnE 'fonts\.(googleapis|gstatic)\.com' frontend/src frontend/index.html`, `! grep -rnE '^\s*(select|table|input|button|a|th|td)\s*[,{]' frontend/src --include=*.css | grep -v 'src/adapters/ui/'` en code 0 ; aucune dépendance Tailwind.
- Given `#ff0000` ajouté au CSS d'un écran, when `npx vitest run -t 'UX-DR1'`, then échec nommant le fichier.
- Given `/login` sans jeton, then onglet, textes, `<label for>`, formulaire (`name="username"` premier champ texte, seul `type="password"`, bouton `type="submit"`) conformes ; « Système Legacy » absent ; `lang="fr"`.
- Given toute page, when premier `Tab`, then le lien « Aller au contenu principal » a le focus, est visible au focus, et son activation place le focus sur le contenu principal.
- Given la story terminée, when `cd frontend && npx vitest run -t 'FR-001-01|FR-001-02|FR-001-03|FR-001-04|FR-001-05|FR-001-06|FR-030-01|FR-030-03'` puis `npx vitest run -t 'axe'`, then code 0 ; retirer un `<label for>` fait échouer `-t 'axe'` en nommant `label`.
- Given la pile compose, when `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/login`, then `200` ; `curl -sI http://localhost:8080/login` porte la CSP ; les polices Inter sont servies par le conteneur web (`/assets/*.woff2` en 200).
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 27 findings — high 0, medium 4, low 18, false 5, maybe-false 0
- findings:
  - `[low]` `[reject]` (Blind) Les routes protégées ne contrôlent pas le rôle (un CLIENT ouvre `/administration`) — vrai, mais ces routes ne rendent aucun contenu ici ; le renvoi d'un rôle insuffisant vers `/tableau-de-bord` est livré par la story 2.4 (epic-2-context, UX de 2.4) ; garde supplémentaire sans dommage actuel.
  - `[low]` `[patch]` (Blind) Une route protégée accepte un jeton illisible et ne le supprime pas, contrairement à `/` et `*` — corrigé : `RouteProtegee` utilise `accueilDeLaSession` (supprime le jeton, renvoie à `/login`) ; test jeton illisible et rôle inconnu sur `/tableau-de-bord`.
  - `[low]` `[reject]` (Blind) L'expiration `exp` n'est pas contrôlée — l'expiration de session et le retour à `/login` sur 401 sont la story 2.5 ; ici un jeton expiré n'ouvre qu'une page vide.
  - `[low]` `[reject]` (Blind) Un utilisateur connecté peut rouvrir `/login` — non spécifié, sans dommage (une reconnexion remplace le jeton) ; ajouter une garde serait une règle inventée.
  - `[low]` `[reject]` (Blind) Aucun délai sur l'appel de connexion — via le conteneur web, nginx borne la requête (`proxy_read_timeout` 60 s par défaut) puis rend 504, traduit en erreur technique ; ajouter un minuteur ajoute de la logique pour un cas rare.
  - `[false]` `[reject]` (Blind) Identifiant validé rogné mais envoyé brut — ni le legacy (FEAT-001) ni le service (`Identifiants.java`, aucun `trim`) ne rognent : l'envoi brut respecte le contrat ; rogner serait un écart non décidé.
  - `[low]` `[reject]` (Blind) Titre statique « Connexion » sur toute page — les routes protégées n'ont pas encore d'écran ; un titre neutre serait un texte inventé ; chaque écran posera son `<title>` (stories suivantes).
  - `[false]` `[reject]` (Blind) Contraste affirmé sans test — calcul fait sur les tokens : erreur sur conteneur d'erreur ≈ 5,4:1, primary-hover sur surface ≈ 8:1, disabled ≈ 5:1, text-secondary sur surface-alt ≈ 5,4:1, tous ≥ 4,5:1.
  - `[medium]` `[patch]` (Blind) `tokens.css` sans contrôle de dérive vis-à-vis de DESIGN.md et `tokens-root.mjs` non testé — corrigé : `tokens.test.ts` lance le script sur des entrées témoins (pile entre guillemets, `--font-weight-*`, bloc absent ou vide → code 1) et régénère depuis DESIGN.md par la CLI installée en comparant octet par octet au fichier versionné.
  - `[low]` `[patch]` (Blind) Le test nginx ne protège pas l'héritage de la CSP — corrigé : assertion qu'aucun bloc `location` ne contient `add_header`.
  - `[low]` `[reject]` (Blind) Outils de test (`rendreApplication.tsx`, `jetonDeTest.ts`) dans l'arbre de production — jamais importés par le code de production (absents du bundle), couverture des adaptateurs au-dessus du seuil ; une exclusion de couverture est interdite et `.dependency-cruiser.cjs` est protégé.
  - `[low]` `[reject]` (Blind) Message de refus peut-être annoncé deux fois (`role="alert"` et focus) — les deux sont exigés (UX-DR44 `role="alert"`, UX-DR86 focus sur le message global).
  - `[low]` `[reject]` (Blind) Les erreurs de champ restent jusqu'à la soumission suivante — comportement non spécifié, conforme au legacy (validation à la soumission) ; aucun dommage.
  - `[false]` `[reject]` (Blind) `package-lock.json` et la spec absents du diff ; `img-src data:` non justifié — le verrou est exclu volontairement du diff de revue (fichier généré, versionné au commit) ; `img-src 'self' data:` n'ouvre aucune origine externe et ne touche ni scripts, ni styles, ni polices.
  - `[low]` `[patch]` (Edge) `RouteProtegee` affiche la route pour tout jeton non nul — même cause que la ligne Blind correspondante, même correctif.
  - `[low]` `[reject]` (Edge) `localStorage` qui lève une `SecurityError` — cas rare (stockage bloqué par le navigateur) ; la garde ajoute des branches à chaque accès.
  - `[low]` `[reject]` (Edge) Appel de connexion sans délai — même raison que la ligne Blind (borne nginx, 504 → erreur technique).
  - `[false]` `[reject]` (Edge) Identifiant avec espaces envoyé brut — même réfutation que la ligne Blind.
  - `[low]` `[reject]` (Edge) `/login` affiché à un utilisateur connecté — même raison que la ligne Blind.
  - `[low]` `[reject]` (Edge) Titre de l'onglet sur les autres pages — même raison que la ligne Blind.
  - `[low]` `[reject]` (Edge) Navigation tardive si l'écran est démonté pendant l'envoi — le jeton est valide et enregistré, l'arrivée sur l'accueil du rôle est le comportement attendu d'une connexion réussie ; cas rare.
  - `[medium]` `[patch]` (Edge) `tokens-root.mjs` coupe le bloc au premier `}` — groupé avec le contrôle de dérive : toute sortie tronquée diffère désormais du fichier versionné et fait échouer `tokens.test.ts`.
  - `[medium]` `[patch]` (Edge) Déclarations sur une même ligne ou sur plusieurs lignes mal traitées — même groupe, même correctif (dérive détectée par le test).
  - `[medium]` `[patch]` (Verification Gap) Conversion des tokens et `tokens.css` sans contrôle automatique — même groupe, même correctif ; l'ajout de `npm run tokens` à `build.sh` reste à l'humain (`scripts/gates/**` protégé), le test Vitest lancé par `build.sh` couvre la dérive.
  - `[low]` `[patch]` (Verification Gap) La CSP peut disparaître sur un bloc `location` — même cause et même correctif que la ligne Blind.
  - `[low]` `[patch]` (Verification Gap) Câblage réel de `main.tsx` jamais vérifié — corrigé : `main.test.ts` lit les dépendances passées aux routes et vérifie l'écriture de `nb.jeton`, le POST vers `/api/identite-clients/connexion` (401 → refus) et la lecture des rôles du JWT.
  - `[false]` `[reject]` (Intent Alignment) Contrôles sur la pile et critères négatifs sur les vrais fichiers non démontrés ; lecture « awaiting-operator » possible — tous ont été exécutés par l'agent (pile reconstruite, `curl` 200 et CSP, `woff2` 200, `trace-smoke.sh` 0, `#ff0000` dans `EcranConnexion.css` et `htmlFor` retiré de `ChampTexte.tsx` font échouer les filtres, puis restaurés) ; aucune action réservée à un humain.

## Design Notes

- **D11 appliqué à DESIGN.md** : décision humaine déjà tranchée (Inter seule) mais non reportée dans DESIGN.md ; corriger la source plutôt que filtrer dans `tokens-root.mjs` (qui ne doit que renommer le bloc). La mention « Inter en repli de Graphik » du critère d'epic est périmée par D11.
- **Pile de polices dans `tokens-root.mjs`** (constaté à l'implémentation) : `@google/design.md` 0.4.0 exporte `--font-*: "Inter, Arial, sans-serif";`, une seule famille inexistante pour le navigateur. Le script, en plus de renommer le bloc, rend cette valeur en liste CSS ; aucune autre valeur n'est touchée, et DESIGN.md ne peut pas l'éviter.
- **Rôles** : la réponse de connexion ne porte que `{"jeton"}` ; l'adaptateur HTTP lit la revendication `roles` du JWT (segment 2, base64url, JSON) sans vérifier la signature (le service la vérifie à chaque appel). Le domaine ne connaît que la règle rôle → route.
- **Astérisque** : `aria-required="true"` + `::after { content: " *" }` : l'astérisque reste visuel, hors `innerText` et hors nom accessible, donc les libellés restent exactement « Identifiant » / « Mot de passe ».
- **Lien d'évitement** : `onClick` avec `preventDefault()` puis `focus()` sur `<main tabIndex={-1}>` (le focus par ancre n'est pas fiable, ni dans jsdom ni avec le routeur).
- **CSP** : Vite produit `<script type="module" src>` et `<link rel="stylesheet">`, sans code en ligne ; React pose les styles par CSSOM. Ne pas utiliser d'attribut `style` dans le JSX.
- **axe en jsdom** : `color-contrast` est classé « incomplete » (pas de rendu) ; le contraste est garanti par les tokens.

## Verification

**Commands:**
- `cd frontend && npm run tokens && git diff --exit-code src/adapters/ui/theme/tokens.css` -- expected: code 0.
- Les trois `! grep` de l'AC -- expected: code 0.
- `cd frontend && npx vitest run -t 'FR-001-01|FR-001-02|FR-001-03|FR-001-04|FR-001-05|FR-001-06|FR-030-01|FR-030-03'`, `npx vitest run -t 'axe'`, `npx vitest run -t 'UX-DR1'` -- expected: code 0.
- Critères négatifs à la main puis annulés : `#ff0000` dans un CSS d'écran → `-t 'UX-DR1'` échoue en nommant le fichier ; `<label for>` retiré → `-t 'axe'` échoue en nommant `label`.
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` -- expected: code 0.
- `make secrets && docker compose -f compose.yaml up -d --build --wait && scripts/gates/trace-smoke.sh` -- expected: code 0 ; puis les deux `curl` de l'AC (200, CSP) et un `curl` d'un `/assets/*.woff2` → 200.

**Manual checks (if no CLI):**
- Sur la pile (données de référence chargées à la main), connexion de `user1` par Entrée dans un navigateur (Playwright MCP) : arrivée sur `/tableau-de-bord`, `nb.jeton` présent, aucune erreur CSP en console.

## Auto Run Result

Status: done

**Résumé :** l'écran `/login` est livré au thème Demo Bank. `npm run tokens` génère `tokens.css` depuis DESIGN.md (export `css-tailwind` de `@google/design.md` 0.4.0, puis `tokens-root.mjs`), D11 est appliqué à DESIGN.md (Inter seule), et Inter est auto-hébergée par `@fontsource/inter`. L'écran repose sur des composants UI accessibles et sur `react-router` 8.4.0 : `/login` est public ; `/tableau-de-bord`, `/espace-conseiller` et `/administration` sont protégées ; `/` et toute route inconnue mènent à l'accueil du rôle ou à `/login`. Comportement de l'écran : validation locale (« Identifiant requis » / « Mot de passe requis » sous le champ, focus sur le premier champ en erreur) ; refus « Identifiants incorrects » / « Vérifiez votre login et mot de passe » ; panne « Une erreur technique est survenue » ; anti double soumission ; jeton dans `localStorage["nb.jeton"]` ; arrivée sur l'accueil du rôle lu dans le JWT. S'y ajoutent le lien d'évitement sur toute page et une CSP nginx limitant scripts, styles et polices à l'origine.

**Fichiers :**
- `_bmad-output/planning-artifacts/ux-designs/.../DESIGN.md` — D11 : pile `Inter, Arial, sans-serif`, plus de Graphik.
- `_bmad-output/planning-artifacts/architecture/.../versions-verifiees.md` — 6 lignes datées : axe-core, jsdom, @testing-library/react, dom, user-event, @fontsource/inter.
- `frontend/package.json`, `package-lock.json` — dépendances aux versions exactes, script `tokens`.
- `frontend/scripts/tokens-root.mjs` — `@theme` → `:root`, pile de polices remise en liste CSS.
- `frontend/src/adapters/ui/**` — `tokens.css` (généré), `base.css`, `LienEvitement`, `ChampTexte`, `BoutonPrimaire`, `AlerteErreur`, `Carte`, `tokens.test.ts`.
- `frontend/src/domain/connexion.ts`, `frontend/src/application/seConnecter.ts` — règles (champs requis, accueil du rôle) et cas d'usage, 100 % couverts.
- `frontend/src/adapters/{http,jeton,stockage,routes,ecrans/connexion}/**`, `textes.ts`, `Application.tsx` — client HTTP, décodeur JWT, stockage, routeur, écran, mise en page commune.
- `frontend/src/infrastructure/main.tsx`, `index.html`, `nginx.conf` — assemblage, titre « BankApp Demo - Connexion », CSP.
- Tests : `EcranConnexion.test.tsx`, `routes.test.tsx`, `Application.test.tsx`, `accessibilite.test.tsx` (axe), `couleurs-hors-tokens.test.ts` (`[UX-DR1]`), `nginx.test.ts`, `main.test.ts`, tests du domaine et de l'application.

**Revue :** 27 constats (Blind Hunter 14, Edge Case Hunter 9, Verification Gap 3, Intent Alignment 1). Corrigés : 4 entrées. La dérive des tokens (medium) est désormais contrôlée par un test Vitest. Les trois autres sont low : la route protégée purge un jeton illisible ; le test nginx vérifie l'absence d'`add_header` dans les `location` ; `main.test.ts` vérifie le câblage réel. Reportés : aucun. Rejetés : 18 constats, chacun motivé dans le journal de tri. Ce sont 5 réfutations (`false`), plus des points low qui relèvent des stories 2.4 et 2.5 (rôle, expiration) ou sont sans dommage.

**Relecture complémentaire recommandée :** non. Premier passage, aucune entrée `high` corrigée et une seule `medium` (corrigées par verdict : high 0, medium 1, low 3).

**Vérification :**
- `npm run tokens` régénère un `tokens.css` identique (`cmp`). Les trois `! grep` sont à 0 et aucune dépendance Tailwind n'est présente.
- Filtres Vitest : `-t 'FR-001-01|…|FR-030-03'` 0 (33 tests) ; `-t 'axe'` 0 ; `-t 'UX-DR1'` 0.
- Critères négatifs faits à la main sur les vrais fichiers, puis restaurés : `#ff0000` dans `EcranConnexion.css` fait échouer le filtre en nommant le fichier ; `htmlFor` retiré de `ChampTexte.tsx` fait échouer `axe` sur la règle `label`.
- `scripts/gates/build.sh` 0 (Vitest 123/123, seuils tenus) ; `check-gates-exist.sh` 0, aucun KO ; `golden-master.sh` 0 (statique, aucun rejeu).
- Pile reconstruite : `trace-smoke.sh` 0 ; `curl` de `/login` → 200 avec l'en-tête `Content-Security-Policy` ; `/assets/inter-latin-400-normal-*.woff2` → 200 `font/woff2`.
- Navigateur (sous-agent, Playwright) : sans jeton, `/tableau-de-bord` mène à `/login` ; un refus affiche les deux lignes ; aucune erreur CSP.

**Risques résiduels :**
- Connexion réussie non jouée sur la pile : les données de référence ne sont chargées par aucune commande (reporté de 2.1), et les mots de passe `RETRO_*` ne sont pas dans `.env`. Les trois arrivées par rôle sont prouvées par les tests seulement.
- Sur ce poste, la pile tourne avec `NB_SOUS_RESEAU=172.31.0.0/24` et `NB_PORT_COEUR_BANCAIRE=18082` (conflits locaux, non committés).
- jsdom 30.1.2 demande Node ≥ 24.15. Le poste en 24.14 n'a qu'un avertissement ; l'image de construction est en 24.21.0.
- Le contrôle des rôles sur les routes protégées et l'expiration du jeton restent à livrer par les stories 2.4 et 2.5.
