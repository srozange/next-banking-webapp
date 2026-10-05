# Contrat de dérivation du golden master

Document généré par `derive-golden-master.py contract` — ne pas éditer à la main.

Scénarios : 33 — dom_text 26, legacy_bug 2, ui_html 3, unknown 2

Registre des bugs legacy : `00-global/legacy-bugs.json`

## Décisions par scénario

| Scénario | Feature | Classe | Décision attendue | Qui/quand | Décision actuelle |
|---|---|---|---|---|---|
| SC-001-01 | FEAT-001 | unknown | Réponse HTTP sans type de contenu capturé : map (status+json_subset) ou drop | bmad-architecture | [ASK ARCHITECT] |
| SC-001-02 | FEAT-001 | ui_html | Point d'entrée cible et sortie qui fait foi : map (status+json_subset) ou drop | bmad-architecture | [ASK ARCHITECT] |
| SC-001-03 | FEAT-001 | unknown | Réponse HTTP sans type de contenu capturé : map (status+json_subset) ou drop | bmad-architecture | [ASK ARCHITECT] |
| SC-001-04 | FEAT-001 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-001-05 | FEAT-001 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-002-01 | FEAT-002 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-002-02 | FEAT-002 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-003-01 | FEAT-003 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-003-02 | FEAT-003 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-003-04 | FEAT-003 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-004-01 | FEAT-004 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-004-02 | FEAT-004 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-004-03 | FEAT-004 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-005-01 | FEAT-005 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-006-01 | FEAT-006 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-007-01 | FEAT-007 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-007-02 | FEAT-007 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-008-01 | FEAT-008 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-009-01 | FEAT-009 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-010-01 | FEAT-010 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-011-01 | FEAT-011 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-012-01 | FEAT-012 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-013-01 | FEAT-013 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-013-02 | FEAT-013 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-014-01 | FEAT-014 | ui_html | Point d'entrée cible et sortie qui fait foi : map (status+json_subset) ou drop | bmad-architecture | [ASK ARCHITECT] |
| SC-015-01 | FEAT-015 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-015-02 | FEAT-015 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-016-01 | FEAT-016 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-016-02 | FEAT-016 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |
| SC-017-01 | FEAT-017 | ui_html | Point d'entrée cible et sortie qui fait foi : map (status+json_subset) ou drop | bmad-architecture | [ASK ARCHITECT] |
| SC-017-02 | FEAT-017 | dom_text | Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié | bmad-architecture | [ASK ARCHITECT] |

## Scénarios exclus du golden master (pour information)

Bugs legacy tranchés à la gate retro-verify : « corriger » — le comportement legacy n'est pas rejoué, la correction est validée par les tests de la nouvelle application ; « ne pas corriger » — bug connu toléré, ni exigence ni test : on ne vérifie pas qu'il est toujours là. « faux positif » reste dans le golden master.

| Scénario | Feature | Bug(s) | Motif |
|---|---|---|---|
| SC-003-03 | FEAT-003 | BUG-011 | corrigé dans le forward, validé par les tests de la nouvelle application |
| SC-010-02 | FEAT-010 | BUG-038 | corrigé dans le forward, validé par les tests de la nouvelle application |

## Décisions globales

- [ ] URL de base et mode d'authentification de la cible (`target_base_url`, `auth` de replay-mapping.json)
- [ ] Sortie qui fait foi pour une feature UI : état base (db_state), extrait de réponse JSON (status+json_subset) ou texte affiché (scénario d'écran)
- [ ] Table des routes : page de l'application existante vers route cible (`routes` de replay-mapping.json)
- [ ] Sessions navigateur de test par rôle : connexion par formulaire, comptes et dossier des sessions (`browser` de replay-mapping.json)
- [ ] Données de référence non produites par la rétro (pas de `00-global/reference-data.json`) : définir le jeu de données de la cible et son chargement avant tout rejeu
- [ ] Remise à zéro de la base cible avant un rejeu complet
- [ ] Bouchons des intégrations tierces (messagerie, paiement, services externes) pendant le rejeu
- [ ] Adapters `sql`, `batch`, `mq` du rejeu (stubs) à implémenter selon la stack cible
- [ ] Critère de cutover : le golden master dérivé rejoue vert (`failed: 0`, `errors: 0`) sur la cible

## Procédure

0. (Optionnel) Revoir la classification sans rien écrire : `uv run docs/retro/tools/derive-golden-master.py classify --gm-dir <golden-master>`.
1. Écrire le squelette de décisions, puis le remplir (c'est lui, `replay-mapping.json`, qui porte les décisions de l'équipe — jamais réécrasé) : `uv run docs/retro/tools/derive-golden-master.py skeleton --gm-dir <golden-master> --output replay-mapping.json`
2. `uv run docs/retro/tools/derive-golden-master.py derive --gm-dir <golden-master> --mapping replay-mapping.json --output-dir <golden-master>/derived`
3. `uv run docs/retro/tools/replay-golden-master.py --scenarios <golden-master>/derived --config <golden-master>/derived/replay-config.json --target new-app`
