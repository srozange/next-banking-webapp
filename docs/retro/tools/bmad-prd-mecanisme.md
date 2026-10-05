# Paramétrage de BMAD par retro-forward-bmad

<!-- Généré par l'outil mechanism-doc du dépôt retro-skill (tools/mechanism-doc/mechanism-doc.py render --name bmad-prd) depuis docs/mecanismes/bmad-prd.toml — ne pas éditer à la main. -->

Ce document explique comment le skill retro-forward-bmad prépare un projet BMAD à partir du corpus de rétro-ingénierie d'une application existante : d'abord il joue lui-même la phase d'analyse de BMAD (product brief ; le PRD, lui, est écrit ensuite par bmad-prd), puis il écrit dans le repo cible les overrides d'équipe BMAD (`_bmad/custom/<skill>.toml`) qui règlent bmad-prd et les skills BMAD suivants. Chaque fiche dit ce que le mécanisme garantit, la consigne exacte écrite, comment BMAD l'exécute et ses limites connues.

Les résultats cités viennent du POC 55m1 (application bancaire de démonstration, BMAD 6.12.0 installé par npm), bmad-prd exécuté sous Sonnet 4.6 ; une passe Opus des mêmes cas est à mesurer. Une consigne « non testée » est livrée sans avoir encore été validée par un essai : son effet est attendu, pas constaté.

## Simulation de la phase d'analyse BMAD

### Objectif

Faire démarrer bmad-prd directement sur un product brief terminé, écrit depuis le corpus, sans jamais lancer un skill BMAD sans humain ; bmad-prd écrit ensuite lui-même tout le PRD depuis le brief et le corpus.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

Aucune consigne : mécanisme purement explicatif.

### Explication

retro-forward-bmad écrit lui-même le dossier de run de bmad-product-brief : `{planning_artifacts}/briefs/brief-{projet}-{date}/` avec `brief.md` en `status: final`, `addendum.md` et `.memlog.md` (événement « Brief finalized »). Il n'écrit jamais de PRD : l'ancien mode brief+prd, qui écrivait un PRD en brouillon repris par bmad-prd, a été retiré après le POC 55m1 (DEC-096) — en mode brief avec Opus, bmad-prd couvre seul les 163 RG Confirmé et traite tous les cas.

Comment : les faits du brief sont extraits du corpus par script (generate-brief-facts.py). Le LLM ne rédige que la prose, uniquement depuis ces faits ; toute affirmation hors faits est marquée [ASSUMPTION]. L'écriture est faite par script (write-bmad-run.py) : frontmatter, titres contrôlés contre le gabarit BMAD installé, memlog écrit par le memlog.py de BMAD lui-même. Ensuite seulement, les overrides décrits dans les fiches suivantes disent à bmad-prd où sont le brief et le corpus.

