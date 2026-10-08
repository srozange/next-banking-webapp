---
title: 'Définir et suivre des plafonds de dépenses par catégorie'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: 'aed6a2d45450494cfe4b194192861a815c1c9d5e'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-12-consulter-ses-depenses-du-mois-par-categorie.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** l'écran `/budget` (story 4.12) affiche les dépenses du mois par catégorie mais ne permet ni de fixer un plafond mensuel, ni de le supprimer, ni de voir la consommation et les dépassements ; la table `plafonds_budget` (V9) n'a aucune écriture.

**Approach:** étendre le cœur bancaire (enregistrement par remplacement, suppression, plafonds et dépenses dans la réponse du budget) et l'écran `/budget` (formulaire « Definir un plafond », barre de progression, badge « Depassement », tableau « Plafonds du mois {AAAA-MM} »). Le texte exact des critères est la story 4.13 de `_bmad-output/planning-artifacts/epics.md` (à partir de la ligne 3649) et FEAT-006 du PRD (FR-006-05 à FR-006-11, NFR-F006) : la lire en entier, ainsi que `docs/retro/features/FEAT-006/{MESSAGES,RG}.md` (RG-006-013 à RG-006-027) et `textes-nouveaux.md` (lot « Stories », `budget.erreur.plafond.max`).

## Boundaries & Constraints

