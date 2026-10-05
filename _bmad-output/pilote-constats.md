# Constats du pilote BMAD

Journal tenu au fil des appels de `/pilote-bmad` sur ce dépôt. Deux usages :

1. garder la trace des écarts entre le skill `pilote-bmad`, ses fiches et le dépôt réel ;
2. repérer ce qui pourrait passer du pilote vers les skills retro (`retro-forward-bmad`, `corpus-bmad-*`), sans surcharger ces skills.

Chaque constat : date, étape, ce qui a été observé, et une proposition (« à corriger dans… » ou « à déporter vers… »).

## Constats

### 2026-10-05 · Étape 2 · `docs/quality-gates.md`

- RAS. Le modèle a été copié tel quel. Le brief dit que l'architecture cible est libre, donc le modèle (microservices Spring Boot, front React hexagonal) ne contredit rien.

### 2026-10-05 · Étape 3 · Surcharges BMAD (`persistent_facts`)

- **Deux skills écrivent dans les mêmes fichiers sans se coordonner.** `retro-forward-bmad` crée les six `_bmad/custom/bmad-*.toml`, puis le pilote y ajoute `file:{project-root}/docs/quality-gates.md`. Une relance de `retro-forward-bmad` réécrit les fichiers en entier, et la ligne du pilote disparaît. Aujourd'hui, le seul garde-fou est `status.sh`, qui passe l'étape 3 en `PARTIEL`.
  - *À déporter vers `retro-forward-bmad` (léger) :* à la relance, conserver les entrées de `persistent_facts` (et des autres tableaux) qu'il n'a pas écrites lui-même. Une simple fusion suffit : l'ancien contenu moins les entrées qu'il avait générées, plus les nouvelles. Variante encore plus simple : si `docs/quality-gates.md` existe, ajouter lui-même l'entrée. L'étape 3 du pilote devient alors un simple contrôle.
- **Syntaxe `file:` dans la fiche de l'étape 3.** La fiche propose `"file:docs/quality-gates.md"`, alors que la documentation BMAD 6.12 (commentaires de `customize.toml`) et toutes les entrées de `retro-forward-bmad` utilisent `file:{project-root}/…`. J'ai appliqué la forme `{project-root}`.
  - *À corriger dans `pilote-bmad/etapes/etape-03.md`.*
- **Commande de vérification à mettre à jour.** La forme utilisée avec succès est `uv run _bmad/scripts/resolve_customization.py --skill .claude/skills/<skill> --project-root . --key workflow`. `--skill` attend le dossier du skill installé sous `.claude/skills/`.
  - *À préciser dans la fiche de l'étape 3.*

### 2026-10-05 · Transverse · Hook `rtk`

- Le hook `rtk` réécrit `grep` en `rtk grep`, qui ne connaît pas `-h`. La commande du skill (`grep -h '^\*\*Moment' …/etape-*.md`) affiche donc l'aide de rtk au lieu du résultat. Contournement : `command grep` ou `sed`. Le hook a aussi bloqué le premier appel de `status.sh` en mode auto (une relance a suffi).
  - *À corriger dans `pilote-bmad/SKILL.md` :* utiliser `command grep` ou une boucle `sed`.

### 2026-10-05 · Étape 4 · Scripts de contrôle

