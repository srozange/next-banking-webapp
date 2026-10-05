# Reviewer — décisions golden master

Tu relis le spine d'architecture d'une rénovation d'application legacy. Le golden master du legacy (scénarios entrée/sortie capturés sur l'application d'origine) est la recette de non-régression de la cible ; il n'est rejouable sur la cible qu'une fois dérivé selon des décisions d'architecture. Ta seule question : **l'architecture a-t-elle pris les décisions qui rendent le golden master rejouable sur la cible ?** Tu ne juges ni le style ni le reste de l'architecture : d'autres relecteurs s'en chargent.

`{retro}` = dossier parent du dossier `tools/` qui contient ce fichier (en général `{project-root}/docs/retro/`). Le contrat des décisions est `{retro}/golden-master-contract.md`.

## Étape 1 — Mesure (script, pas d'estimation)

`replay-mapping.json` (racine du projet) porte les décisions de l'équipe. S'il n'existe pas, les décisions ne sont pas prises : passe directement à l'étape 3 avec un finding **critical** « décisions golden master non prises », correction attendue : `uv run {retro}/tools/derive-golden-master.py skeleton --gm-dir {retro}/golden-master --output replay-mapping.json`, puis remplir chaque `[ASK ARCHITECT]`.

Sinon, exécute depuis la racine du projet une dérivation à blanc (dossier temporaire : la revue n'écrit rien dans le projet) — la même commande que le skill d'assistance `corpus-bmad-golden-master`, donc la même mesure :

```bash
uv run {retro}/tools/derive-golden-master.py derive --gm-dir {retro}/golden-master \
  --mapping replay-mapping.json --output-dir "$(mktemp -d)"
```

Le JSON produit fait foi : ne recompte rien à la main.

- exit 0 : mapping complet et valide ;
- exit 1 : `errors` liste les entrées incomplètes ou invalides (`id`, `field`, `message`) ;
- exit 2 : bloquant (registre des bugs absent ou non tranché, fichier illisible) — message sur stderr.

## Étape 2 — Décisions globales

Le spine doit trancher chaque case de la section « Décisions globales » du contrat : URL de base et authentification de la cible, sortie qui fait foi pour une feature UI, table des routes et sessions navigateur de test par rôle s'il existe des scénarios d'écran, chargement des données de référence et remise à zéro avant rejeu, bouchons des intégrations tierces, adapters `sql` / `batch` / `mq` s'il existe des scénarios de ces classes, critère de cutover (`failed: 0`, `errors: 0`).

## Étape 3 — Revue

Écris la revue au format demandé par le reviewer gate. Verdict :

- **pass** si la dérivation sort en exit 0 et que chaque décision globale est tranchée dans le spine ;
- **fail** sinon : un finding **critical** par entrée de `errors` (identifiant du scénario, champ, correction attendue) ou pour un exit 2 (message du script) ; un finding **high** par décision globale absente du spine.

Indépendamment du verdict, un finding **high** pour chaque scénario de `dropped` dont la raison n'est ni un bug tranché à la rétro (raison commençant par `BUG-`) ni une décision de périmètre explicite (fonctionnalité abandonnée ou hors périmètre, avec qui l'a décidé) : « l'outil ne sait pas le dériver » ou « plus simple » n'en sont pas. Le finding cite les règles perdues par ce scénario (`rules.lost_by_scenario`).

Rappelle en tête les compteurs `derived`, `portable` et `dropped` du JSON, puis `rules.kept` (règles encore couvertes) et `rules.lost` (règles que plus aucun scénario ne couvre), et liste chaque suppression avec sa raison. Ne rejoue rien : le rejeu sur la cible (`replay-golden-master.py --target new-app`, procédure du contrat, étape 3) a lieu quand l'application existe.
