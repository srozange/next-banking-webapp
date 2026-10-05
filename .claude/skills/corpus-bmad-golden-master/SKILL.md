---
name: corpus-bmad-golden-master
description: Aide à prendre les décisions de dérivation du golden master du legacy (replay-mapping.json), à le dériver, à générer les tests du runner, puis à lire le rejeu sur la nouvelle application. Utiliser avant de finaliser l'architecture avec bmad-architecture, puis à chaque rejeu, quand l'utilisateur dit « dérive le golden master », « remplis le mapping », « où en est le golden master ? », « rejoue le golden master ».
compatibility: Claude Code ou GitHub Copilot, dans le repo de la nouvelle application ; uv et Python ≥ 3.11 ; Node et @playwright/test pour le rejeu par le runner. Lit le corpus exporté par retro-forward-bmad.
metadata:
  version: "1.0"
  generated-by: retro-forward-bmad
---

# corpus-bmad-golden-master

Tu aides l'utilisateur à rendre le golden master du legacy rejouable sur la nouvelle application : une décision par scénario dans `replay-mapping.json`, la dérivation, les tests du runner, puis la lecture du rejeu. Tu donnes des mesures produites par les scripts, jamais une estimation.

Tu écris **uniquement** `replay-mapping.json` (racine du repo), et seulement ce que l'utilisateur a confirmé. Le dossier dérivé et les tests générés sont produits par les scripts, jamais retouchés à la main. Tu ne modifies jamais les scénarios capturés (`docs/retro/golden-master/scenarios/`), le contrat, ni le document d'architecture.

## Entrées

- Corpus exporté : `docs/retro/` (`golden-master/`, `golden-master-contract.md`, `00-global/legacy-bugs.json`, scripts dans `tools/`).
- Décisions de l'équipe : `replay-mapping.json` ; son format fait foi dans son champ `_doc` — le suivre, ne pas inventer de champ.
- Document d'architecture, s'il existe : pour proposer routes et sélecteurs. Une proposition tirée de ce document reste une proposition.

## Étape 0 — Où en est-on

S'arrêter au premier cas vrai et l'annoncer avec sa prochaine action :

1. `docs/retro/golden-master/scenarios/` absent : corpus non exporté — relancer retro-forward-bmad. Stop.
2. `replay-mapping.json` absent : proposer le squelette, puis étape 1.
   ```bash
   uv run docs/retro/tools/derive-golden-master.py skeleton --gm-dir docs/retro/golden-master --output replay-mapping.json
   ```
3. `docs/retro/golden-master/derived/` absent, ou plus ancien que `replay-mapping.json` : étape 1.
4. Sinon : golden master dérivé — étape 6 si l'application cible tourne.

## Étape 1 — Mesure (script, jamais estimée)

Dérivation à blanc, dans un dossier temporaire : la même commande que le relecteur de finalisation de l'architecture.

```bash
uv run docs/retro/tools/derive-golden-master.py derive --gm-dir docs/retro/golden-master \
  --mapping replay-mapping.json --output-dir "$(mktemp -d)"
```

Le JSON fait foi : ne recompte rien.

- exit 0 : mapping complet et valide — étape 5 ;
- exit 1 : `errors` liste ce qui reste (`id`, `field`, `message`) ; sans `id` = décision globale ;
- exit 2 : bloquant (registre des bugs absent ou non tranché, fichier illisible) — afficher le message et s'arrêter : cela se règle côté rétro.

## Étape 2 — Restitution

En tête : nombre de scénarios, d'erreurs, de scénarios exclus d'office.

1. **Décisions globales** d'abord (erreurs sans `id`, valeurs `[ASK ARCHITECT]` en tête du fichier) : URL de base, authentification, table des routes `routes` (page legacy → route cible), bloc `browser` (dossier des sessions, comptes par rôle).
2. **Scénarios à décider**, par fonctionnalité. Lire le bloc `legacy` de l'entrée (prérempli par le squelette, c'est ce que le script validera) : description, classe, rôle, point d'entrée legacy (requête, ou page de départ et sélecteurs), ce qui est comparé (réponse, ou lignes attendues), règles couvertes (`rg_ids` du scénario).
3. **Exclus d'office** (bugs « corriger » ou « ne pas corriger ») : pour mémoire, avec leur motif ; le script refuse toute autre décision.

## Étape 3 — Choix, point par point

Pour chaque scénario, l'une des trois décisions :

- **garder** (`keep`) — point d'entrée et réponse inchangés (scénarios HTTP seulement) ;
- **faire correspondre** (`map`) — route ou requête cible et sortie qui fait foi ; tu peux proposer une correspondance lue dans le document d'architecture, présentée comme « proposition — à confirmer » ;
- **supprimer** (`drop`) — avec sa raison, en clair ; rappeler les règles qui ne seront plus couvertes (`rules.lost_by_scenario` du JSON de mesure).

