---
name: corpus-bmad-prd-coverage
description: Vérifie, avant de finaliser le PRD avec bmad-prd, que chaque règle confirmée et chaque bug à corriger du corpus de rétro-ingénierie est repris dans le PRD, ou écarté volontairement avec une raison. Utiliser quand l'utilisateur dit « vérifie la couverture du PRD », « avant de finaliser le PRD », « le PRD reprend-il tout le corpus ? ».
compatibility: Claude Code ou GitHub Copilot, dans le repo de la nouvelle application ; python3 (stdlib). Lit le corpus exporté par retro-forward-bmad.
metadata:
  version: "1.0"
  generated-by: retro-forward-bmad
---

# corpus-bmad-prd-coverage

Tu aides l'utilisateur à vérifier, **avant de finaliser le PRD avec bmad-prd**, que le PRD reprend tout le corpus de rétro-ingénierie de l'application existante, ou que ce qu'il ne reprend pas a été écarté **sciemment**, avec une raison écrite. Tu donnes l'une de ces deux garanties, jamais une estimation.

Tu ne modifies **jamais** `prd.md` ni aucun fichier du run de bmad-prd : c'est bmad-prd qui rédige et journalise dans son memlog. Tu remets des consignes à lui donner.

## Entrées

- Corpus exporté : `docs/retro/` (contient `rules-index.json`, `features/FEAT-NNN/RG.md`, `GAPS.md`, `tools/check-prd-coverage.py`).
- PRD en cours : sous `_bmad-output/planning-artifacts/prds/`, le dossier de run le plus récent dont `prd.md` n'est pas en `status: final`. Aucun, ou plusieurs du même jour : demander le chemin à l'utilisateur.
- Addendum : `addendum.md` à côté de `prd.md`, s'il existe.

## Étape 1 — Mesure (script, jamais estimée)

Depuis la racine du repo :

```bash
python3 docs/retro/tools/check-prd-coverage.py --prd <run>/prd.md --retro docs/retro
```

Le JSON fait foi : ne recompte rien, n'ajoute aucun identifiant qu'il ne cite pas. Exit 2 : afficher `error` (ou le message sur stderr) et s'arrêter — la couverture n'est pas mesurée.

## Étape 2 — Restitution, par fonctionnalité

Présenter en clair, regroupé par fonctionnalité (`rules.missing[].feature`) :

1. **Règles confirmées non reprises** (`rules.missing`) : pour chacune, son énoncé lu dans `docs/retro/features/FEAT-NNN/RG.md`, reformulé en une phrase. Si une FR du PRD décrit déjà ce comportement (souvent une règle voisine d'une autre fonctionnalité fusionnée en une seule FR), l'écrire : « probablement couverte par FR-xxx — à confirmer ». C'est une suggestion : l'utilisateur confirme.
2. **Bugs à corriger sans FR** (`bugs.missing`) : le bug et le comportement attendu lus dans `docs/retro/00-global/LEGACY-BUGS.md`.
3. **Bugs conservés non documentés** (`bugs_kept.missing`) : un renvoi du PRD à `docs/retro/00-global/LEGACY-BUGS.md` suffit.
4. **Manques bloquants du corpus** (`blocking_gaps`) : le fichier et le texte du manque ; chercher dans le PRD la FR ou l'Open Question tranchée qui y répond, et la citer, ou dire qu'aucune ne le fait.
5. **FR interdites** (`forbidden_as_fr`) : la FR, les identifiants et la raison.
6. Pour mémoire : **décisions de séance** (`promoted_suspect`, règles suspectes reprises volontairement) et **retraits déjà tracés** (`removed_in_prd`).

Rappeler en tête les taux `coverage.rules` et `coverage.bugs`. Rien à présenter : passer à l'étape 5.

## Étape 3 — Choix, point par point

Pour chaque règle ou bug manquant, demander à l'utilisateur :

- **à reprendre** — ajouter l'identifiant dans une FR existante (celle suggérée, si l'utilisateur la confirme), ou une nouvelle FR ;
- **écarté volontairement** — avec sa raison, en clair.

Pour chaque manque bloquant : « déjà traité par FR-xxx / l'Open Question … » ou « à reprendre » (FR ou Open Question tranchée). Pour chaque FR interdite : la retirer, la réécrire, ou la marquer comme décision de séance (`[ÉVOLUTION — raison]`) si la règle suspecte est reprise volontairement.

Ne décide jamais à la place de l'utilisateur.

## Étape 4 — Consignes pour bmad-prd

Remettre **un seul bloc** à copier dans bmad-prd (intention Update / Edit du PRD en cours), une ligne par point :

- `Ajouter RG-NNN-NNN sur la ligne Source de FR-xxx (même comportement : …).`
- `Nouvelle FR dans la section de FEAT-NNN : <comportement>, Source : RG-NNN-NNN.`
- `Nouvelle FR pour BUG-NNN : <comportement attendu>, Source : BUG-NNN.`
- `Section Non-Goals : « RG-NNN-NNN — <raison> ».` (retrait volontaire : c'est cette ligne que le contrôle reconnaît)
- `Ajouter un renvoi à docs/retro/00-global/LEGACY-BUGS.md pour les bugs conservés.`
- `Manque bloquant « <texte> » : FR ou Open Question tranchée — <réponse de l'utilisateur>.`

Dire à l'utilisateur de lancer bmad-prd avec ce bloc, puis de revenir ici pour la nouvelle mesure.

## Étape 5 — Nouvelle mesure et verdict

Après le passage de bmad-prd, relancer la commande de l'étape 1, puis conclure par l'un des deux :

- **« Tout le corpus est repris ou écarté sciemment »** — si l'exit est 0 et que chaque manque bloquant a une réponse confirmée par l'utilisateur. Le PRD peut être finalisé avec bmad-prd.
- **Ce qui reste** — la liste des points encore ouverts (identifiant, fonctionnalité, choix attendu), à reprendre à l'étape 3.

## Hors périmètre

Epics, stories, tests, architecture : ce skill s'arrête au PRD.
