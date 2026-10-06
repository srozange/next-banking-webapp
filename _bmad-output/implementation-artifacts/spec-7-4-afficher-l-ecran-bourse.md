---
title: 'Afficher l''écran Bourse'
type: 'feature'
created: '2026-10-06'
baseline_revision: 'd83d2e2ec1a012614d83dd9503d23dad0332b4f6'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-7-context.md'
warnings: []
deferred:
  - summary: >-
      Les vert et rouge de l'écran viennent des tokens `--color-success` (#0b7a3e) et `--color-error` (#c4122f), pas des teintes legacy #2e7d32 et #E3000F.
    evidence: |-
      `tokens.css` est généré depuis DESIGN.md et le test `couleurs-hors-tokens` interdit toute couleur en dur dans `src` ; l'écart de teinte avec le legacy relève du design (UX-DR1), pas de cette story.
    location: >-
      frontend/src/adapters/ecrans/bourse/EcranBourse.css
    severity: low
  - summary: >-
      Le service bourse renvoie des prix à 4 décimales ("170.1600") ; le front les arrondit à 2 décimales (`centimesDepuisPrix`).
    evidence: |-
      Contrat constaté dans le service (7.2/7.3) ; l'arrondi demi-supérieur est cohérent avec l'affichage legacy à 2 décimales, à confirmer par le métier si l'arrondi exact importe.
    location: >-
      frontend/src/domain/bourse.ts:44
    severity: low
---

<intent-contract>

## Intent

**Problem:** `/bourse` est un lien d'en-tête sans écran : le service bourse (watchlist, historique, graphique SVG serveur) existe mais aucun utilisateur ne peut le voir ni ajouter ou retirer une valeur (FR-009-01 à 13, FR-029-03 à 14).

**Approach:** Ajouter au front hexagonal la route protégée `/bourse` et son écran : cas d'usage et règles de calcul purs (centimes entiers), port `ClientBourse` et adaptateur HTTP, écran React. Même contenu pour CLIENT, CONSEILLER et ADMIN.

## Boundaries & Constraints

**Always:**
- Hexagone front : `src/domain` et `src/application` en TypeScript pur (aucun import npm, ni `Date.now()`/`new Date()`), couverture 100 % ; tout le React dans `src/adapters`.
- Montants : chaînes `"123.45"` du service ; le front calcule en centimes entiers (`centimesDepuisChaine`), jamais en flottant ; performance et « Var. J » en pourcentage à 2 décimales par calcul entier.
- Textes legacy à l'identique (`docs/retro/features/FEAT-029/MESSAGES.md`, `FEAT-009/MESSAGES.md`, casse comprise), nouveaux depuis `textes-nouveaux.md` (`bourse.graphique.nom`) ; rien d'inventé, un texte manquant est un point ouvert. Tous dans `TEXTES.bourse` (`adapters/textes.ts`).
- Titre d'onglet « BankApp Demo - Bourse » ; chargement : zone de contenu `aria-busy="true"` jusqu'aux données ; pas de bandeau API ni de squelette.
- `data-testid` `bourse-{élément}[-{id}]` sur tout élément interactif ; couleurs vert `#2e7d32` / rouge `#E3000F` uniquement par les variables de `tokens.css` (jamais en dur dans `src`) ; libellés « VARIATION SUR LA PÉRIODE » / « PERFORMANCE % » rendus en capitales par CSS (`text-transform`), le golden master compare `innerText`.
- Sélecteurs Valeur et Période : soumission immédiate au changement, 1M et première valeur présélectionnées, seul l'historique est rechargé.
- Appels via `fetchAuthentifie` (tout 401 → `/login`), `Authorization: Bearer`, chemins `/api/bourse/watchlist` (GET, POST, DELETE `/{id}`) et `/api/bourse/historique?symbole=&periode=` (`1W`, `1M`, `1Y`, `5Y`) ; erreurs `{"message"}`.
- Graphique : le SVG serveur (champ `graphique`, sans texte utilisateur) injecté dans une `card` `role="img"`, nom accessible « Évolution du cours de {symbole} sur {période} », sous « 📈 Évolution du cours ».

