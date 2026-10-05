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
- **Base NVD téléchargée** (`dependency-check:update-only` avec la clé de `settings.xml`) : `BUILD SUCCESS`, 401 169 entrées en **36 min**. La première exécution est longue : la fiche de l'étape 5 doit le dire, et la lancer en arrière-plan tôt (avant l'étape 6).
  - *À ajouter dans la fiche de l'étape 5 du pilote :* un téléchargement initial des bases Trivy et NVD (`update-only`), et dire **qui** les rafraîchit ensuite (par exemple une commande `scripts/gates/cve-update.sh` lancée avant la boucle).
- Déport vers retro : rien de pertinent. La sécurité des dépendances ne dépend pas du corpus.

### 2026-10-05 · Étape 6 · Préparation du PRD

- **Le corpus se contredit sur BUG-084 (fraude contrôlée après le virement, JIRA-9200 et JIRA-9202).** Dans le tableau de `LEGACY-BUGS.md:129`, le comportement attendu est « fixé par le métier » et le bug est « validé par le métier ». Mais la note de revue `LEGACY-BUGS.md:225` dit « Proposition Claude, à relire par l'expert. Dépend de la question bloquante FEAT-027 (le contrôle fraude doit-il bloquer le virement ?) ». Et cette question bloquante n'apparaît pas dans `GAPS.md`, qui n'a que deux `[TODO important]` sur des enums de fraude. Résultat : le brief reprend BUG-084 comme tranché, et `bmad-prd` ne posera pas la question.
  - *À déporter vers le skill qui produit `LEGACY-BUGS.md` et `GAPS.md` (léger) :* toute note « dépend de la question bloquante X » doit créer une ligne `[TODO bloquant]` dans `GAPS.md`, et le statut ne doit pas être « validé par le métier » tant que X n'est pas tranché. Un contrôle de cohérence de quelques lignes dans le script de vérification du corpus suffit.
- **Le pilote n'a pas de variante « fast path » du prompt PRD.** L'utilisateur a écrit la sienne (PRD rédigé seul, `[ASSUMPTION]`, un seul lot de questions). Sans garde-fou, elle perd l'ancrage sur le corpus et l'exigence de parité du golden master.
  - *À ajouter dans `pilote-bmad/reference/prompts-bmad.md` :* une variante « fast path, avec corpus », qui garde les quatre paragraphes obligatoires et un format pour les questions ouvertes bloquantes (`[EN ATTENTE BD-n]`).

### 2026-10-05 · Étape 6 · PRD finalisé, UX ajoutée hors fiche

- **PRD contrôlé selon la fiche de l'étape 6.** Les 25 outils, seuils et éléments d'architecture de `docs/quality-gates.md` sont tous trouvés par `grep` dans `prd.md` et `addendum.md`, et 11 NFR-QG sont en place. La couverture du corpus est à 100 %, mesurée par `check-prd-coverage.py`.
- **`bmad-party-mode` (Kafka ou un autre broker) a été sauté.** Kafka est imposé par `docs/quality-gates.md`, et le temps manquait (démo). `status.sh` passe directement à `bmad-architecture` : cohérent.
- **`bmad-ux` a été joué alors que le pilote ne le prévoit pas.** `retro-forward-bmad` n'écrit pas de surcharge pour `bmad-ux`, qui ignore donc le corpus et les contraintes du golden master. Il a fallu les donner à la main : wording intouchable, attributs de test stables, écarts de texte tracés pour le `text_map`.
  - *À déporter vers `retro-forward-bmad` (léger) :* une surcharge `_bmad/custom/bmad-ux.toml` avec trois `persistent_facts` :
    - les textes affichés du legacy (`MESSAGES.md`) sont intouchables, sauf écart décidé dans le PRD ;
    - un attribut de test stable sur chaque élément actionné par un scénario ;
    - les sources UI du corpus (`UI-KIT.md`, `navigation-flow.md`, `FEAT-NNN/{FIELDS,STATES,FLOW}.md`).
  - *À ajouter dans `pilote-bmad/reference/prompts-bmad.md` :* une variante « UX avec corpus et design system existant », placée entre le PRD et l'architecture.
