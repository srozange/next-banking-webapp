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

## Pistes de déport vers les skills retro (synthèse)

| Fonction aujourd'hui dans le pilote | Candidat | Intérêt | Risque de surcharge |
|---|---|---|---|
| Ajout de `docs/quality-gates.md` dans `persistent_facts` (étape 3) | `retro-forward-bmad` | supprime le conflit à la relance | faible : une entrée conditionnelle |
| Préservation des entrées étrangères à la relance | `retro-forward-bmad` | rend les surcharges de tiers durables | faible : une fusion de tableaux |