**Never:**
- Pas de bibliothèque de graphiques ni de nouveau paquet npm ; pas de `vitest.config.*` ; pas de texte inventé.
- Ne pas modifier `scripts/gates/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `sprint-status.yaml`, ni le golden master (`e2e/`, `docs/retro/golden-master/**`, `replay-mapping.json`) ; ne jamais rejouer le golden master.
- Pas de modification du service bourse (7.1 à 7.3 sont livrées) ; en cas d'écart de contrat, le signaler.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Ouverture | watchlist de référence | AI.PA, 1M présélectionnés ; indicateurs, graphique, 10 dernières séances (récent → ancien) | — |
| Hausse / baisse | performance ≥ 0 / < 0 | vert « ▲ + » / rouge « ▼ » ; ligne de séance verte si clôture ≥ ouverture | — |
| Sans données | `points: []` | message legacy « ⚠️ Aucune donnée reçue pour « {SYMBOLE} » sur {période libellée}… » ; graphique et tableau masqués, reste utilisable | — |
| Ajout invalide | symbole vide / nom vide / « bnp.pa » | formulaire ouvert, message du service (400) sous le formulaire, rien ajouté | texte `{"message"}` affiché tel quel |
| Ajout valide | « sg.pa », « Société Générale » | « Valeur ajoutée ! », formulaire fermé, SG.PA sélectionnée, historique chargé | — |
| Longueur | 25 car. symbole, 120 car. nom | champs bloqués à 20 et 100 (`maxLength`), sans message | — |
| Retrait | ✕ sur la valeur sélectionnée | retirée sans confirmation, première restante sélectionnée ; dernière retirée : sélection effacée, graphique et tableau masqués | — |
| Annuler | formulaire ouvert | refermé, focus rendu au bouton « + Ajouter une valeur » | — |
| Panne | réseau coupé / 5xx | écran reste utilisable, texte technique `api.erreur.technique` | non bloquant |

</intent-contract>

## Code Map

- `frontend/src/adapters/routes/routes.tsx` -- `ROUTES_PROTEGEES` / `TITRES_ONGLET` : aujourd'hui des `<title>` seuls ; ajouter `/bourse` avec l'écran (le script `verifier-scenario.sh` cherche le littéral `'/bourse'` dans ce fichier) ; `Dependances` reçoit `clientBourse`.
- `frontend/src/infrastructure/main.tsx` -- assemblage ; modèle : `creerClientProfilHttp(fetchAuthentifie)`. `frontend/src/adapters/test/rendreApplication.tsx` -- aide de rendu des tests, à compléter.
- `frontend/src/adapters/http/ClientProfilHttp.ts` (+ test) -- modèle d'adaptateur HTTP (statuts traduits, jamais le jeton journalisé). `application/chargerProfil.ts` -- modèle de port + cas d'usage.
- `frontend/src/domain/montant.ts` (`centimesDepuisChaine`), `application/lireMontant.ts` -- calculs en centimes. `domain/navigation.ts` : lien `bourse` → `/bourse` déjà présent.
- `frontend/src/adapters/ecrans/connexion/EcranConnexion.{tsx,css}` -- modèle d'écran ; `adapters/ui/{Carte,ChampTexte,BoutonPrimaire,BoutonTexte,AlerteErreur}`, `adapters/gabarit/GabaritAuthentifie.tsx` (rend déjà l'en-tête et `ContenuPrincipal occupe`) ; `adapters/textes.ts`.
- Tests d'architecture à garder verts : `src/{motifs-interdits,architecture-temps,couleurs-hors-tokens}.test.ts`, `accessibilite.test.tsx` (axe).
- Contrat service (lecture seule) : `services/bourse/.../adapter/in/http/PointEntreeHistorique.java` (`Historique{symbole,periode,points[{date,ouverture,haut,bas,cloture,volume}],graphique?}`, points par date croissante), `PointEntreeWatchlist.java` (`Valeur{id,symbole,nom,marche,devise}` ; POST 201), `Textes.java`.
- Exigences : `_bmad-output/planning-artifacts/epics.md` « Story 7.4 » ; `docs/retro/features/FEAT-009/` (RG, MESSAGES, screens/SCR-009-*.png) et `FEAT-029/` ; scénario `docs/retro/golden-master/derived/FEAT-009/SC-009-01` (start_url `/bourse`, textes Valeur, BNP.PA, AI.PA, VARIATION SUR LA PÉRIODE, PERFORMANCE %) ; tags de tests = identifiants FR dans le nom du `it`/`describe`.

## Tasks & Acceptance

**Execution:**
- `frontend/src/domain/bourse.ts` (+ test) -- périodes (code, libellé), indicateurs (cours, variation, performance, signe, préfixes), « Var. J », sélection des 10 dernières séances, choix de la sélection après ajout/retrait -- règles pures, 100 %, tests tagués FR-009-*/FR-029-*
- `frontend/src/application/` `consulterBourse.ts`, `gererWatchlist.ts` (+ tests) -- port `ClientBourse` (watchlist, historique, ajouter, retirer) et cas d'usage -- sans React
- `frontend/src/adapters/http/ClientBourseHttp.ts` (+ test) -- adaptateur HTTP via `fetchAuthentifie`, traduction des statuts et `{"message"}` -- ≥ 80 %
- `frontend/src/adapters/ecrans/bourse/EcranBourse.{tsx,css}` (+ test) et `adapters/textes.ts` -- écran, formulaire d'ajout, tableaux, graphique, message sans données -- UX et AC de la story
- `frontend/src/adapters/routes/routes.tsx`, `infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx` -- route `/bourse` protégée, injection de `clientBourse` -- les trois rôles voient le même écran
- Tests : routes (non authentifié → `/login`, trois rôles), accessibilité axe de l'écran, couverture JaCoCo-équivalent Vitest respectée

