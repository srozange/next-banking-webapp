---
title: 'Afficher l''écran Produits'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: 'dcbf11acb3542677746c192f8f6b864d77852329'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-7-context.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Le dépôt minimal s'affiche « 300,00 EUR » alors que le legacy affichait « EUR300 » (format monétaire JSF non spécifié par le PRD).
    evidence: |-
      Choix de cette story (Design Notes), à confirmer par le métier ; le golden master SC-011-01 ne compare pas cette valeur.
    location: >-
      frontend/src/domain/produits.ts
    severity: low
---

<intent-contract>

## Intent

**Problem:** `/produits` est un lien d'en-tête sans écran : le catalogue (service open-banking-conseiller) et le journal des traitements nocturnes (cœur bancaire), livrés par la story 7.5, ne sont visibles par aucun utilisateur (FEAT-011, FR-010-01).

**Approach:** Ajouter au front hexagonal la route protégée `/produits` et son écran en lecture seule : règles de mise en forme pures (domain), port `ClientProduits` et cas d'usage qui appelle les deux services en parallèle (application), adaptateur HTTP et écran React (adapters). Même contenu pour CLIENT, CONSEILLER et ADMIN.

## Boundaries & Constraints

**Always:**
- Hexagone front : `src/domain` et `src/application` en TypeScript pur (aucun import npm, ni `Date.now()`/`new Date()`), couverture 100 % ; tout le React dans `src/adapters`.
- Appels via `fetchAuthentifie` (tout 401 → `/login`), `Authorization: Bearer`, `GET /api/open-banking-conseiller/produits` (enveloppe `{epargne[], credit[], investissement[]}` ; champs `id, nom, description, taux, depotMin, capitalGaranti, dureeMaxMois` / `…taux, dureeMaxMois, ltvMax, assuranceRequise` / `…secteur, niveauRisque, rendementAttendu`) et `GET /api/coeur-bancaire/traitements?limite=50` (`job, debut, fin` ISO-8601 avec décalage, `dureeMs, enregistrements, statut, details`) ; **en parallèle** (`Promise.all`), jamais en cascade ; le « Rafraichir » refait les deux appels.
- Taux, rendement, LTV : chaînes décimales du service (« 0.0300 »), jamais de flottant ; affichage en pourcentage à deux décimales avec virgule par calcul entier (« 3,00 % », « 6,50 % », LTV « 80,00 % ») — BUG-039. Dépôt min : chaîne `"300.00"` mise en forme sans flottant (« 300,00 EUR »).
- Textes legacy à l'identique, casse et absence d'accents comprises (`docs/retro/features/FEAT-011/MESSAGES.md`), titres « Traitements nocturnes » et « Historique des traitements nocturnes » sans « (EJB @Schedule) », bandeau JPA supprimé ; tous dans `TEXTES.produits` (`adapters/textes.ts`). Rien d'inventé.
- Titre d'onglet « BankApp Demo - Produits bancaires » (MSG-011-001) ; zone de contenu `aria-busy="true"` jusqu'aux données ; pas de squelette.
- `data-testid` `produits-{élément}[-{id}]` sur tout élément interactif (ici `produits-rafraichir`) et sur les sections ; capitales par CSS (`text-transform`), le golden master compare `innerText` ; couleurs uniquement par les variables de `tokens.css`.
- Dates « dd/MM/yyyy HH:mm:ss » obtenues par découpage de la chaîne ISO (déjà au fuseau Europe/Paris) ; absent → vide.
- Sections vides par type (A-011-1) ; une panne d'un des deux services n'empêche pas d'afficher l'autre, et annonce `api.erreur.technique`.