Pourquoi ça marche : BMAD reconnaît un dossier de run terminé à son frontmatter (status) et à son memlog ; il enchaîne donc sur l'étape suivante comme si l'équipe avait fait le brief. Chaque affirmation remonte au corpus. Écarté : lancer bmad-product-brief ou bmad-prd sans humain (le LLM répondrait à la place de l'équipe, résultat non reproductible).

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| Le brief « final » n'a pas eu le coaching de l'équipe : ce que le corpus ignore (vision, objectifs) reste vide | PRD : « Vision … à définir lors de la reprise par bmad-prd » → Validate : critical « Vision is a placeholder », cohérence stratégique « thin » | bmad-product-brief (coaching) ; bmad-prd Discovery | Analyse → Discovery |
| Un manque bloquant n'apparaît pas dans le brief | le brief disait seulement « 126 lacunes » ; il a fallu activation_steps_append pour que la piste d'audit soit posée | memlog du brief (entrées question) | Analyse → Activation de bmad-prd |
| La simulation dépend du format de la version BMAD installée (titres, frontmatter, memlog) | profils g4 / g5 dans assets/bmad-profiles/ ; seule 6.12.0 (npm) testée par le POC, 6.13.0-next jamais | gabarit du brief et du PRD de la version installée ; memlog.py | toutes |
| bmad-prd doit retrouver lui-même les RG dans le corpus : la couverture dépend du modèle | POC 55m1, mode brief : Sonnet 4.6 reprend 153 RG Confirmé sur 163, Opus 163 sur 163 ; le relecteur de couverture et corpus-bmad-prd-coverage signalent les manques | bmad-prd Create, Drafting | Drafting |

## Contexte « rénovation » commun à tous les agents BMAD

### Objectif

Chaque skill BMAD lancé ensuite sait qu'il rénove une application existante : le corpus est la référence de l'existant, pas une spécification à recopier, et il sait quels fichiers du corpus lire pour son travail.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **renovation** — champ `workflow.persistent_facts` du fichier `_bmad/custom/{skill}.toml (bmad-prd, bmad-architecture, bmad-create-epics-and-stories, bmad-qa-generate-e2e-tests, bmad-sprint-planning, bmad-build, bmad-build-auto)`
  - condition : MANIFEST.json exporté
  - testée avec : 6.12.0

```text
Projet de rénovation d'une application existante. Le corpus de rétro-ingénierie {retro}/ (index : {retro}/MANIFEST.json{manifest_at}) décrit son comportement actuel : c'est la référence de ce qui existe, pas une spécification à recopier. L'architecture cible est libre et peut différer de l'actuelle. Le périmètre fonctionnel peut être revu avec le métier (ajouts, retraits, évolutions) : tout écart volontaire au comportement actuel est décidé explicitement et tracé (FEAT-NNN, RG-NNN-NNN concernés).
```

- **pointers** — champ `workflow.persistent_facts` du fichier `_bmad/custom/{skill}.toml (bmad-prd, bmad-architecture, bmad-create-epics-and-stories, bmad-qa-generate-e2e-tests, bmad-build)`
  - condition : MANIFEST.json exporté
  - testée avec : 6.12.0

```text
Fichiers du corpus utiles à ton travail : {files}{note}.
```

### Explication

`persistent_facts` est chargé une fois, à l'activation du skill, et gardé en contexte pendant toute la session (les entrées `file:` sont chargées comme faits). La consigne « rénovation » est écrite en tête pour chaque skill de `overrides.renovation_fact_skills` du profil ; la liste des fichiers utiles vient de `overrides.renovation_pointers` et ne cite que des fichiers réellement exportés (motifs résolus par le script). Rien n'est écrit si l'export n'a pas de MANIFEST.json.

### Limitations connues

Aucune connue.

## Entrées du PRD : le brief et le corpus

### Objectif

bmad-prd prend le brief simulé et le corpus comme entrées obligatoires, et chaque FR cite la règle de gestion qu'elle couvre.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **prd-inputs** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : le brief est fourni (--brief)
  - testée avec : 6.12.0

```text
Projet de rénovation dérivé d'un corpus retro (commit {commit}). Entrées obligatoires du PRD : le brief {brief} et {retro}/ (règles : {retro}/features/*/RG.md, index {retro}/rules-index.json). Chaque FR cite l'identifiant RG-NNN-NNN qu'elle couvre. Une fonctionnalité ou une règle volontairement non reprise est citée (FEAT-NNN, RG-NNN-NNN) dans la section Non-Goals, avec sa raison.
```

- **prd-read-brief** — champ `workflow.activation_steps_append` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : le brief est fourni (--brief)
  - testée avec : 6.12.0

```text
Lis le brief {brief} et son addendum {addendum} : ce sont les entrées obligatoires du PRD, à donner aussi comme documents d'entrée (Input reconciliation à Finalize).
```

### Explication

Deux consignes complémentaires. Le fait `persistent_facts` donne les entrées et la règle de citation pour toute la session. L'étape `activation_steps_append` est exécutée après l'accueil, avant le workflow, avec confirmation par bmad-prd : elle lui fait lire le brief et son addendum et les déclarer comme documents d'entrée, ce qui les fait entrer dans la réconciliation des entrées à la finalisation (Input reconciliation). Le chemin du dossier de run du brief est celui écrit par la simulation.

### Limitations connues

Aucune connue.

## Manques bloquants posés dès le démarrage

### Objectif

Un manque bloquant du corpus (ligne [TODO bloquant] de GAPS.md) est posé à l'utilisateur dès le démarrage de bmad-prd et finit dans le PRD comme FR ou Open Question tranchée.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **prd-gaps-file** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté GAPS.md
  - testée avec : 6.12.0

```text
file:{project-root}/{retro}/GAPS.md
```

- **prd-gaps-blockers** — champ `workflow.activation_steps_append` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté GAPS.md
  - testée avec : 6.12.0

```text
Lis {project-root}/{retro}/GAPS.md. Pour chaque ligne marquée [TODO bloquant], présente la question à l'utilisateur avant de commencer Discovery (ou la reprise d'un PRD en brouillon) et annonce-la comme phase-blocker : elle doit finir dans le PRD soit comme FR, soit comme Open Question tranchée. Dès que le dossier du run existe, consigne chacune dans le memlog (memlog.py append).
```

### Explication

GAPS.md est chargé en contexte (`persistent_facts`, entrée `file:`), mais un fait est passif : il ne déclenche aucune action. L'étape `activation_steps_append` est, elle, exécutée par l'agent principal de bmad-prd après l'accueil et avant la découverte, à chaque démarrage ou reprise : elle lui fait présenter chaque manque bloquant comme phase-blocker et le consigner dans le memlog. Résultat du POC : la question de la piste d'audit est posée dès l'activation, à chaque reprise. Le contrôle de fin (chaque manque bloquant a-t-il une réponse dans le PRD ?) est fait par le relecteur de couverture (fiche « Couverture du corpus »), à partir de la liste `blocking_gaps` calculée par check-prd-coverage.py : il n'y a pas de relecteur séparé.

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| persistent_facts est passif : du contexte, pas une action | GAPS.md seulement dans persistent_facts : le manque bloquant n'est pas posé à l'utilisateur | bmad-prd/customize.toml [workflow] persistent_facts (chargé une fois à l'activation) | Activation |
| La réponse à un phase-blocker reste dans le memlog jusqu'à la finalisation | piste d'audit consignée en Open Question dans le memlog, absente du PRD → 2 critical au Validate | workflow bmad-prd, phase-blockers résolus avant Finalize | Drafting → Finalize |
| L'export retire le niveau du manque des fichiers du corpus | PERMISSIONS.md exporté : « Exigence légale de la piste d'audit inconnue… » sans [TODO bloquant] ; seul GAPS.md garde le niveau | côté retro : export-distilled.py | — |

## Corpus consulté à la demande, avec une carte

### Objectif

Quand une question sur le comportement de l'application existante se pose, bmad-prd ouvre d'abord le fichier du corpus qui y répond, au lieu de supposer ou de chercher sur le web.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **prd-index-file** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté INDEX.md
  - non testée

```text
file:{project-root}/{retro}/INDEX.md
```

- **prd-corpus-first** — champ `workflow.external_sources` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté INDEX.md
  - non testée

```text
Pour toute question sur le comportement de l'application existante (écrans, champs, messages, règles, droits, batchs, données), consulte d'abord {project-root}/{retro}/ (carte : {retro}/INDEX.md) avant toute recherche web ou supposition, et cite le fichier et la section utilisés.
```

### Explication

INDEX.md, produit par export-distilled.py, est la carte des fichiers réellement exportés et de leur contenu ; il est chargé en contexte (`persistent_facts`). `external_sources` est un registre que BMAD consulte à la demande, jamais de façon préventive, quand la conversation fait apparaître un besoin correspondant (découverte, rédaction, validation) : la consigne y fait du corpus la première source à consulter, et demande de citer le fichier lu. Ces deux consignes sont non testées : le cas du POC qui les met à l'épreuve (questions dont la réponse n'est que dans FIELDS.md) n'a pas encore de résultat.

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| external_sources n'est consulté que si l'agent juge qu'un besoin correspond | une question dont la réponse est dans FIELDS.md peut recevoir une réponse supposée sans ouverture du corpus | bmad-prd/customize.toml [workflow] external_sources (consulté à la demande, jamais de façon préventive) | Discovery, Drafting |

## Traçabilité des écarts au corpus

### Objectif

Une décision prise en séance qui s'écarte du corpus est tracée dans le PRD : règle citée, marqueur d'écart et raison ; aucune règle ne perd son identifiant quand bmad-prd en fusionne plusieurs.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **prd-merge-cite-all** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté features/*/RG.md
  - non testée

```text
Quand tu fusionnes des règles voisines de plusieurs fonctionnalités en une seule FR, cite tous leurs identifiants RG.
```

- **prd-replaced-rule-cite** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté features/*/RG.md
  - non testée

```text
Quand une décision remplace une règle du corpus, cite l'identifiant de la règle remplacée dans la FR.
```

- **prd-decision-marker** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté features/*/RG.md
  - non testée

```text
Quand une FR s'écarte volontairement d'une règle du corpus, ou reprend une règle de statut Suspect, par décision prise en séance, marque-la [ÉVOLUTION — <raison>] et cite la RG concernée sur sa ligne Source.
```

- **prd-deviation-review** — champ `workflow.finalize_reviewers` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté features/*/RG.md
  - non testée

```text
Relecteur « conformité des écarts au corpus », sur tout le PRD, fonctionnalité par fonctionnalité. Pour chaque FR, lis la ou les RG citées sur sa ligne Source dans {project-root}/{retro}/features/FEAT-NNN/RG.md. Si la FR diffère de la RG (seuil, condition, effet, message), elle doit être marquée comme écart volontaire, citer la RG exacte concernée et donner sa raison ; sinon finding critical. Vérifie aussi que chaque décision du memlog ({doc_workspace}/.memlog.md) qui mentionne une RG cite la bonne RG (texte de la RG conforme à ce que dit la décision) et a été appliquée au PRD ; sinon finding critical.
```

### Explication

Trois consignes de rédaction en `persistent_facts`, gardées en contexte pendant la rédaction : citer tous les identifiants des règles fusionnées, citer la règle remplacée par une décision, et marquer l'écart volontaire d'un seul marqueur, `[ÉVOLUTION — raison]`, que check-prd-coverage.py reconnaît (avec `[CORRECTION …]` et « promu » constatés au POC). Le relecteur « conformité des écarts au corpus » est un sous-agent lancé à la finalisation et au Validate, sur tout le PRD : il compare chaque FR aux RG citées et chaque décision du memlog à la bonne RG. Au premier POC, limité aux virements, il a fait passer le « découvert de 1 000 € » tracé comme écart à la règle des 500 € ; étendu à tout le PRD sans essai, il est non testé dans cette forme.

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| Un écart au corpus dit en séance n'est pas signalé spontanément | « découvert 1 000 € » appliqué ; la contradiction avec le corpus n'est vue que sur demande ; memlog 433 cite la mauvaise RG (RG-003-006), corrigé en 434 | aucun champ ne déclenche une comparaison au corpus pendant la rédaction | Drafting |
| bmad-prd déduplique les règles voisines (bonne pratique de PRD) mais perd la traçabilité des identifiants | RG-025-009 / RG-025-010 couvertes par la FR de RG-002-01x sans être citées (second POC, Sonnet 4.6) | aucun champ ; dépend des consignes | Drafting |

## Conditions qui se recouvrent

### Objectif

Quand deux règles s'appliquent à la même situation, le PRD rend leur lien explicite : une seule FR avec « sauf si … » citant les deux RG, ou deux FR qui se renvoient l'une à l'autre.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **prd-exception-one-sentence** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté features/*/RG.md
  - non testée

```text
Si tu as besoin de deux points pour décrire une même règle, écris-la en une seule phrase avec l'exception explicite et cite les deux RG.
```

- **prd-overlap-review** — champ `workflow.finalize_reviewers` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté features/*/RG.md
  - non testée

```text
Relecteur « conditions qui se recouvrent », sur tout le PRD : pour chaque fonctionnalité, {project-root}/{retro}/features/FEAT-NNN/RG.md et la section correspondante du PRD ; commence par les RG dont le texte renvoie déjà à une autre RG. Compare les RG deux à deux. Si deux règles de statut Confirmé ont des conditions qui peuvent être vraies en même temps et des effets opposés (ex. rejeter / exécuter), c'est une contradiction, même si l'une paraît être l'exception de l'autre : une exception n'est acceptée que si la règle générale l'énonce explicitement (« sauf », « unless », renvoi à l'autre RG). Quand deux RG s'appliquent à la même situation, le PRD rend leur lien explicite : une seule FR avec « sauf si … » citant les deux RG, ou deux FR qui se renvoient l'une à l'autre. Chaque contradiction non explicitée, dans le corpus ou entre les FR qui en dérivent, est un finding critical qui cite les deux identifiants.
```

### Explication

Une consigne de rédaction en `persistent_facts` (une règle et son exception en une seule phrase, les deux RG citées) agit pendant la rédaction ; le relecteur « conditions qui se recouvrent », sous-agent lancé à la finalisation et au Validate sur tout le PRD, compare les RG deux à deux et fait un finding critical de chaque contradiction non explicitée. Au premier POC, limité aux virements, il a relevé RG-003-007 / RG-003-035 (2 critical). Le correctif en amont est côté corpus : retro-rules signale dès la rédaction deux règles qui se recouvrent sans « sauf » (retro-skill-x85h.5).

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| Une contradiction écrite dans le corpus n'est pas vue pendant la rédaction | RG-003-007 (refus) et RG-003-035 (découvert 500 € accepté) rédigées « règle générale + exception » ; seul le relecteur dédié la voit au Validate | finalize_reviewers ne s'exécutent qu'à Finalize / Validate | Drafting, Finalize |
| Le modèle repère deux valeurs différentes pour la même donnée, pas une règle générale contredite par une exception | relève 300 / 360 mois (RG-007-005 / RG-007-027), pas RG-003-007 / RG-003-035 | aucun champ ; dépend du modèle (Sonnet 4.6 ; Opus à mesurer) | Drafting |

## Couverture du corpus avant la finalisation

### Objectif

Avant de finaliser le PRD, aucune règle confirmée, aucun bug à corriger ni aucun manque bloquant du corpus n'est perdu, ou bien l'écart est tracé (section Non-Goals).

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **prd-coverage-review** — champ `workflow.finalize_reviewers` du fichier `_bmad/custom/bmad-prd.toml`
  - condition : fichier exporté tools/retro-coverage-review.md
  - testée avec : 6.12.0

```text
file:{project-root}/{retro}/tools/retro-coverage-review.md
```

### Explication

Le relecteur de couverture est une consigne en fichier (`file:` …/tools/retro-coverage-review.md, copiée par l'export) : BMAD la charge comme prompt d'un sous-agent à la finalisation et au Validate. Ce sous-agent exécute check-prd-coverage.py, dont le JSON fait foi (règles et bugs manquants, FR interdites, décisions de séance, manques bloquants `blocking_gaps`), puis rend son verdict. Comme un relecteur de finalisation peut être sauté, retro-forward-bmad dépose aussi dans le repo cible le skill d'assistance corpus-bmad-prd-coverage, que l'utilisateur lance lui-même avant de finaliser : même script, restitution par fonctionnalité, choix « à reprendre » ou « écarté volontairement », consignes remises à bmad-prd (le skill ne modifie jamais le PRD).

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| Un relecteur de finalisation peut être sauté | second POC : aucune revue de couverture produite à la finalisation, 10 RG Confirmé oubliées sans que personne le voie | bmad-prd SKILL.md, gate de finalisation « stakes-calibrated » (hobby / solo : relecteurs lancés sans menu ou sautés) | Finalize |
| Les relecteurs LLM ne sont pas déterministes et coûtent un sous-agent chacun | relecteurs du premier POC limités à une fonctionnalité pour le coût ; un relecteur par sujet sur tout le PRD retenu | finalize_reviewers : un sous-agent par entrée | Finalize, Validate |

## Bugs du legacy à corriger

### Objectif

Chaque bug du legacy que le métier a décidé de corriger devient une exigence couverte par un test, dans tous les skills BMAD qui suivent.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **bugs-to-fix** — champ `workflow.persistent_facts` du fichier `_bmad/custom/{skill}.toml (bmad-prd, bmad-architecture, bmad-create-epics-and-stories, bmad-qa-generate-e2e-tests, bmad-build, bmad-build-auto)`
  - condition : au moins un bug du legacy à corriger
  - testée avec : 6.12.0

```text
Les bugs du legacy à corriger sont listés dans {retro}/00-global/LEGACY-BUGS.md ; chaque correction doit être couverte par un test.
```

### Explication

Le fait est écrit en `persistent_facts` des skills `overrides.bugs_fact_skills` du profil (PRD, architecture, epics, tests, build), seulement si le registre exporté compte au moins un bug « corriger ». Les bugs « ne pas corriger » ne sont ni exigence ni test : un renvoi du PRD à LEGACY-BUGS.md suffit (contrôlé par check-prd-coverage.py).

### Limitations connues

Aucune connue.

## Golden master du legacy

### Objectif

L'architecture tranche comment rejouer sur la nouvelle application les scénarios enregistrés sur l'existante, et les skills suivants rejouent ce golden master dérivé jusqu'au cutover.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **gm-mapping** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-architecture.toml`
  - condition : contrat golden master exporté
  - testée avec : 6.12.0

```text
Golden master du legacy : l'architecture tranche les décisions de {retro}/golden-master-contract.md (URL de base et authentification de la cible, point d'entrée cible de chaque scénario, sortie qui fait foi) dans replay-mapping.json, puis dérive le golden master (section « Procédure » de {retro}/golden-master-contract.md, étapes 1 et 2).
```

- **gm-target-conventions** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-architecture.toml`
  - condition : contrat golden master exporté
  - non testée

```text
Pour que le golden master soit rejouable sur la nouvelle application, l'architecture fixe : la table des routes (page de l'application existante vers route cible, clé routes de replay-mapping.json) ; un attribut de test stable sur chaque champ et bouton actionné par un scénario ; la règle sur les libellés et messages affichés (repris à l'identique, ou écart listé) ; une page de connexion à formulaire et une session conservée en cookie ou en stockage local, pour la connexion de test de chaque rôle ; le chargement des données de référence et la remise à zéro de la base avant un rejeu complet.
```

- **gm-assist** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-architecture.toml`
  - condition : contrat golden master exporté
  - non testée

```text
Les décisions par scénario de replay-mapping.json se prennent avec le skill corpus-bmad-golden-master, à lancer avant de finaliser l'architecture ; il mesure par les mêmes scripts que le relecteur de finalisation.
```

- **gm-replay** — champ `workflow.persistent_facts` du fichier `_bmad/custom/{skill}.toml (bmad-create-epics-and-stories, bmad-qa-generate-e2e-tests, bmad-build, bmad-build-auto)`
  - condition : contrat golden master exporté
  - testée avec : 6.12.0

```text
Recette de non-régression : le golden master dérivé selon {retro}/golden-master-contract.md (section « Procédure », étape 3) rejoue sur l'application cible ; critère de cutover : failed: 0 et errors: 0.
```

- **gm-replay-foundation** — champ `workflow.persistent_facts` du fichier `_bmad/custom/bmad-create-epics-and-stories.toml`
  - condition : contrat golden master exporté
  - non testée

```text
Une story livre le socle du rejeu final du golden master : chargement des données de référence, remise à zéro, comptes de test par rôle, bouchons des intégrations tierces, commande de rejeu. Chaque story cite les scénarios SC-NNN-NN du golden master dérivé ({retro}/golden-master/derived/) qui couvrent sa fonctionnalité, pour savoir où corriger un écart au rejeu final ; aucun scénario dérivé ne reste sans story.
```

- **gm-replay-final** — champ `workflow.persistent_facts` du fichier `_bmad/custom/{skill}.toml (bmad-build, bmad-build-auto)`
  - condition : contrat golden master exporté
  - non testée

```text
Le golden master se rejoue une seule fois, en entier, à la fin du développement, avant la bascule : pas pendant les stories ni entre les epics. Ne modifie jamais un test généré (e2e/golden-master/), le dossier dérivé ({retro}/golden-master/derived/) ni replay-mapping.json : si un scénario paraît faux, arrête-toi et signale-le.
```

- **gm-review** — champ `workflow.finalize_reviewers` du fichier `_bmad/custom/bmad-architecture.toml`
  - condition : contrat golden master exporté ; fichier exporté tools/golden-master-review.md
  - testée avec : 6.12.0

```text
file:{project-root}/{retro}/tools/golden-master-review.md
```

### Explication

Écrit seulement si le contrat golden master est exporté. `persistent_facts` donne à l'architecture la procédure du contrat (mapping et dérivation), les conventions qui rendent la cible rejouable (table des routes, attribut de test stable, règle sur les libellés, connexion de test par rôle, données de référence et remise à zéro) et le renvoi au skill d'assistance corpus-bmad-golden-master pour les décisions par scénario. Le découpage en stories reçoit le socle du rejeu final et le rattachement de chaque scénario à une story ; le build, manuel ou de la boucle bmad-loop (bmad-build-auto), sait que le golden master ne se rejoue qu'une fois, en entier, à la fin du développement, et ne touche jamais aux tests générés, au dossier dérivé ni au mapping. Epics, tests et build gardent le critère de cutover. Le relecteur golden master (`file:` …/tools/golden-master-review.md) est un sous-agent lancé à la finalisation de l'architecture : il exécute la dérivation à blanc, vérifie que le mapping est complet, rappelle les règles que les suppressions font perdre et lève un finding pour chaque suppression qui n'est ni un bug tranché ni une décision de périmètre. Un skill facultatif du profil (overrides.optional_skills : bmad-build-auto, bmad-sprint-planning) non installé est sauté sans bloquer les autres ; un skill obligatoire absent bloque.

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| Deux rédacteurs possibles du mapping : la consigne gm-mapping demande encore à l'architecture de le remplir, alors que le skill d'assistance l'écrit | dette acceptée le 4 octobre 2026 (option A) ; reformuler gm-mapping remettrait à zéro son statut testé | persistent_facts de bmad-architecture | Architecture |

## Contrôles qualité du projet partagés avec pilote

### Objectif

Le skill pilote et retro-forward-bmad écrivent dans les mêmes fichiers d'override : une relance du forward ne fait plus perdre le renvoi aux contrôles qualité du projet.

### Versions concernées

| Génération | Plage | Versions testées |
| --- | --- | --- |
| g4 | >=6.11.0 <6.13.0 | 6.12.0, 6.11.0 |
| g5 | >=6.13.0-0 <6.14.0 | 6.13.0-next |

### Solution

- **quality-gates-file** — champ `workflow.persistent_facts` du fichier `_bmad/custom/{skill}.toml (chaque skill qui reçoit une autre consigne)`
  - condition : fichier docs/quality-gates.md présent dans le repo cible
  - non testée

```text
file:{project-root}/docs/quality-gates.md
```

### Explication

Le skill pilote, installé dans le même repo cible, ajoute à chaque override `file:{project-root}/docs/quality-gates.md`. Le forward réécrit en entier les fichiers qui portent son en-tête : sans cette consigne, la ligne de pilote disparaissait à chaque relance. Le forward écrit donc lui-même ce renvoi, en dernier fait de `persistent_facts`, dans chaque fichier qu'il écrit, dès que `docs/quality-gates.md` existe dans le repo cible (condition `target:`). Ordre conseillé : forward d'abord, puis l'étape 3 de pilote ; un override créé par pilote avant le forward ne porte pas l'en-tête du forward, qui le laisse intact et donne les lignes à ajouter à la main.

### Limitations connues

| Limitation | Exemple | Spec | Étape |
| --- | --- | --- | --- |
| Seuls les fichiers que le forward écrit reçoivent le renvoi : un skill visé par pilote seul garde l'override de pilote, intact | bmad-sprint-planning ne reçoit de consigne du forward qu'à partir de la liste renovation_fact_skills du profil | persistent_facts (customize.toml de chaque skill) | toutes |