**Acceptance Criteria:**
- Given un utilisateur CLIENT, CONSEILLER ou ADMIN, when il ouvre `/bourse`, then même écran ; non authentifié → `/login`.
- Given la watchlist de référence, when l'écran s'ouvre, then 1M et AI.PA sélectionnés, `aria-busy` jusqu'aux données, textes Valeur, BNP.PA, AI.PA, Variation sur la période, Performance % présents.
- Given un historique, when affiché, then « {nom} — {symbole} », cours actuel, variation (+ si positive), performance, Marché, vert « ▲ + » ou rouge « ▼ », graphique sous « 📈 Évolution du cours » (`role="img"`, nom accessible, badge de période), 10 dernières séances avec « Var. J ».
- Given un changement de Valeur ou Période, then seul l'historique est rechargé, sans bouton.
- Given `points: []`, then message legacy, graphique et tableau masqués.
- Given la watchlist, then « ⭐ Ma Watchlist (9 valeurs) », colonnes Symbole, Nom, Marché, Devise, Suppr., triée par le service ; formulaire d'ajout, validations, `maxLength`, « Valeur ajoutée ! », ✕ avec infobulle « Supprimer {nom} de la watchlist » conformes à la story 7.4 de `epics.md`.
- Given la story terminée, then `cd frontend && npx vitest run -t '<FR>'` code 0 pour chacune de FR-009-01…07, 09…13 et FR-029-03, 06…14 ; `scripts/golden-master/verifier-scenario.sh SC-009-01` code 0 ; `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh` code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 0, false 5, maybe-false 0
- findings:
  - `[false]` `[reject]` Les capitales de « VARIATION SUR LA PÉRIODE » / « PERFORMANCE % » reposent sur le CSS, non exercé par jsdom — c'est le choix voulu par la spec (`text-transform`) ; `verifier-scenario.sh` tolère ce cas et le golden master ne se rejoue qu'à l'epic 10.
  - `[false]` `[reject]` Le lien entre étiquettes FR et couverture repose sur le nommage — `vitest -t '<FR>'` est le mécanisme imposé par la story ; les 22 étiquettes passent.
  - `[false]` `[reject]` `epic-7-context.md` modifié, hors critères de la story — recompilation du cache par le workflow (planification plus récente), pas un changement de la story.
  - `[false]` `[reject]` `routes.test.tsx` : description de `ROUTES_PROTEGEES` — test adapté et vert, aucune conséquence observable.
  - `[false]` `[reject]` Aucun test commun avec le service réel (service, proxy) — hors périmètre : le service est livré (7.1 à 7.3), l'adaptateur est testé contre le contrat lu dans `PointEntreeHistorique`/`PointEntreeWatchlist`.