- Les cinq scripts sont copiés et ont réussi le niveau statique : `bash -n`, outils présents, options confirmées dans l'aide. Trivy 0.74.0 confirme toutes ses options, dont `--ignore-unfixed` qui n'est pas déprécié. `help:describe` confirme `dependency-check-maven:aggregate` (avec `failBuildOnCVSS`, `autoUpdate`, `ossIndexAnalyzerEnabled`, `nodeAuditAnalyzerEnabled`, `nvdApiServerId`), `pitest-maven:mutationCoverage` (avec `mutationThreshold`, `historyInputFile`, `withHistory`) et la propriété Surefire `failIfNoTests`.
- `check-gates-exist.sh` sur le dépôt sans code : code 1, 12 `KO` lisibles, aucune erreur shell. **Limite :** sans service, la boucle par service (lecture du POM effectif, motifs XML) n'est jamais exécutée. Le test statique ne dit donc rien des motifs XML, qui restent le principal risque (étape 9).
- **Base Trivy absente** (`~/.cache/trivy` n'existe pas). `cve.sh` utilise `--skip-db-update` et `--skip-java-db-update`, donc il échouera tant que les bases n'auront pas été téléchargées une fois.
  - *À ajouter dans une fiche du pilote (étape 5 ou avant la story 1.6) :* `trivy fs --download-db-only` et `--download-java-db-only`, à confirmer dans l'aide.
- **`${CLAUDE_SKILL_DIR}` n'est pas défini** dans le shell de l'outil Bash. Les commandes des fiches (`cp "${CLAUDE_SKILL_DIR}"/…`) ne fonctionnent pas telles quelles, j'ai dû utiliser le chemin absolu.
  - *À corriger dans les fiches du pilote :* écrire le chemin résolu, ou demander de le substituer.
- Le shell de l'outil est zsh : une expression `[ =]` dans un motif `grep` non quoté a été interprétée par zsh (« bad math expression »). C'est un incident mineur de ma part, sans impact sur les scripts (ils ont un shebang bash).
- Déport vers retro : rien de pertinent à cette étape. Les contrôles qualité sont indépendants du corpus.

### 2026-10-05 · Étape 5 · Exceptions CVE et clé NVD

- Formats confirmés dans les docs officielles : en-tête `dependency-suppression.1.4.xsd` (version actuelle), expiration `until="AAAA-MM-JJZ"` ; dans `.trivyignore`, `CVE-… exp:AAAA-MM-JJ`.
- Point 13 de `non-verifie.md` (champ du `<server>` lu par `nvdApiServerId`) : **résolu**. L'aide de `dependency-check-maven` 13.0.0 dit que le `password` du serveur porte la clé.
  - *À reporter dans `pilote-bmad/reference/non-verifie.md`.*
- Clé NVD contrôlée sans être lue ni affichée : présence de `<id>nvd</id>` dans `<servers>`, mot de passe non vide de 36 caractères (format UUID). Preuve fonctionnelle : `mvn dependency-check:update-only -DnvdApiServerId=nvd` lancé sur un POM jetable. L'API NVD répond (401 162 entrées) sans l'avertissement « API Key was not provided ».
- Bases Trivy téléchargées (`trivy fs --download-db-only`, puis `--download-java-db-only`) : 24 s, 1,4 Go + 1,5 Go dans `~/.cache/trivy`. La base Java n'est gardée en cache que 3 jours, et `cve.sh` tourne avec `--skip-java-db-update`. Le premier constat de l'étape 4 reste donc vrai : il faut une mise à jour avant chaque série de scans, pas une seule fois.
  - *À ajouter dans la fiche de l'étape 5 du pilote :* un téléchargement initial des bases Trivy et NVD (`update-only`), et dire **qui** les rafraîchit ensuite (par exemple une commande `scripts/gates/cve-update.sh` lancée avant la boucle).
- Déport vers retro : rien de pertinent. La sécurité des dépendances ne dépend pas du corpus.

### 2026-10-05 · Étape 6 · Préparation du PRD

- **Le corpus se contredit sur BUG-084 (fraude contrôlée après le virement, JIRA-9200 et JIRA-9202).** Dans le tableau de `LEGACY-BUGS.md:129`, le comportement attendu est « fixé par le métier » et le bug est « validé par le métier ». Mais la note de revue `LEGACY-BUGS.md:225` dit « Proposition Claude, à relire par l'expert. Dépend de la question bloquante FEAT-027 (le contrôle fraude doit-il bloquer le virement ?) ». Et cette question bloquante n'apparaît pas dans `GAPS.md`, qui n'a que deux `[TODO important]` sur des enums de fraude. Résultat : le brief reprend BUG-084 comme tranché, et `bmad-prd` ne posera pas la question.
  - *À déporter vers le skill qui produit `LEGACY-BUGS.md` et `GAPS.md` (léger) :* toute note « dépend de la question bloquante X » doit créer une ligne `[TODO bloquant]` dans `GAPS.md`, et le statut ne doit pas être « validé par le métier » tant que X n'est pas tranché. Un contrôle de cohérence de quelques lignes dans le script de vérification du corpus suffit.
- **Le pilote n'a pas de variante « fast path » du prompt PRD.** L'utilisateur a écrit la sienne (PRD rédigé seul, `[ASSUMPTION]`, un seul lot de questions). Sans garde-fou, elle perd l'ancrage sur le corpus et l'exigence de parité du golden master.
  - *À ajouter dans `pilote-bmad/reference/prompts-bmad.md` :* une variante « fast path, avec corpus », qui garde les quatre paragraphes obligatoires et un format pour les questions ouvertes bloquantes (`[EN ATTENTE BD-n]`).

## Pistes de déport vers les skills retro (synthèse)

| Fonction aujourd'hui dans le pilote | Candidat | Intérêt | Risque de surcharge |
|---|---|---|---|
| Ajout de `docs/quality-gates.md` dans `persistent_facts` (étape 3) | `retro-forward-bmad` | supprime le conflit à la relance | faible : une entrée conditionnelle |
| Préservation des entrées étrangères à la relance | `retro-forward-bmad` | rend les surcharges de tiers durables | faible : une fusion de tableaux |
| Note « dépend de la question bloquante X » sans ligne correspondante dans `GAPS.md` | skill du corpus (vérification de cohérence) | la question remonte d'elle-même dans `bmad-prd` | faible : une règle de cohérence |