- **Des faits du golden master utiles très tôt, découverts pendant l'UX** :
  - les scénarios d'écran comparent en mode `contains` : un texte ajouté ne casse rien, un texte retiré casse ;
  - le script de rejeu ne lit que les 5 000 premiers caractères de chaque page : une section ajoutée en haut de page peut pousser des lignes attendues hors de la fenêtre ;
  - le contrat annonçait 26 dom_text, 3 ui_html et 2 unknown, mais SC-001-03 a un fichier d'actions tout en étant rejoué en HTTP. Les décomptes manuels se sont trompés deux fois.
  - *À déporter vers `retro-forward-bmad` ou le contrat du golden master (léger) :* écrire ces trois faits (mode de comparaison, fenêtre de 5 000 caractères, décompte écran / HTTP) en tête de `golden-master-contract.md`, où `bmad-prd`, `bmad-ux` et `bmad-architecture` les liraient.
- **Une décision métier est prise en table ronde (`bmad-party-mode`, transcript `_bmad-output/party-rg-027-003-script.md`)**, puis intégrée par `bmad-prd`. Le pilote ne prévoit pas ce cas : le party mode n'y sert qu'à la vidéo client.
  - *À ajouter dans la fiche de l'étape 6 :* une table ronde peut trancher une question ouverte bloquante du PRD. Son transcript devient alors une entrée du PRD, à réconcilier à la finalisation.

### 2026-10-05 · Étape 6 · `status.sh` pendant `bmad-architecture`

- **`status.sh` compte `bmad-architecture` comme joué dès que `ARCHITECTURE-SPINE.md` existe**, alors que le fichier n'est encore que le modèle copié par le skill. Il annonce donc l'étape 6 à 2 sur 5, propose `bmad-project-context` comme prochain skill, et lève l'alerte « la spine ne mentionne pas ArchUnit, JaCoCo… » sur un brouillon vide.
  - *À corriger dans `pilote-bmad/status.sh` :* ne compter un artefact BMAD comme fait que si sa frontmatter porte `status: final`. Un brouillon donne l'état « en cours », sans alerte de contenu. Il faut faire de même pour le PRD et l'UX.