**Never:**
- Aucun champ de saisie ; pas de nouveau paquet npm ; pas de `vitest.config.*` ; pas de texte inventé ; pas de rôle exigé.
- Ne pas modifier `scripts/gates/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `sprint-status.yaml`, ni le golden master ; ne jamais le rejouer ; pas de modification des services (7.5 livrée) — écart de contrat : le signaler.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Ouverture | catalogue 4/4/5 et journal | trois sections triées (ordre du service), booléens « Oui »/« Non », « {n} mois », `aria-busy` jusqu'aux données | — |
| Taux | `0.0300` / rendement `0.0650` | « 3,00 % » / « 6,50 % » | jamais « 0.03 » ni « 0,03 % » |
| Absents | taux, dépôt, LTV, secteur, rendement, durée crédit `null` | « — » | durée épargne `null` → « Illimite » |
| Risque | niveau 1…5 / 0, 6, `null` | « Tres faible (1/5) » … « Tres eleve (5/5) » / « N/A » | — |
| Type vide | un groupe `[]` | « Aucun produit d epargne / de credit / d investissement disponible » dans sa section seule | — |
| Journal | 50 exécutions | « Job, Debut, Fin, Duree (ms), Enregistrements, Statut, Details », ordre du service, `dureeMs` affiché | début/fin absent : cellule vide, durée 0 |
| Journal vide | `[]` | « Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00 » | — |
| Rafraîchir | clic | les deux services rappelés en parallèle, reste sur `/produits` | — |
| Non authentifié / 401 | pas de jeton ou 401 | `/login`, jeton supprimé | — |
| Panne | un service en 5xx ou réseau coupé | l'autre section s'affiche, texte technique annoncé | non bloquant |

</intent-contract>

## Code Map

- `frontend/src/adapters/routes/routes.tsx` -- ajouter `/produits` avec l'écran (le littéral `'/produits'` est cherché par `verifier-scenario.sh`) ; `Dependances.clientProduits`. Modèle : route `/bourse`.
- `frontend/src/infrastructure/main.tsx`, `frontend/src/adapters/test/rendreApplication.tsx` -- injection du client, aide de rendu à compléter (défaut : panne technique).
- `frontend/src/adapters/http/ClientBourseHttp.ts` (+ test) -- modèle d'adaptateur ; `application/consulterBourse.ts` -- modèle de port, `jetonOuFin`, résultats `deconnecte`/`technique`.
- `frontend/src/domain/montant.ts` -- centimes entiers ; `adapters/ecrans/bourse/EcranBourse.{tsx,css}` -- modèle d'écran (cartes, tableaux, `aria-busy`), `adapters/ui/{Carte,BoutonSecondaire,AlerteErreur}`, `adapters/textes.ts` (`TEXTES.titresOnglet`).
- Tests d'architecture à garder verts : `src/{motifs-interdits,architecture-temps,couleurs-hors-tokens}.test.ts`, `accessibilite.test.tsx` (axe, ajouter `/produits`).
- Contrat service (lecture seule) : `services/open-banking-conseiller/.../PointEntreeProduits.java`, `services/coeur-bancaire/.../PointEntreeTraitements.java`.
- Sources : `docs/retro/features/FEAT-011/{MESSAGES,RG,FIELDS}.md` ; scénario `docs/retro/golden-master/derived/FEAT-011/SC-011-01` (start_url `/produits`, textes « Catalogue des produits », « Produits d epargne », « CEL - Compte Epargne Logement », « Livret A ») ; `epics.md` « Story 7.6 » ; PRD FR-011-01…09.

## Tasks & Acceptance

**Execution:**
- `frontend/src/domain/produits.ts` (+ test) -- types, pourcentage entier (BUG-039), libellé de risque, durée, booléen, valeur absente, durée de traitement/date -- règles pures, 100 %, tests tagués FR-011-*
- `frontend/src/application/consulterProduits.ts` (+ test) -- port `ClientProduits` (catalogue, traitements), cas d'usage en parallèle, 401 → déconnecté, panne partielle -- sans React
- `frontend/src/adapters/http/ClientProduitsHttp.ts` (+ test) -- adaptateur HTTP via `fetchAuthentifie`, validation du contrat -- ≥ 80 %
- `frontend/src/adapters/ecrans/produits/EcranProduits.{tsx,css}` (+ test) et `adapters/textes.ts` -- écran, trois sections, journal, « Rafraichir » -- UX et AC
- `frontend/src/adapters/routes/routes.tsx`, `infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx`, `accessibilite.test.tsx` -- route protégée, injection, test axe, routes (non authentifié → `/login`, trois rôles)

**Acceptance Criteria:**
- Given un CLIENT, CONSEILLER ou ADMIN, when il ouvre `/produits`, then même écran « Catalogue des produits » sans champ de saisie ; non authentifié → `/login` ; les deux services sont demandés en parallèle.
- Given l'écran, when je clique « Rafraichir », then catalogue et journal rechargés, route inchangée.
- Given les données de référence, then sections, colonnes, « Oui »/« Non », « {n} mois », « — », « Illimite », « 3,00 % », « 6,50 % », libellés de risque, « N/A », sections vides et journal (ordre, durée, état vide) conformes à la matrice ; `[BUG-039]` échoue si l'affichage rend « 0.03 », « 0,03 % » ou une forme non multipliée par 100.
- Given la story terminée, then `cd frontend && npx vitest run -t '<tag>'` code 0 pour FR-010-01, FR-011-01…06, FR-011-08, FR-011-09 et BUG-039 ; `scripts/golden-master/verifier-scenario.sh SC-011-01` code 0 ; `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh` code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 0, false 4, maybe-false 0
- findings:
  - `[false]` `[reject]` Chemins `/api/open-banking-conseiller/**` et `/api/coeur-bancaire/**` sans routage dans le diff — `frontend/vite.config.ts` (8082, 8088) et `frontend/nginx.conf` (liste fermée) les renvoient déjà ; rien à changer.
  - `[false]` `[reject]` `?limite=50` non vérifié côté service — appliqué et testé par la story 7.5 (`TraitementsIT`, FR-011-09) ; hors périmètre du front.
  - `[false]` `[reject]` Adaptateur strict : un champ mal formé donne une erreur technique pour toute la section — même parti pris que `ClientBourseHttp` ; le contrat du service est typé, les valeurs absentes (`null`) sont acceptées et rendues « — » ; la spec prévoit la panne partielle.
  - `[false]` `[reject]` Tests sur doublures, golden master non rejoué, capitales par CSS — voulu : `AGENTS.md` interdit le rejeu avant l'epic 10 ; `verifier-scenario.sh SC-011-01` code 0 ; même choix que la story 7.4.

## Design Notes

- Pourcentage : valeur `"0.0300"` → entier en dix-millièmes (300) → deux décimales par division entière ; « 3,00 % » (espace avant `%`, comme le PRD). Une chaîne hors format décimal est rejetée par l'adaptateur (erreur technique).
- Dépôt min : le legacy affiche « EUR300 » (format monétaire JSF, non spécifié par le PRD) ; choix de cette story : « 300,00 EUR », à confirmer par le métier.
- Défaut du port de test : panne technique, comme `clientBourse`.

## Verification

**Commands:**
- `cd frontend && npx tsc --noEmit && npx vitest run --coverage` -- expected: code 0, seuils respectés
- `cd frontend && for t in FR-010-01 FR-011-01 FR-011-02 FR-011-03 FR-011-04 FR-011-05 FR-011-06 FR-011-08 FR-011-09 BUG-039; do npx vitest run -t "$t" || echo KO $t; done` -- expected: aucun KO
- `scripts/golden-master/verifier-scenario.sh SC-011-01` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` puis `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `/produits` est une route protégée qui affiche, identique pour CLIENT, CONSEILLER et ADMIN, le catalogue (épargne, crédit, investissement) et l'historique des traitements nocturnes (50 au plus), lus en parallèle aux deux services ; « Rafraichir » recharge les deux. Taux, rendements et LTV en pourcentage entier à deux décimales (BUG-039), « — » / « Illimite », risque « {libellé} ({n}/5) » / « N/A », sections vides par type. Aucun geste humain requis : pas d'`operator_actions`.

**Fichiers :**
- `frontend/src/domain/produits.ts` — mise en forme pure ; `application/consulterProduits.ts` — port `ClientProduits` et chargement parallèle.
- `frontend/src/adapters/http/ClientProduitsHttp.ts` — adaptateur HTTP ; `adapters/ecrans/produits/EcranProduits.{tsx,css}` — écran ; `adapters/textes.ts` — `TEXTES.produits`.
- `routes.tsx`, `infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx` — route et injection ; tests associés dont accessibilité (axe).
- `reports/golden-master/textes-non-trouves.md` — rapport régénéré par `verifier-scenario.sh`.

**Revue :** 4 constats, tous rejetés comme faux (motifs ci-dessus) ; 0 correctif ; 1 report de gravité faible (format du dépôt min). Revue complémentaire recommandée : non.

**Vérification :** `tsc --noEmit` et `vitest run --coverage` (571 tests) code 0 ; étiquettes FR-010-01, FR-011-01…06, 08, 09 et BUG-039 en code 0 (relevé du sous-agent) ; `verifier-scenario.sh SC-011-01` code 0 ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0 (relevé du sous-agent). Golden master non rejoué.

**Risques résiduels :** capitales de « CATALOGUE DES PRODUITS » et « PRODUITS D EPARGNE » dépendantes du CSS (non vérifiables en jsdom, deux textes « non trouvés » consignés), contrôlées au rejeu de l'epic 10 ; secondes omises par le service rendues « :00 » par l'écran.