**Always:** hexagone (`domain`/`application` Java pur et TypeScript pur, 100 % lignes et branches) ; temps par le port `Horloge` seul ; montants `BigDecimal` 2 décimales, chaîne `"500.00"` en JSON, front en centimes entiers (jamais de flottant) ; client déduit du jeton (`sub`), aucun identifiant de client en paramètre ; textes du legacy repris à l'identique (casse et absence d'accents comprises) sauf « Le plafond ne doit pas dépasser 13 chiffres avant la virgule » (`textes-nouveaux.md`) ; aucun texte inventé ; couleurs par les tokens du thème (barre verte `success`, orange `warning`, rouge `error`, fond de dépassement `error-container`, montant dépassé `error`) ; `data-testid` `budget-*` ; tests tagués `FR-006-05` à `FR-006-11` et `BUG-023` (Java `@Tag`, Vitest dans le titre `[FR-006-0x]`) ; versions de `versions-verifiees.md`.
- **API** : `PUT /api/coeur-bancaire/budget/plafonds` (corps `{categorie, mois, plafond}`, `mois` = `AAAA-MM` de la période affichée, plafond en chaîne décimale) crée ou remplace le plafond unique (client, catégorie, mois) en une instruction atomique (`insert … on conflict (client_id, categorie, mois) do update`) et rend 200 avec le plafond ; `DELETE /api/coeur-bancaire/budget/plafonds/{id}` supprime immédiatement (204) ; un identifiant inexistant ou appartenant à un autre client rend le même 404 `{"message": …}`. `GET …/budget` ajoute `plafonds[{id, categorie, plafond, depense}]` pour la période (ordre des catégories ; `depense` = dépenses de la catégorie sur la période, `"0.00"` si aucune), `plafondsDefinis` en reste le nombre.
- **Validation (400 `{"message": …}`, jamais 500)** : catégorie absente, inconnue ou `Revenus` (BUG-023, FR-006-06) → « Veuillez selectionner une categorie » ; plafond absent, non numérique, nul ou négatif → « Veuillez saisir un plafond valide » ; plus de 13 chiffres entiers → « Le plafond ne doit pas dépasser 13 chiffres avant la virgule » ; `mois` illisible → 400 ; plafond arrondi à 2 décimales. Aucun plafond n'est créé en cas de refus. Toute autre panne : 500 sans détail ; l'écran affiche alors `TEXTES.erreurTechnique` (« Une erreur est survenue. Veuillez reessayer. », A-006-1), jamais « Erreur : … » ni « Erreur suppression : … ».
- **Progression** (pure, domaine front, en centimes) : pourcentage entier = ⌊dépense × 100 / plafond⌋ ; couleur verte < 80, orange 80 à 99, rouge ≥ 100 ; remplissage plafonné à 100 ; badge « Depassement », fond `error-container` et montant en `error` seulement si dépense > plafond (500,00 → 100 % rouge sans badge ; 500,01 → badge) ; barre `role="progressbar"` avec `aria-valuenow`, `aria-valuemin`/`aria-valuemax`, libellé « {pourcentage}% du plafond » ; sans plafond « Aucun plafond defini ». Catégories de la répartition uniquement (une catégorie avec plafond mais sans dépense n'est que dans le tableau).
- **Front** : formulaire « Definir un plafond » (`<select>` « Categorie » `budget-categorie` : « -- Choisir -- » puis Logement, Alimentation, Transport, Sante, Loisirs, Abonnements, Epargne, Assurances, Autres ; champ « Plafond mensuel (EUR) » `budget-plafond`, placeholder « ex: 500 » ; bouton `budget-enregistrer` « Enregistrer le plafond ») placé sous le titre « Plafonds budgetaires » ; contrôles locaux avant appel (catégorie, plafond vide/nul/négatif/non numérique, 14 chiffres entiers) ; succès « Plafond enregistre pour {catégorie} » en `role="status"`, champs vidés, écran rechargé ; erreurs en `role="alert"`, `aria-invalid`, saisie conservée ; tableau « Plafonds du mois {AAAA-MM} » (colonnes Categorie, Plafond, Depense, Ecart, Action ; `<th scope="col">`; écart plafond − dépense en `error` si négatif, `success` sinon ; bouton « Supprimer » `budget-supprimer-{id}` sans confirmation) affiché seulement s'il y a au moins un plafond ; la section est placée après le contenu déjà comparé (golden master).

**Never:** modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils, exclusions ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; modifier la catégorisation (`CategorieBudget`) sauf pour exposer la liste des catégories plafonnables ; `now()`/`Clock`/`Date.now()`/`new Date()` hors adaptateurs d'horloge ; appel HTTP entre services ; span créé à la main ; écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Création | Alimentation 500 pour 2026-10 | un plafond, ligne et décompte à 1, « Plafond enregistre pour Alimentation », champs vidés | — |
| Remplacement | puis Alimentation 300 | toujours un seul plafond, valeur 300 | — |
| Catégorie Revenus (API) | `PUT` Revenus | aucun plafond créé | 400 (`FR-006-06`, `BUG-023`) |
| Formulaire invalide | sans catégorie ; vide, `0`, `-10`, `abc` | pas d'appel ; « Veuillez selectionner une categorie » / « Veuillez saisir un plafond valide » | messages en `role="alert"` |
| Plafond trop grand | 14 chiffres entiers (API et écran) | refus explicite | 400, « Le plafond ne doit pas dépasser 13 chiffres avant la virgule » |
| Suppression | clic « Supprimer » | supprimé aussitôt, sans confirmation, liste rafraîchie | — |
| Plafond d'autrui | `DELETE` de l'id d'un autre client ou inexistant | rien supprimé, même réponse | 404 identique |
| Panne | 500 ou corps illisible à l'enregistrement puis à la suppression | « Une erreur est survenue. Veuillez reessayer. » sans détail | — |
| Progression | plafond 500 ; dépenses 399 / 400 / 500 / 500,01 ; catégorie sans plafond | 79 % vert / 80 % orange / 100 % rouge sans badge / 100 % rouge plafonné + « Depassement » + fond + montant rouge ; « Aucun plafond defini » | — |
| Tableau | un plafond dépassé, un non dépassé ; aucun plafond | écarts rouge / vert ; tableau absent sans plafond | — |
| Sans session | `PUT`/`DELETE` sans jeton | 401 | — |

</intent-contract>

## Code Map

Cœur bancaire sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `…/src/test/java/fr/nextbanking/coeurbancaire/` (`T/`) ; front sous `frontend/src/`.

- `M/application/port/out/Plafonds.java`, `M/adapter/out/persistance/PlafondsJdbc.java` -- étendre : `enregistrer(clientId, categorie, mois, plafond)` (upsert), `supprimer(clientId, id)` (booléen, filtre `client_id`), `duMois(clientId, mois)` ; `compter` existant conservé ; table V9 déjà prête (pas de nouvelle migration, sauf besoin avéré).
- `M/application/ConsulterBudgetCasUsage.java`, `port/in/ConsulterBudget.java` -- ajouter les plafonds et leur dépense au record `Budget` ; nouveaux `GererPlafondsCasUsage` + `port/in/GererPlafonds` (validation 400, refus de Revenus, 13 chiffres, 404) ; exceptions applicatives sur le modèle de `GererVirementsProgrammes*`.
- `M/domain/CategorieBudget.java` -- ajouter `estPlafonnable()` (faux pour `REVENUS`) ; `M/domain/Montant` réutilisé.
- `M/adapter/in/PointEntreeBudget.java` (+ nouveau point d'entrée des plafonds, modèle des points d'entrée de `virements programmés`), gestion d'erreurs `{"message": …}` du module, `M/infrastructure/AssemblageBudget.java`, `ConfigurationSecurite.java` (routes authentifiées).
- Tests : `T/application/MemoireCoeurBancaire` (implémenter les nouvelles méthodes de `Plafonds`), `T/domain/CategorieBudgetTest`, `T/application/{ConsulterBudget,GererPlafonds}CasUsageTest`, `T/adapter/out/persistance/PlafondsJdbcIT`, `T/adapter/BudgetIT` (HTTP : 400, 404, 401, isolation A/B, upsert unique), tests `*ContratHttp*` existants si la forme de la réponse est figée quelque part.
- Front : `domain/budget.ts` (+ `domain/plafond.ts` pur : validation, pourcentage, couleur, écart, 13 chiffres), `application/consulterBudget.ts` + `application/gererPlafonds.ts` (port `ClientPlafonds.enregistrer/supprimer`), `adapters/http/ClientBudgetHttp.ts` (validation de forme étendue) + `ClientPlafondsHttp.ts` (modèle `ClientVirementsProgrammesHttp.ts`), `adapters/ecrans/budget/EcranBudget.{tsx,css}` (modèle des formulaires de `EcranVirementsProgrammes`), `adapters/textes.ts` (bloc `budget`), `adapters/routes/routes.tsx`, `infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx` (nouveau paramètre en dernier), leurs tests Vitest.
- Lecture seule : `docs/retro/**`, golden master, `scripts/gates/**`, `scripts/golden-master/verifier-scenario.sh` (SC-006-01 doit rester vert).

## Tasks & Acceptance

**Execution:**
- `M/domain`, `M/application/**` -- plafonnable, enregistrement, suppression, plafonds et dépenses dans le budget -- FR-006-05..08, 10, 11, BUG-023
- `M/adapter/**`, `M/infrastructure/AssemblageBudget` -- API `PUT`/`DELETE`, extension du `GET`, persistance -- FR-006-05..08, NFR-SEC-04
- `T/**` -- 100 % lignes et branches domain/application, 80 % adapter/infrastructure ; une `@Tag` par exigence (`FR-006-05` à `FR-006-11`, `BUG-023`) ; test de concurrence/unicité du plafond (upsert)
- `frontend/src/{domain,application}/**` + tests -- règles pures (validation, progression, écart) -- FR-006-05, 09, 10, 11
- `frontend/src/adapters/**` + tests Vitest `[FR-006-0x]` -- formulaire, barre, badge, tableau, erreurs génériques -- UX
- Ne toucher à aucun contrôle ni exclusion.

**Acceptance Criteria:**
- Given la story 4.13 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-006-05|FR-006-06|FR-006-07|FR-006-08|FR-006-09|FR-006-10|FR-006-11|BUG-023'` puis `cd frontend && npx vitest run -t 'FR-006-05|FR-006-06|FR-006-07|FR-006-08|FR-006-09|FR-006-10|FR-006-11|BUG-023'`, when lancés, then code 0 chacun
- Given `scripts/golden-master/verifier-scenario.sh SC-006-01`, when lancé, then code 0
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` (et `trace-smoke.sh` si la pile compose est disponible), when lancés, then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 4, false 0, maybe-false 0
- findings:
  - `[low]` `[reject]` aucun test ne joint l'écran au vrai service (le champ en erreur se déduit du texte du 400 de chaque côté) — chaque couche est prouvée avec sa copie du contrat (`BudgetIT` HTTP sur base réelle, `ClientPlafondsHttp` et écran Vitest) ; un test de bout en bout relèverait du golden master, interdit avant l'epic 10.
  - `[low]` `[reject]` couleurs des tokens vérifiées par classes CSS et non par rendu calculé — jsdom ne calcule pas les styles ; les classes `success`/`warning`/`error`/`error-container` sont testées.
  - `[low]` `[reject]` chemin 500 du service non exercé dans `BudgetIT` — il tombe sur le gestionnaire d'erreurs générique existant ; l'écran est prouvé en Vitest (panne à l'enregistrement et à la suppression).
  - `[low]` `[reject]` `FR-006-09` sans `@Tag` Java — la progression est une règle pure du front (`domain/plafond.ts`), tagée en Vitest.

## Design Notes

Le remplacement du plafond se fait en une instruction SQL (`on conflict … do update`) : deux enregistrements concurrents ne laissent qu'une ligne. Un plafond « Revenus » reste refusé même si une ligne existait déjà (RG-006-028 remplacée par FR-006-06). Un plafond sur « Autres » est autorisé ; la dépense d'une catégorie sans ligne dans la répartition vaut `0.00` dans le tableau. Le pourcentage est calculé en centimes entiers côté front (division entière), jamais en flottant.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-006-05|FR-006-06|FR-006-07|FR-006-08|FR-006-09|FR-006-10|FR-006-11|BUG-023'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-006-05|FR-006-06|FR-006-07|FR-006-08|FR-006-09|FR-006-10|FR-006-11|BUG-023'` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-006-01` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0


## Auto Run Result

Status: done

**Résumé :** le client fixe un plafond mensuel par catégorie sur `/budget` (hors « Revenus », BUG-023), le remplace ou le supprime sans confirmation, et voit sa consommation : barre de progression (vert < 80 %, orange 80-99 %, rouge ≥ 100 %), badge « Depassement », « Aucun plafond defini », tableau « Plafonds du mois {AAAA-MM} ». API `PUT`/`DELETE /api/coeur-bancaire/budget/plafonds`, `GET …/budget` étendu.

**Réparation de la vérification :** `scripts/gates/build.sh` était rouge sur 3 tests de concurrence (`VirementTransitionConcurrenteTest` ×2, `EcheanceTransitionConcurrenteTest`) : ils utilisaient l'horloge réelle, et le contrôle de fraude suspend tout virement à partir de 23 h (heure inhabituelle). Correction dans les tests : horloge fixe `HorlogeFixe` (10 h Paris) injectée via `@TestConfiguration`, comme dans `VirementIT`. Aucun contrôle ni seuil touché.

**Vérification :** `build.sh`, `check-gates-exist.sh` et `golden-master.sh` : code 0 (à 23 h 30).

**Risques résiduels :** `trace-smoke.sh` non lancé (pile compose non démarrée) ; un 404 à la suppression est traité côté écran comme une erreur technique générique.