- **Le découpage en services n'est fixé ni par `quality-gates.md` ni par les fiches du pilote.** La story 1.4 (flux témoin A → Kafka → B) suppose deux services, sans dire lesquels. Une passerelle placée sous `services/` hérite des contrôles par service (`check-gates-exist.sh` exige un `*ArchitectureTest` par module).
  - *À ajouter dans `pilote-bmad/reference/prompts-bmad.md` (prompt d'architecture) :* un point « découpage en services et place d'une éventuelle passerelle ou BFF (sous `services/` avec un hexagone et ArchUnit, ou hors `services/`) ».

### 2026-10-05 · Étape 6 · Avant `bmad-project-context`

- **`bmad-project-context` a été lancé sans le prompt du pilote.** Le skill a détecté l'intention `setup` (aucun `AGENTS.md` ni `CLAUDE.md`) et commencé son état des lieux. Les exigences du prompt (section « Règles pour les agents » mot pour mot, commandes de contrôle, pointeur, import `@AGENTS.md`) sont à lui donner dans la suite de la même session.
- **Double chargement des règles dans les sessions de dev.** Le prompt du pilote met la section « Règles pour les agents » mot pour mot dans `AGENTS.md`. Or `bmad-build` et `bmad-build-auto` chargent déjà tout `docs/quality-gates.md` par `persistent_facts` (étape 3). Dans une session de dev de bmad-loop, ces règles sont donc chargées deux fois, ce que `bmad-project-context` signale comme un défaut (« aucune instruction dans deux fichiers chargés »).
  - *À trancher dans `pilote-bmad/etapes/etape-06.md` ou `etape-03.md` :* garder les deux (redondance assumée : `AGENTS.md` couvre les sessions hors skill BMAD) et le dire dans la fiche, pour que le skill ne propose pas de supprimer l'une des deux copies.
- **Ordre entre G4 et `bmad-project-context` non précisé.** G4 installe `scripts/gates/golden-master.sh` « avant le premier epic ». Si `bmad-project-context` passe avant, le bloc ne peut pas citer ce contrôle dans les commandes à lancer, et il faudra un `refresh`.
  - *À préciser dans `pilote-bmad/etapes/gm-4.md` :* placer G4 avant `bmad-project-context`, ou demander au bloc de citer le contrôle golden master dès G4 faite.

### 2026-10-05 · Étape G4 · Contrôle statique du golden master

- Script copié tel quel : aucun chemin à adapter (valeurs par défaut conformes au dépôt). Options de `derive-golden-master.py derive` et `export-golden-master-tests.py --check` confirmées par leur `--help`. Niveau statique consigné.
- **`AGENTS.md` interdit désormais `scripts/gates/**` aux agents**, alors que le pilote y installe encore `golden-master.sh` (G4) et que les stories 1.2 et 1.4 y créent des fichiers. La spine (ligne 160) prévoit ces exceptions, mais `AGENTS.md` ne les cite pas : un agent de dev de la story 1.4 pourrait refuser de créer `trace-smoke.sh`.
  - *À préciser dans le prompt `bmad-project-context` de `pilote-bmad/reference/prompts-bmad.md` :* ajouter une ligne d'exception datée (« jusqu'à la fin de l'epic 1 : la story 1.2 crée `frontend/.dependency-cruiser.cjs`, la story 1.4 crée `scripts/gates/trace-smoke.sh` »), à retirer au refresh après l'étape 10.
- Déport vers retro : rien.

### 2026-10-05 · Étape G5 · Contrôle golden master prouvé

- Réel : code 0, deux `OK`. Négatifs « test généré retouché » et « dérivé retouché » : code 1, fichiers nommés.
- **Le négatif « route du mapping remplacée par `[ASK ARCHITECT]` » de la fiche ne prouve rien sur ce dépôt.** `derive-golden-master.py` (`screen_start_url`) ne lit `routes[page]` que si `target.start_url` est vide. Ici, les 26 scénarios d'écran en `map` ont tous un `target.start_url` explicite : `routes` n'est jamais lu, et le contrôle sort en 0, à juste titre. Le cas a été remplacé par `target.start_url` de SC-003-01 à `[ASK ARCHITECT]` : code 1, scénario et champ nommés.
  - *À corriger dans `pilote-bmad/etapes/gm-5.md` et `reference/tests-scripts.md` :* viser `target.start_url` d'un scénario d'écran, ou ne viser `routes` qu'après avoir vérifié qu'un scénario `map` sans `start_url` l'utilise.
  - *À déporter vers `corpus-bmad-golden-master` (léger) :* signaler, à la dérivation, une table `routes` qu'aucun scénario ne lit (donnée morte, trompeuse pour les relecteurs).
- Moi : mon premier script de modification supposait `scenarios` en objet ; c'est une liste. Le plantage a eu lieu avant l'écriture, sans effet.
- **Fausse alerte de `status.sh` après les tests négatifs.** Restaurer `replay-mapping.json` par `git checkout` le rend plus récent que le dérivé. `status.sh` compare les dates (`-nt`, lignes 156 et 159) : il repasse G3 à « à faire » et lève « `replay-mapping.json` a changé depuis la dernière dérivation », alors que le contenu est identique au commit et que `golden-master.sh` sort en 0.
  - *À corriger dans `pilote-bmad/status.sh` :* pour G3 et cette alerte, appeler `scripts/gates/golden-master.sh` quand il existe (comparaison de contenu, environ 1 s), et ne garder la comparaison de dates qu'avant G4. *Dans `gm-5.md` :* après restauration, remettre la date du mapping (`touch -r docs/retro/golden-master/derived/replay-config.json replay-mapping.json`).
- Pour l'étape 9 (séquence de fin d'epic) et l'étape 10 (`[verify]` et empreinte) : brancher `scripts/gates/golden-master.sh` (environ 1 s) ; ajouter à l'empreinte `replay-mapping.json`, `e2e/golden-master/playwright.config.ts`, `e2e/golden-master/auth.setup.ts` et `e2e/golden-master/specs/*.spec.ts`.

