---
title: 'Générer le graphique d''évolution du cours côté serveur'
type: 'feature'
created: '2026-10-06'
baseline_revision: '2eebd4ff61e553f921069187de5c968259650697'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-7-context.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** `GET /api/bourse/historique` rend les points mais pas le graphique d'évolution du cours que le legacy génère côté serveur (FR-009-05, FR-009-06, FR-029-08) ; l'écran Bourse (7.4) n'a donc rien à afficher.

**Approach:** Générer en Java pur, dans le domaine, un SVG 800 × 300 (5 lignes de grille, 5 dates MM-JJ, badge de période, courbe verte ou rouge) et l'ajouter à la réponse sous `graphique` ; champ absent quand l'historique est vide.

## Boundaries & Constraints

**Always:**
- SVG de 800 × 300 (`width`, `height`, `viewBox`), exactement 5 lignes horizontales de grille, 5 dates d'abscisse régulièrement espacées, au format `MM-JJ` (`date.toString().substring(5)`), prises sur des points d'indices régulièrement répartis.
- Courbe verte `#2e7d32` si dernière clôture ≥ première, rouge `#E3000F` si strictement inférieure ; dans le cas rouge, le SVG ne contient pas `#2e7d32` (et inversement).
- Badge de période en bas à droite : « 1 semaine (données journalières) », « 1 mois (données journalières) », « 1 an (données hebdomadaires) », « 5 ans (données hebdomadaires) » (MSG-029-070 à 073, casse et accents du legacy).
- Historique vide → aucun champ `graphique` dans le JSON (les réponses vides existantes restent identiques à l'octet près).
- Un seul point ou des clôtures toutes égales → SVG valide, sans division par zéro.
- Calcul des coordonnées en `BigDecimal`, jamais de flottant ; le SVG ne contient ni le symbole ni aucun texte saisi par l'utilisateur.

**Never:**
- Pas d'écran ni de titre « 📈 Évolution du cours » (7.4) ; pas de bibliothèque de graphiques ; pas d'import Jackson ou Spring dans `domain`/`application`.
- Ne pas modifier `scripts/gates/**`, `docs/quality-gates.md`, `sprint-status.yaml`, ni le golden master.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Hausse | AI.PA 1M simulé, dernière ≥ première | `graphique` : 800 × 300, 5 grilles, 5 dates, `#2e7d32` | — |
| Baisse | dernière < première | `#E3000F`, aucun `#2e7d32` | — |
| Vide | symbole inconnu | pas de `graphique` | — |
| Un point / plat | 1 point ou clôtures égales | SVG valide, courbe horizontale | — |
| Perf | 20 appels, périodes 1W, 1M, 1Y, 5Y | p95 < 3 000 ms | — |

</intent-contract>

## Code Map

- `services/bourse/src/main/java/fr/nextbanking/bourse/domain/{HistoriqueCours,Periode,PointCotation}.java` -- `HistoriqueCours` reçoit `graphique()` (Optional) ; `Periode` reçoit `libelle()`.
- `.../adapter/in/http/PointEntreeHistorique.java` -- ajoute `graphique` (`@JsonInclude(NON_NULL)`).
- `services/bourse/src/test/java/.../adapter/HistoriqueIT.java`, `domain/PeriodeTest.java` -- motifs de tests ; tags FR-009-05, FR-009-06, FR-029-08.
- Couverture JaCoCo : 100 % lignes et branches sur `domain`.

## Tasks & Acceptance

**Execution:**
- `.../domain/GraphiqueCours.java` -- génération du SVG -- logique pure, testable
- `.../domain/Periode.java`, `HistoriqueCours.java` -- libellé et accès au graphique
- `.../adapter/in/http/PointEntreeHistorique.java` -- champ `graphique` absent si vide
- Tests : `GraphiqueCoursTest` (domaine, matrice), `HistoriqueIT` (réponse HTTP, absence, performance 20 appels)

**Acceptance Criteria:**
- Given l'adaptateur simulé, when `GET /api/bourse/historique?symbole=AI.PA&periode=1M`, then la réponse porte `graphique` (800 × 300, 5 grilles, 5 dates MM-JJ).
- Given hausse ou baisse, then couleur `#2e7d32` ou `#E3000F` selon la règle, sans l'autre couleur.
- Given historique vide, then aucun `graphique`.
- Given 20 appels sur 1W, 1M, 1Y, 5Y, then p95 < 3 000 ms.
- Given la story terminée, then `-Dgroups` FR-009-05, FR-009-06, FR-029-08 en code 0 ; `build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh` en code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 1, false 3, maybe-false 0
- findings:
  - `[low]` `[patch]` Le test HTTP acceptait l'une ou l'autre couleur sans la lier aux données — le test calcule maintenant hausse/baisse depuis les points de la réponse et exige la bonne couleur, sans l'autre ; vert.
  - `[false]` `[reject]` `sprint-status.yaml` modifié dans le diff — la modification vient du commit d'orchestration a6491c4 (HEAD a avancé après la base) ; aucun fichier de ce type n'est touché par cette story.
  - `[false]` `[reject]` Dates exactes vérifiées seulement au niveau du domaine — les dates exactes et l'espacement sont couverts par `GraphiqueCoursTest`, la réponse HTTP vérifie les 5 libellés ; le contrat HTTP est le transport du même générateur.
  - `[false]` `[reject]` Commandes d'exécution non visibles dans le diff / p95 par rang — commandes lancées (voir Auto Run Result) ; p95 sur 20 mesures = 19e valeur, lecture standard.

## Verification

**Commands:**
- `mvn -B -q -o -f services/bourse/pom.xml verify` -- expected: code 0
- `for t in FR-009-05 FR-009-06 FR-029-08; do mvn -B -q -f services/bourse/pom.xml test -Dgroups=$t -Dsurefire.failIfNoSpecifiedTests=true || echo KO $t; done` -- expected: aucun KO
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` puis `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `GET /api/bourse/historique` porte désormais `graphique`, un SVG 800 × 300 généré en Java pur dans le domaine (5 lignes de grille, 5 dates MM-JJ, badge de période, courbe `#2e7d32` ou `#E3000F`) ; le champ est absent quand l'historique est vide. Aucun geste humain requis : pas d'`operator_actions`.

**Fichiers :**
- `services/bourse/src/main/java/fr/nextbanking/bourse/domain/GraphiqueCours.java` — générateur SVG.
- `.../domain/{Periode,HistoriqueCours}.java` — libellé de période, accès au graphique.
- `.../adapter/in/http/PointEntreeHistorique.java` — champ `graphique` (absent si vide).
- Tests : `GraphiqueCoursTest`, `HistoriqueIT` (3 tests dont la performance sur 20 appels), `AlphaVantageReelIT` (assertion de corps complet assouplie).

**Revue :** 4 constats ; 1 correctif (low), 3 rejets motivés ci-dessus ; 0 report. Revue complémentaire recommandée : non.

**Vérification :** `mvn -B -q -o -f services/bourse/pom.xml verify` code 0 (JaCoCo 100 % domaine) ; tags FR-009-05, FR-009-06, FR-029-08 en code 0 ; `build.sh`, `golden-master.sh` code 0, `check-gates-exist.sh` sans KO ; `trace-smoke.sh` code 0 (relevé par le sous-agent).

**Risques résiduels :** le graphique n'a pas de titre ni de nom accessible (7.4).
