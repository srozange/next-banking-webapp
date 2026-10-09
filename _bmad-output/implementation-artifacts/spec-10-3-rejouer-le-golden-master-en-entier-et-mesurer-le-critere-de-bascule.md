---
title: 'Rejouer le golden master en entier et mesurer le critère de bascule'
type: 'chore'
created: '2026-10-09'
status: 'awaiting-operator'
baseline_revision: '1d9d333486b2385ebf262d60d97ccb1844eb3f4c'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
warnings: ['oversized']
operator_actions:
  - "Trancher le sort des 29 FR du PRD sans test : soit faire livrer les stories optionnelles 3.5 (cumul glissant), 3.6 (reprise après refus), 3.10 (alerte après échecs de connexion), 4.5 (journal des traitements), 4.9 (intérêts mensuels), 4.10 (décompte nocturne), 4.11 (solde faible), 5.4 (décision de prêt notifiée) et 9.6 (journal et catalogue sur l'administration), soit acter l'écart dans le PRD, puis relancer le calcul de la liste des FR sans test."
  - "Faire la revue humaine de la story 9.4 (activer et désactiver un client depuis la gestion des clients, commit 9091676, spec en in-review), prérequis « aucune story en cours » de la story 10.3."
deferred:
  - summary: >-
      29 FR du PRD n'ont aucun test tagué : le critère « liste des FR sans test vide » de la story 10.3 n'est pas atteint.
    evidence: |-
      Recherche des @Tag("FR-…") dans services/*/src/test et des [FR-…] dans frontend/src, comparée aux FR de prd.md : FR-003-28, FR-016-17, FR-016-18, FR-018-01..05, FR-019-01..06, FR-020-01..04, FR-022-01..06, FR-024-09, FR-025-03, FR-025-08, FR-027-11, FR-027-18. Toutes relèvent des stories 3.5, 3.6, 3.10, 4.5, 4.9, 4.10, 4.11, 5.4 et 9.6, marquées optionnelles et non livrées ; antérieur à cette story.
    location: >-
      _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md
    severity: medium
  - summary: >-
      Prérequis « aucune story en cours » non tenu : la story 9.4 est encore en revue.
    evidence: |-
      spec-9-4-activer-et-desactiver-un-client-depuis-la-gestion-des-clients.md porte status in-review ; son code (commit 9091676) est inclus dans le rejeu, qui passe SC-016-01 et SC-016-02.
    location: >-
      _bmad-output/implementation-artifacts/spec-9-4-activer-et-desactiver-un-client-depuis-la-gestion-des-clients.md
    severity: low
---

<intent-contract>

## Intent

**Problem:** Le critère de bascule (NFR-PAR-01 : 31 réussis, 0 en échec, 2 non rejoués avec raison) n'a jamais été mesuré par le rejeu complet ordonné. Le premier lancement de la story 10.2 (spécification `spec-10-2-monter-le-socle-du-rejeu-final-du-golden-master.md`) a laissé SC-004-01 et SC-011-01 en échec. Trois lignes attendues manquent, toutes pour une question de casse : « MONTANT (EUR) », « IBAN / N COMPTE BENEFICIAIRE » et « CATALOGUE DES PRODUITS ».

**Approach:**
- Corriger ces écarts dans l'application, comme le fait le legacy : les libellés des champs texte de `/virements-programmes` passent en capitales par CSS (`.form-group label { text-transform: uppercase }` dans `virement-programme.xhtml`). L'écran `/produits` reçoit, avant son titre, le séparateur `sep-label` « Catalogue des produits » en capitales CSS, comme dans `produits.xhtml`.
- Lancer une seule fois le rejeu complet : `make rejeu-up` puis `make rejeu-golden-master`.
- Faire la contre-épreuve sur SC-016-01, puis restituer le rapport et mesurer la couverture du PRD.

## Boundaries & Constraints

**Always:**
- Corriger un écart dans l'application, les données de référence ou la remise à zéro. Le texte source garde sa casse : les capitales viennent du CSS, selon la règle « Casse rendue » de EXPERIENCE.md.
- Les tests Vitest touchés gardent leurs titres `[FR-xxx-yy]`. La couverture de `src/adapters` reste d'au moins 80 %.
- Les mots de passe se chargent par `set -a; . ./.env; set +a`, sans jamais être affichés.
- Les scénarios se comptent avec `results.json` et les outils de `docs/retro/tools/`, jamais à la main.
- La contre-épreuve est annulée avant le rejeu final. Le dépôt ne garde aucune trace du renommage.

**Never:**
- Modifier `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`, `scripts/gates/**` ou `sprint-status.yaml`.
- Écrire un texte nouveau : « Catalogue des produits » est `TEXTES.produits.catalogue`, texte legacy existant.
- Implémenter ici les stories marquées optionnelles. Ce sont les stories 3.5, 3.6, 3.10, 4.5, 4.8, 4.9, 4.10, 4.11, 5.4 et 9.6 : le cumul glissant, la reprise après refus, l'alerte après échecs de connexion, le journal des traitements, l'échéance suspecte, les intérêts mensuels, le décompte nocturne, le solde faible, la décision de prêt notifiée, et le journal et le catalogue sur l'administration.
- Retirer une FR du PRD pour vider la liste des FR sans test.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Rejeu final | `make rejeu-up`, puis `make rejeu-golden-master` | code 0 ; `results.json` : `stats.unexpected` = 0, `errors` = [], 31 scénarios `@golden-master` réussis, 2 ignorés et annotés « Non rejoué » | — |
| Contre-épreuve | « Gestion des clients » renommé dans `textes.ts`, image `web` reconstruite | code non nul ; SC-016-01 en échec, avec le texte attendu « Gestion des clients » et le texte trouvé | renommage annulé, image reconstruite, rejeu complet relancé |
| FR sans test | PRD actuel, stories optionnelles non livrées | liste mesurée et consignée dans l'Auto Run Result | décision humaine : livrer les stories ou acter l'écart (operator_actions) |

</intent-contract>

## Code Map

- `frontend/src/adapters/ecrans/virementsProgrammes/EcranVirementsProgrammes.css` -- capitales déjà posées sur `__separateur` et `th`. Les `ChampTexte` (`.champ-texte__libelle`) ne sont pas en capitales, alors que les `ChampListe` le sont, par `ChampListe.css`.
- `frontend/src/adapters/ui/ChampTexte.css` -- composant partagé (connexion, prêts…). Ne pas le passer en capitales globalement ; régler la casse au niveau de la page.
- `frontend/src/adapters/ecrans/produits/EcranProduits.tsx:107-117` -- `SectionCatalogue` : le titre `h2` « Catalogue des produits » n'a pas de séparateur. `SectionTraitements:241` sert de modèle (`<p className="ecran-produits__separateur">`).
- `frontend/src/adapters/ecrans/produits/EcranProduits.css` -- `.ecran-produits__separateur` déjà en capitales.
- `frontend/src/adapters/ecrans/produits/EcranProduits.test.tsx:83-91` -- test d'accès `[FR-010-01][FR-011-01]` ; à compléter par le séparateur.
- `frontend/src/adapters/textes.ts:173` -- `titre: 'Gestion des clients'` : cible de la contre-épreuve, renommage temporaire.
- `scripts/rejeu-golden-master.sh`, `Makefile` (`rejeu-up`, `reset-data`, `rejeu-golden-master`) -- livrés par la story 10.2, à lancer sans les modifier, sauf défaut avéré.
- `e2e/golden-master/results.json`, `report/` -- produits par la fusion, ignorés par git. Les 2 `test.skip` annotés « Non rejoué » sont SC-003-03 (BUG-011) et SC-010-02 (BUG-038).
- `docs/retro/tools/check-prd-coverage.py` -- déjà en code 0, couverture des règles et des bugs à 1.0.
- FR sans test (`@Tag("FR-…")` dans `services/*/src/test` ou `[FR-…]` dans `frontend/src`, sur les FR de `prd.md`) : 29 FR, toutes portées par les stories optionnelles non livrées. FR-003-28 (3.6) ; FR-027-18 (3.5) ; FR-027-11 (3.10) ; FR-020-01..04 (4.5) ; FR-018-01..05 (4.9) ; FR-019-01..06 (4.10) ; FR-022-01..06 et FR-025-08 (4.11) ; FR-025-03 et FR-024-09 (5.4) ; FR-016-17 et FR-016-18 (9.6).
- `spec-9-4-activer-et-desactiver-un-client-depuis-la-gestion-des-clients.md` -- `status: in-review`, revue humaine attendue (commit `9091676`).

## Tasks & Acceptance

**Execution:**
- `frontend/src/adapters/ecrans/virementsProgrammes/EcranVirementsProgrammes.css` -- ajouter `.ecran-virements-programmes .champ-texte__libelle { text-transform: uppercase; }`, avec un commentaire qui renvoie au legacy -- SC-004-01.
- `frontend/src/adapters/ecrans/produits/EcranProduits.tsx` -- en tête de `SectionCatalogue`, `<p className="ecran-produits__separateur">{T.catalogue}</p>` -- SC-011-01.
- `frontend/src/adapters/ecrans/produits/EcranProduits.test.tsx` -- dans le test d'accès, vérifier que le séparateur « Catalogue des produits » précède le titre dans `produits-catalogue`.
- Rejeu : `make rejeu-up` (reconstruit `web`), puis `set -a; . ./.env; set +a; make rejeu-golden-master`. Lire `results.json` avec `jq`.
- Contre-épreuve : renommer temporairement `textes.ts:173`, puis `make rejeu-up` et `make rejeu-golden-master` (échec attendu sur SC-016-01). Faire ensuite `git checkout -- frontend/src/adapters/textes.ts`, puis `make rejeu-up` et le rejeu complet final.
- Auto Run Result : compteurs du rapport final ; écarts HTTP observés ; résultat de la contre-épreuve ; couverture du PRD ; liste des FR sans test, avec la story optionnelle qui porte chacune.

**Acceptance Criteria:**
- Given le PRD et le corpus, when `uv run docs/retro/tools/check-prd-coverage.py --prd …/prd.md --retro docs/retro --addendum …/addendum.md` est lancé, then le code de sortie est 0 et la couverture des règles vaut 1.0 ; and la liste des FR sans test est calculée et consignée. Elle n'est vide que si les stories optionnelles sont livrées ou l'écart acté par l'humain (operator_actions).
- Given `make rejeu-up`, when `make rejeu-golden-master` est lancé, then le code de sortie est 0 ; and `results.json` indique `stats.unexpected` = 0 et un tableau `errors` vide ; and 31 tests `@golden-master` sont réussis et 2 sont ignorés, annotés « Non rejoué » (SC-003-03 BUG-011, SC-010-02 BUG-038).
- Given le rapport final, when on lit les 5 scénarios HTTP, then SC-001-01, SC-001-02, SC-001-03, SC-014-01 et SC-017-01 sont réussis, avec les statuts attendus des écarts voulus (200, 401, 401 et message, 302, 401).
- Given la contre-épreuve, when `make rejeu-golden-master` est lancé, then le code de sortie est non nul et le rapport désigne SC-016-01 en échec, avec le texte attendu et le texte trouvé ; and une fois le renommage annulé, le rejeu complet redonne 31/0/2.
- Given le rejeu terminé, when `git diff --exit-code -- e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est lancé, then le code de sortie est 0.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh` et `golden-master.sh` sont lancés, then chacun sort en code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 8 findings — high 0, medium 1, low 3, false 4, maybe-false 0
- findings:
  - `[false]` `[reject]` La preuve 31/0/2 n'existe qu'en récit, `results.json` étant ignoré par git — relu à neuf par l'orchestrateur : `results.json` du rejeu final indique `stats.unexpected` = 0, `errors` = [], 31 tests `@golden-master` réussis, 2 `@non-rejoue` ignorés et annotés « Non rejoué » (BUG-011, BUG-038).
  - `[low]` `[reject]` La capitalisation CSS des libellés de `/virements-programmes` n'a pas de test Vitest — jsdom n'applique pas `text-transform` ; la preuve observable est le rejeu de SC-004-01, vert ; un test unitaire ajouterait un faux contrôle.
  - `[low]` `[reject]` Le test de `/produits` vérifie le séparateur en casse source, pas en capitales — même raison : la capitale vient de la règle CSS existante `.ecran-produits__separateur`, prouvée par SC-011-01 vert au rejeu.
  - `[medium]` `[defer]` Le critère « liste des FR sans test vide » est reformulé au lieu d'être atteint — réel et antérieur : les 29 FR relèvent de 9 stories optionnelles non livrées ; les livrer ou retirer les FR est une décision de produit. Reporté (`deferred`) et inscrit dans `operator_actions`.
  - `[false]` `[reject]` Frontmatter non finalisé (`in-review`, pas de clé `operator_actions`) — instantané pris pendant la revue ; la finalisation pose `status: awaiting-operator` et la liste YAML `operator_actions`.
  - `[low]` `[patch]` `operator_actions` contenait une action faisable par un agent (lecture du rapport par le skill `corpus-bmad-golden-master`) — lecture faite par l'orchestrateur selon l'étape 6 du skill (compteurs, écarts, raisons des non-rejoués vérifiées) et retirée des actions humaines.
  - `[low]` `[defer]` Prérequis « aucune story en cours » non tenu (story 9.4 en revue) — antérieur ; le code de la 9.4 est dans le rejeu, SC-016-01 et SC-016-02 passent ; reporté et inscrit dans `operator_actions`.
  - `[false]` `[reject]` Modifications de `epic-10-context.md` hors périmètre — fichier de contexte recompilé par l'étape 1 du flux, car `golden-master-architecture.md` était plus récent ; aucune règle ne l'interdit.

## Design Notes

Le rapport fusionné contient aussi les tests de connexion (projet `connexion`, 3 par lancement, soit 6 lancements). Pour compter, on filtre les tests dont les `tags` contiennent `golden-master` (31) ou `non-rejoue` (2). Exemple : `jq '[.. | objects | select(has("tags") and (.tags | index("golden-master"))) | .tests[].results[-1].status]' results.json`. La structure exacte se vérifie sur le fichier.

La liste des FR sans test ne peut pas être vidée dans cette story. Les 29 FR relèvent de stories du plan que l'orchestrateur a marquées optionnelles et qui ne sont pas livrées. Les implémenter ici dépasserait la story de très loin. Les retirer du PRD serait une décision de produit. La story est donc menée jusqu'au bout pour tout le reste, puis finalisée en `awaiting-operator` avec la décision attendue.

## Verification

**Commands:**
- `cd frontend && npx vitest run src/adapters/ecrans/produits src/adapters/ecrans/virementsProgrammes` -- expected: code 0
- `make rejeu-up && set -a && . ./.env && set +a && make rejeu-golden-master` -- expected: code 0, 31/0/2
- `git diff --exit-code -- e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/trace-smoke.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