### 2026-10-05 · Étape 6 · Prompt `bmad-create-epics-and-stories`

- **Le prompt des epics ne cite pas `scripts/gates/golden-master.sh`.** Le critère de chaque story des epics suivants est « tous les contrôles de `docs/quality-gates.md` sont verts », et ce fichier ne mentionne pas le contrôle golden master installé à G4. Une story pourrait toucher une route ou un `data-testid` figés sans que son critère le vérifie. J'ai ajouté une ligne au prompt donné à l'utilisateur.
  - *À ajouter dans `pilote-bmad/reference/prompts-bmad.md` (variante « avec corpus » des epics) :* « chaque story a aussi pour critère `scripts/gates/golden-master.sh` en code 0 ».
- **Le prompt laisse le choix des deux services de la story 1.1 ouvert**, alors que la spine les fixe (`coeur-bancaire` et `notifications`, flux témoin `coeur-bancaire.temoin-ecrit.v1`, ARCHITECTURE-SPINE.md:242). J'ai ajouté la précision au prompt, pour que le skill ne les réinvente pas.
  - *À ajouter dans le prompt des epics :* « reprends le découpage et le flux témoin décidés par la spine ».

### 2026-10-05 · Étape 6 · Contrôle de `epics.md`

- `epics.md` : 10 epics, 67 stories ; l'epic 1 contient 1.1 à 1.6 dans l'ordre du prompt ; chaque story a un critère avec commande et code de sortie, et un critère négatif (6 stories l'expriment par un cas de refus ou d'erreur, sans « code non nul »).
- Contrôle `grep` des outils et seuils de `docs/quality-gates.md` dans le PRD, la spine et `epics.md` : tous présents, sauf `--ignore-unfixed`, absent de `epics.md` (la story 1.6 renvoie à `scripts/gates/cve.sh`, qui le porte). Le PRD ne nomme pas OpenTelemetry, Jaeger ni datasource-micrometer (ils sont dans l'addendum et la spine).
- **Le prompt du pilote pour les epics ne prévoit pas les écarts de règles découverts à l'écriture des stories** (22 points « à arbitrer » : textes absents du corpus, contradictions PRD / spine / UX). Ils ont été tranchés en session et versés dans un lot de `textes-nouveaux.md` et dans AD-16.
  - *À ajouter dans `pilote-bmad/reference/prompts-bmad.md` (epics) :* demander la liste des `[À ARBITRER]` en fin de skill, et prévoir que les textes tranchés vont dans `textes-nouveaux.md` (lot daté).
- **Exception `e2e/golden-master/package.json` (story « Socle du rejeu final »)** : la règle « ne jamais modifier `e2e/golden-master/**` » d'`AGENTS.md` et des surcharges BMAD bloque l'installation du runner prévue par le prompt des epics. Exception décidée, à reporter dans `AGENTS.md` par un refresh.
  - *À corriger dans `pilote-bmad` (étape 3 et prompt du contexte projet) :* écrire l'interdiction sur les fichiers générés (`specs/`, `auth.setup.ts`, `playwright.config.ts`), pas sur tout le dossier.
- Déport vers retro : `retro-forward-bmad` pourrait écrire la règle golden master dans `docs/quality-gates.md` (liste des fichiers interdits), absente aujourd'hui ; léger.

### 2026-10-05 · Golden master hors de l'empreinte

- **Décision du propriétaire : le golden master reste hors de l'empreinte et du hook** (`replay-mapping.json`, `docs/retro/golden-master/derived/`, `e2e/golden-master/`). L'agent de la story « Socle du rejeu final » peut corriger le mapping, redériver et régénérer en autonomie, avec inscription au registre des écarts voulus ; `golden-master.sh` reste bloquant. Spine (ligne de l'empreinte, AD-10) et `.memlog.md` mis à jour ; règle dans `docs/quality-gates.md` (fichier préparé, copie par le propriétaire).
  - *À corriger dans `pilote-bmad/etapes/gm-5.md` :* retirer « ajoute à la commande d'empreinte : `replay-mapping.json`, `playwright.config.ts`, `auth.setup.ts`, `specs/*.spec.ts` », ou en faire une option à décider avec le propriétaire.
  - *À corriger dans `pilote-bmad/etapes/etape-10.md` (et le hook de l'étape 11) :* ne pas inclure ces fichiers par défaut quand le projet autorise leur correction par la story socle.

### 2026-10-05 · Étape 6 terminée · `bmad-sprint-planning`

- Porte de préparation à PASS : 333 FR du PRD toutes dans la table de correspondance et dans au moins une story, aucune FR orpheline côté stories ; toutes les NFR reprises ; 13 UX-DR sans story, justifiées dans `epics.md`. Les 6 questions ouvertes de la spine sont affectées à une story. Seule décision humaine en attente : expiration sur inactivité et renouvellement du jeton (AR-106), prérequis de la story 2.5.
- `sprint-status.yaml` : 10 epics, 67 stories en `backlog`, 10 rétrospectives `optional` ; le script ne lit pas comme epics 4 titres annexes de `epics.md` (« Epic 1 imposé », « Epic List »…), ce qui est attendu.
- **Clés de story accentuées** (`1-2-rendre-l-architecture-hexagonale-vérifiable-et-bloquante`) : le slug du script est Unicode par conception. `status.sh` les lit correctement (étape 7 : `1.1=backlog ; 1.2=backlog`). À surveiller à l'étape 8 : bmad-loop et ses expressions de clés de story.
  - *À ajouter dans `pilote-bmad/etapes/etape-08.md` :* vérifier que bmad-loop accepte des clés non ASCII avant le premier `run`.

### 2026-10-05 · Étape 7 · Story 1.1 (bmad-build, déroulée par l'agent à la demande du propriétaire)

- Commit `9e27361`, story en `review`. Vérifié : `mvn -B -q verify -DfailIfNoTests=true` (6 tests par module), `help:effective-pom` des deux services, `vitest run` (5 tests), `tsc --noEmit`, `golden-master.sh` : code 0 ; aucun fichier protégé modifié depuis le commit de départ.
- **`build.sh` ne peut pas être vert en 1.1** : il lance `-Dtest='*ArchitectureTest'` et depcruise, livrés en 1.2. La consigne du pilote (et AGENTS.md « avant de déclarer un travail terminé : build.sh ») est inapplicable à la 1.1.
  - *À corriger dans `pilote-bmad/etapes/etape-07.md` et le prompt de la 1.1 :* critère de fin de la 1.1 = commandes de ses critères + `golden-master.sh` ; `build.sh` à partir de la 1.2.
- **Critère « fichiers protégés inchangés » basé sur `origin/main`** : le `main` local a 14 commits d'avance non poussés (planification), donc `git diff merge-base origin/main` liste tous les fichiers protégés. Comparaison faite depuis le commit de départ de la story.
  - *À ajouter dans le prompt des epics :* comparer depuis `baseline_commit` de la spec, ou pousser `main` avant le premier `bmad-build`.
- **`build.sh` sans `clean`** : un module dont on retire tous les tests passe encore, les classes de test compilées restent dans `target/test-classes` (constaté par l'agent ; échec seulement avec `mvn clean verify`). À tester à l'étape 7 (négatif de `build.sh`) ; correctif éventuel du script protégé à proposer avant l'empreinte.
- **`build.sh` ne vérifie pas les types du front** (pas de `tsc --noEmit`) : une erreur de type passe tous les contrôles. Consigné dans `deferred-work.md`. Décision humaine avant l'étape 10.
- RTK réécrit `npx vitest run` et écrit `frontend/.vitest/json/output.json` : ignoré par `.gitignore`. Sans effet sur les contrôles (`build.sh` est lancé hors réécriture).

### 2026-10-05 · Étape 7 · Story 1.2 et tests de `build.sh` / `canary-arch.sh`

- Commit `f7ff77d`, story en `review`. `build.sh` code 0, `check-gates-exist.sh` : 7 lignes de la story à `OK`, `golden-master.sh` code 0, aucun fichier protégé modifié hors `frontend/.dependency-cruiser.cjs`.
- Tests consignés : `build.sh` réel OK, négatif OK (depcruise sur domaine → adaptateur et `react` dans application, ArchUnit sur Spring dans le domaine) ; `canary-arch.sh` réel et négatif OK, sans résidu.
- **`build.sh` a tort sur un cas** : tous les tests d'un module retirés → code 0, car `mvn verify` sans `clean` rejoue `target/test-classes`. Le même biais masquerait la suppression d'un `*ArchitectureTest` (seul `check-gates-exist.sh` la verrait, sur les sources). Correctif proposé à l'humain : `clean verify`. Consigné `negatif KO`.
  - *À corriger dans `pilote-bmad/fichiers/scripts/gates/build.sh` et `reference/tests-scripts.md` :* `mvn -B -q clean verify -DfailIfNoTests=true` ; ajouter le négatif « module sans test » au plan de test de `build.sh`.
- `onionArchitecture()` échoue sur une couche vide (« domain service » non déclarée) : `withOptionalLayers(true)` nécessaire, compensé par `ensureAllClassesAreContainedInArchitecture()`.
  - *À ajouter dans `pilote-bmad/reference/formes.md` (story 1.2) :* ces deux réglages, et `ImportOption.DoNotIncludeTests` (les tests de contexte Spring dépendent des adaptateurs).
- La revue a relevé des listes d'interdits plus courtes que les contournements possibles (références de méthode `Instant::now` corrigées ; `nanoTime`, `ZoneId.systemDefault`, `RestOperations`, `HttpURLConnection`, imports non résolus de depcruise reportés) : décisions d'architecture à prendre avant l'empreinte, listées dans `deferred-work.md`.
- Les stories restent en `review` (statut de fin de `bmad-build`) : `status.sh` garde l'étape 7 à « à faire » tant que l'humain ne les passe pas à `done`.

### 2026-10-05 · Correctif de `build.sh` et de `docs/quality-gates.md` (copie par le propriétaire)

- Versions préparées par l'agent dans un dossier non suivi, copiées par le propriétaire : `clean verify` (un module sans test n'est plus masqué par `target/`) et `npx tsc --noEmit` (types du front) ; deux lignes « Types front » et « Build back » dans `docs/quality-gates.md`.
- Tests rejoués sur la version installée et consignés : réel OK ; négatifs OK (depcruise ×2, ArchUnit, module sans test, erreur de type TS2322).
  - *À reporter dans `pilote-bmad/fichiers/scripts/gates/build.sh`, `fichiers/quality-gates.md` et `reference/tests-scripts.md` :* les deux ajouts et les deux négatifs correspondants.
- Le mode opératoire « l'agent prépare, l'humain copie » marche bien pour les fichiers protégés avant l'empreinte : il garde la règle « jamais modifier un contrôle » intacte pour l'agent.
  - *À ajouter dans `pilote-bmad` (règles) :* quand un script protégé a tort, préparer la version corrigée dans un dossier non suivi, la tester en l'exécutant depuis ce dossier, puis donner la commande `cp` à l'humain.

### 2026-10-05 · Étape 8 · Installation de bmad-loop et essai à blanc

- `uv tool install` de bmad-loop 0.13.1 (tag vérifié par `git ls-remote`), `bmad-loop init --project . --cli claude`, `bmad-loop validate` sans `FAIL`, `bmad-loop run --dry-run --epic 1` : 4 stories, de 1.3 à 1.6, clés accentuées acceptées, stories en `backlog` prises en charge (le dev les crée).
- **`init` crée `.claude/settings.json`** (hooks `relay` SessionStart, Stop, SessionEnd, PreCompact…), fichier de la liste protégée. Il faut lancer `claude` une fois dans le projet pour approuver ces hooks avant le premier `run`.
  - *À ajouter dans `pilote-bmad/etapes/etape-08.md` :* annoncer la création de `.claude/settings.json` dans le plan ; l'étape 11 devra fusionner son hook avec ceux de bmad-loop.
- **`init` ajoute `.bmad-loop/policy.toml` au `.gitignore`**, alors que l'étape 10 le fige dans l'empreinte. Décision D3 de `_bmad-output/decisions-a-trancher.md`.
  - *À ajouter dans `etape-08.md` :* retirer la ligne du `.gitignore`, ou décider explicitement de le garder local.
- **`init` génère une politique complète (16 Ko)** : seuls `[verify] commands` et `[review] trigger = "always"` ont été changés, le reste gardé.
  - *À corriger dans `etape-08.md` :* la fiche donne un `policy.toml` réduit, qui ferait perdre les réglages par défaut s'il était écrit tel quel ; dire « modifier ces clés dans le fichier généré ».
- **`validate` exige un arbre propre** : `_bmad/render/` était suivi (recommandation de l'outil : `git rm -r --cached _bmad/render`), et un dossier non suivi du propriétaire bloquait (exclu en local par `.git/info/exclude`).
- **Les agents de la boucle tournent en `--permission-mode bypassPermissions`**, et le hook de protection n'arrive qu'à l'étape 11. Pendant l'epic 1, rien n'empêche techniquement un agent d'écrire dans `scripts/gates/**`. Décision D1.
  - *À ajouter dans `etape-08.md` :* le signaler, et prévoir un `git diff --name-only` des fichiers protégés à la fin du run (étape 9).
- La story 1.6 a des prérequis humains (clé NVD, Trivy 0.75.0, bases à jour) que la boucle ne peut pas fournir. Décision D2.

## Pistes de déport vers les skills retro (synthèse)

| Fonction aujourd'hui dans le pilote | Candidat | Intérêt | Risque de surcharge |
|---|---|---|---|
| Ajout de `docs/quality-gates.md` dans `persistent_facts` (étape 3) | `retro-forward-bmad` | supprime le conflit à la relance | faible : une entrée conditionnelle |
| Préservation des entrées étrangères à la relance | `retro-forward-bmad` | rend les surcharges de tiers durables | faible : une fusion de tableaux |
| Note « dépend de la question bloquante X » sans ligne correspondante dans `GAPS.md` | skill du corpus (vérification de cohérence) | la question remonte d'elle-même dans `bmad-prd` | faible : une règle de cohérence |
| Surcharge `bmad-ux` (wording intouchable, attributs de test, sources UI du corpus) | `retro-forward-bmad` | l'UX respecte le golden master sans consigne manuelle | faible : un fichier de surcharge de plus |
| Faits du rejeu (mode `contains`, fenêtre de 5 000 caractères, décompte écran / HTTP) en tête du contrat | export du golden master | évite des erreurs de décompte et de placement dans le PRD et l'UX | faible : 3 lignes générées |