## Design Notes

- Calcul de pourcentage en entiers : `performance = (derniere − premiere) × 100 / premiere` sur centimes, arrondi à 2 décimales par division entière explicite (règle de l'arrondi de la RG de FEAT-009 à lire avant de coder) ; affichage `fr` avec virgule seulement si le legacy le fait — suivre `FEAT-009/RG.md` et les captures SCR-009-01.
- Le SVG serveur ne contient aucun texte saisi (garanti par 7.3) : injection `dangerouslySetInnerHTML` acceptable, commentée ; sans `graphique`, la zone reste absente.
- Les couleurs vert/rouge existent peut-être déjà comme tokens (`success`/`error`) : les réutiliser ; si absentes, le signaler (le fichier est généré par `npm run tokens`, ne pas l'éditer à la main).

## Verification

**Commands:**
- `cd frontend && npx tsc --noEmit && npx vitest run --coverage` -- expected: code 0, seuils respectés
- `cd frontend && for t in FR-009-01 FR-009-02 FR-009-03 FR-009-04 FR-009-05 FR-009-06 FR-009-07 FR-009-09 FR-009-10 FR-009-11 FR-009-12 FR-009-13 FR-029-03 FR-029-06 FR-029-07 FR-029-08 FR-029-09 FR-029-10 FR-029-11 FR-029-12 FR-029-13 FR-029-14; do npx vitest run -t "$t" || echo KO $t; done` -- expected: aucun KO
- `scripts/golden-master/verifier-scenario.sh SC-009-01` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` puis `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `/bourse` est une route protégée qui affiche, identique pour CLIENT, CONSEILLER et ADMIN, la watchlist commune, les indicateurs (cours, variation, performance, marché), le graphique SVG du service dans une `card` `role="img"` au nom accessible, les 10 dernières séances, et permet d'ajouter ou de retirer une valeur ; l'historique seul se recharge au changement de valeur ou de période. Aucun geste humain requis : pas d'`operator_actions`.

**Fichiers :**
- `frontend/src/domain/bourse.ts` — règles pures en centimes entiers ; `application/{consulterBourse,gererWatchlist}.ts` — port `ClientBourse` et cas d'usage.
- `frontend/src/adapters/http/ClientBourseHttp.ts` — adaptateur HTTP ; `adapters/ecrans/bourse/EcranBourse.{tsx,css}` — écran ; `adapters/textes.ts` — `TEXTES.bourse`.
- `frontend/src/adapters/ui/{BoutonSecondaire,ListeDeroulante}` (nouveaux), `Carte`, `ChampTexte`, `BoutonTexte` — propriétés facultatives.
- `routes.tsx`, `infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx` — route et injection ; tests associés dont accessibilité (axe).
- `_bmad-output/implementation-artifacts/epic-7-context.md` — cache de contexte recompilé (AR-102 tranché).

**Revue :** 5 constats, tous rejetés comme faux (motifs ci-dessus) ; 0 correctif ; 2 reports de gravité faible (teintes vert/rouge issues des tokens, arrondi des prix à 4 décimales). Revue complémentaire recommandée : non.

**Vérification :** `tsc --noEmit` et `vitest run --coverage` (466 tests) code 0 ; les 22 étiquettes FR en code 0 (relevé du sous-agent) ; `build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh`, `trace-smoke.sh` code 0 ; `verifier-scenario.sh SC-009-01` code 0. Golden master non rejoué.

**Risques résiduels :** teintes vert/rouge différentes du legacy ; capitales des libellés dépendantes du CSS (non vérifiables en jsdom) ; contrôlées au rejeu de l'epic 10.