**Corrections (application seule) :**
- `EcranVirementsProgrammes.css` : `.ecran-virements-programmes .champ-texte__libelle { text-transform: uppercase; }` (renvoi à `virement-programme.xhtml`) -- SC-004-01 (« MONTANT (EUR) », « IBAN / N COMPTE BENEFICIAIRE »).
- `EcranProduits.tsx` : séparateur `<p className="ecran-produits__separateur">{T.catalogue}</p>` en tête de `SectionCatalogue` -- SC-011-01 (« CATALOGUE DES PRODUITS »). Aucun texte nouveau.
- `EcranProduits.test.tsx` : le test `[FR-010-01][FR-011-01]` vérifie que le séparateur « Catalogue des produits » est le premier enfant de `produits-catalogue` et précède le titre. Vitest produits + virementsProgrammes : 66/66.

**Rejeu complet final** (`make rejeu-up`, puis `make rejeu-golden-master`) : code 0. `results.json` : `stats` expected 49 (31 `@golden-master` + 18 connexions sur 6 lancements), skipped 2, unexpected 0, flaky 0 ; `errors` = []. Filtre par tags : 31 `golden-master` réussis ; 2 `non-rejoue` ignorés, annotés « Non rejoué » : SC-003-03 (BUG-011) et SC-010-02 (BUG-038). Le premier rejeu, avant contre-épreuve, donnait déjà 31/0/2.