Scénarios d'écran, dans cet ordre (il réduit le travail) : la table des routes (une route sert tous les scénarios de la même page), puis les scénarios sans action (seul `map` reste à poser), puis ceux avec actions, un sélecteur à la fois (`target.selector_map`, `null` = action retirée, avec une raison). `expected.text_map` seulement quand un libellé change.

Motifs de suppression admis : bug tranché à la rétro, fonctionnalité abandonnée ou hors périmètre par décision de l'équipe. « C'est plus simple » n'en est pas un : demander une raison de périmètre. « L'outil ne sait pas le dériver » non plus : si le script refuse toute décision autre que la suppression (par exemple « input-request.json manquant » pour un scénario d'écran), dire que cette version de l'outil ne dérive pas ce type de scénario et le laisser non décidé.

Ne décide jamais à la place de l'utilisateur. Ne supprime jamais un scénario pour obtenir un exit 0.

## Étape 4 — Écriture

Écrire dans `replay-mapping.json` les décisions confirmées, et elles seules : une entrée à la fois, sans toucher aux autres ; retirer `"proposed": true` seulement sur confirmation ; ne jamais modifier une entrée de classe `legacy_bug` ; laisser `[ASK ARCHITECT]` sur ce qui n'est pas décidé. Puis revenir à l'étape 1.

Si l'écriture est refusée par le hook de protection des contrôles (skill pilote installé) : le dire, sans contourner — le mapping est alors protégé, sa modification passe par le déverrouillage réservé à l'utilisateur.

## Étape 5 — Verdict, dérivation, tests du runner

Mesure en exit 0 : annoncer `derived`, `portable`, `dropped` (chaque raison) et `rules` (`kept`, `lost`). Sur accord, dériver pour de bon :

```bash
uv run docs/retro/tools/derive-golden-master.py derive --gm-dir docs/retro/golden-master \
  --mapping replay-mapping.json --output-dir docs/retro/golden-master/derived --force
```

Puis générer les tests du runner (dossier `e2e/golden-master/` par défaut, à confirmer au premier lancement) et annoncer `tests` et `skipped` (scénarios non rejoués) :

```bash
uv run docs/retro/tools/export-golden-master-tests.py --runner playwright \
  --derived docs/retro/golden-master/derived --mapping replay-mapping.json \
  --gm-dir docs/retro/golden-master --output e2e/golden-master
```

Les fichiers générés ne s'éditent pas : on corrige le mapping, on redérive, on régénère (`--check` signale un fichier modifié à la main ou périmé).

Remettre enfin **un bloc** à donner à bmad-architecture, pour que le document d'architecture porte les décisions globales que son relecteur y cherche : URL de base et authentification de la cible, table des routes, sessions de test par rôle, sortie qui fait foi, runner de test, données de référence et remise à zéro avant rejeu, critère de bascule.

## Étape 6 — Rejeu sur la cible

Seulement quand l'application cible tourne. Vérifier d'abord, et lister ce qui manque sans rien lancer : application joignable à l'URL de base, variable d'environnement du mot de passe de chaque rôle (jamais de mot de passe dans un fichier du repo), données de référence chargées, base remise à l'état de référence.

Avec le runner (premier choix), depuis `e2e/golden-master/` :

```bash
npx playwright test                         # rejeu complet, à la fin du développement ; rapport : report/index.html
npx playwright test --grep "<SC-NNN-NN>"   # quelques scénarios, pour diagnostiquer un écart
```

Sans runner, rejeu de repli. Préparer d'abord la session de chaque rôle :

```bash
uv run docs/retro/tools/login-state.py --project-root . --base-url <page de connexion de la cible> \
  --accounts '<accounts de adapters.playwright du replay-config dérivé, en JSON>' \
  --output-dir <storage_state_dir du replay-config dérivé> --force
```

Le dossier des sessions doit être ignoré par git (sinon le script refuse d'écrire). Puis `uv run docs/retro/tools/replay-golden-master.py --scenarios docs/retro/golden-master/derived --config docs/retro/golden-master/derived/replay-config.json --target new-app`, avec `--output docs/retro/golden-master/results-new-app.json` pour un rejeu complet, ou `--only <SC-NNN-NN>` (répétable) pour diagnostiquer quelques scénarios.

Restituer les compteurs (réussis, en échec, non rejoués) et, pour chaque écart, le texte attendu et le texte trouvé. Aider à trouver la cause, sans trancher à la place de l'utilisateur :

- **le mapping est faux** : corriger `replay-mapping.json` (étape 3), redériver, régénérer ;
- **l'application diverge du legacy** : c'est à l'application d'être corrigée — hors de ce skill ;
- **les données diffèrent** : corriger le chargement ou la remise à zéro.

Ne jamais modifier un test généré ni une sortie attendue du dossier dérivé pour faire passer un scénario. Le critère de bascule est atteint quand le rejeu complet ne laisse ni échec ni scénario non rejoué sans raison acceptée.

## Hors périmètre

Conception de l'architecture, écriture des stories, correction de l'application, tests des fonctionnalités que le golden master ne couvre pas : ce skill s'arrête au mapping, à la dérivation, aux tests générés et à la lecture du rejeu.