**Écarts HTTP observés (tous réussis) :** SC-001-01 200 sur `/login` ; SC-001-02 401 ; SC-001-03 401 et `{"message":"Identifiants incorrects"}` ; SC-014-01 302 ; SC-017-01 401.

**Contre-épreuve :** `textes.ts:173` renommé en « Gestion des usagers », image `web` reconstruite, rejeu : code non nul (make 2, lancement `lectures` nommé en échec), `unexpected` = 1, seul SC-016-01 en échec : attendu « Gestion des clients », trouvé « Gestion des usagers ». Renommage annulé par `git checkout`, `make rejeu-up`, rejeu complet : 31/0/2, code 0.

**Golden master intact :** `git diff --exit-code -- e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` : code 0.

**Contrôles :** `build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh` : code 0.

**Couverture du PRD :** `check-prd-coverage.py` : code 0, `blocking_gaps` = [], couverture règles 1.0, bugs 1.0.

**FR sans test (29), toutes portées par des stories optionnelles non livrées :**
- FR-003-28 : story 3.6 (reprise après refus)
- FR-027-18 : story 3.5 (cumul glissant)
- FR-027-11 : story 3.10 (alerte après échecs de connexion)
- FR-020-01..04 : story 4.5 (journal des traitements)
- FR-018-01..05 : story 4.9 (intérêts mensuels)
- FR-019-01..06 : story 4.10 (décompte nocturne)
- FR-022-01..06, FR-025-08 : story 4.11 (solde faible)
- FR-025-03, FR-024-09 : story 5.4 (décision de prêt notifiée)
- FR-016-17, FR-016-18 : story 9.6 (journal et catalogue sur l'administration)

**Lecture du rapport (étape 6 du skill `corpus-bmad-golden-master`) :** 31 réussis, 0 en échec, 2 non rejoués avec raison acceptée : SC-003-03 (BUG-011, couvert par les tests FR-003-06 du cœur bancaire, dont `ExecuterVirementCasUsageTest`, `VirementIT`) et SC-010-02 (BUG-038, couvert par les tests FR-010-09 d'open-banking-conseiller, dont `DialoguerCasUsageTest`, `AzureOpenAiSimuleTest`). Aucun écart à attribuer (mapping, application ou données). Critère de bascule NFR-PAR-01 atteint pour le rejeu.

**Revue :** 8 constats — 1 correctif (lecture du rapport faite et retirée des actions humaines, `low`), 2 reportés (FR sans test `medium`, story 9.4 en revue `low`), 5 rejetés (4 faux, 2 faibles non testables en jsdom — voir Review Triage Log).

**Recommandation de revue complémentaire :** `false` (correctifs : 0 high, 0 medium, 1 low).

**Vérifications refaites par l'orchestrateur :** Vitest produits + virements programmés 66/66 ; `scripts/gates/build.sh` 0 ; `check-gates-exist.sh` 0 ; `golden-master.sh` 0 ; `trace-smoke.sh` 0 ; `git diff --exit-code` sur le golden master 0 ; `results.json` relu par `jq`.

**Risques résiduels :** le critère « FR sans test » de la story reste non atteint tant que l'humain n'a pas tranché ; `trace-smoke.sh` a pu relancer la pile hors profil `rejeu` : refaire `make rejeu-up` avant tout nouveau rejeu.

Status: awaiting-operator

**operator_actions :**
- Trancher le sort des 29 FR du PRD sans test : livrer les stories optionnelles 3.5, 3.6, 3.10, 4.5, 4.9, 4.10, 4.11, 5.4 et 9.6, ou acter l'écart dans le PRD.
- Faire la revue humaine de la story 9.4 (activer et désactiver un client depuis la gestion des clients).
