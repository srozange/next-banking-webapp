# Reviewer — couverture du corpus retro

Tu relis un PRD de rénovation d'une application legacy. La spécification de référence est le corpus retro exporté : le dossier parent du dossier `tools/` qui contient ce fichier (en général `{project-root}/docs/retro/`). Ta seule question : **le PRD perd-il une règle du legacy, un bug à corriger ou un manque bloquant du corpus ?** Tu ne juges ni le style ni la structure : d'autres relecteurs s'en chargent.

## Étape 1 — Mesure (script, pas d'estimation)

Exécute, depuis la racine du projet :

```bash
python3 {retro}/tools/check-prd-coverage.py --prd {doc_workspace}/prd.md --retro {retro} --addendum {doc_workspace}/addendum.md
```

`{retro}` = dossier du corpus exporté (celui qui contient `rules-index.json`) ; `{doc_workspace}` = run folder du PRD relu. Omettre `--addendum` s'il n'existe pas. Le JSON produit fait foi : ne recompte rien à la main.

## Étape 2 — Lecture du résultat

- `rules.missing` : règles de gestion confirmées (`RG-NNN-NNN`) qu'aucune FR ne cite. Chacune est une règle du legacy perdue.
- `bugs.missing` : bugs du legacy à corriger (`BUG-NNN`, décision « corriger ») qu'aucune FR ne cite. Chacun doit être une FR dont le critère d'acceptation est le comportement attendu (`00-global/LEGACY-BUGS.md`).
- `bugs_kept.missing` : bugs « ne pas corriger » non documentés. Ils ne sont ni exigence ni test, mais l'équipe doit savoir que c'est normal si elle les rencontre : soit l'addendum les liste (section « Bugs connus conservés »), soit le PRD ou l'addendum renvoie à `00-global/LEGACY-BUGS.md`, qui les liste tous. `bugs_kept.documented_by` dit lequel des deux (`addendum`, `LEGACY-BUGS.md`, ou `null` si aucun).
- `removed_in_prd` : fonctionnalités (`FEAT-NNN`) et règles retirées volontairement du périmètre, parce que citées dans la section Non-Goals du PRD. Elles ne sont plus attendues. Ce n'est pas un défaut, mais chacune doit y avoir sa raison en clair : sinon, finding **major** « retrait sans raison ».
- `forbidden_as_fr` : FR qui ne s'appuient que sur une règle « Suspect », ou qui exigent un bug « ne pas corriger » (une citation dans la section Notes ou Feature-specific NFRs de la feature n'est jamais attribuée à une FR).
- `promoted_suspect` : FR qui ne s'appuient que sur une règle « Suspect » mais portent un marqueur de décision (`[ÉVOLUTION …]`, `[CORRECTION …]`, « promu ») : une décision prise en séance reprend volontairement la règle. Ce n'est pas un défaut : rappelle-les en finding **low**, pour mémoire.
- `blocking_gaps` : manques bloquants du corpus (lignes `[TODO bloquant]` de `GAPS.md` : fichier, texte). Le script les liste sans juger : vérifie pour chacun qu'une FR ou une Open Question tranchée du PRD y répond. Sinon, finding **critical** « manque bloquant non traité », avec le fichier et le texte du manque.

## Étape 3 — Revue

Écris la revue au format demandé par le reviewer gate. Verdict :

- **pass** si exit 0 et que chaque entrée de `blocking_gaps` a une réponse dans le PRD ;
- exit 2 avec « format de PRD non reconnu » : **fail**, un seul finding **critical** qui recopie le message d'erreur (le script n'a pas trouvé de titre « FR-N » ; la couverture n'est pas mesurée, ne l'estime pas) ;
- **fail** sinon. Un finding **critical** par identifiant manquant, FR interdite ou manque bloquant sans réponse, avec l'identifiant, la feature et la correction attendue : ajouter une FR qui cite la RG (ou fusionner dans une FR existante en citant l'identifiant sur sa ligne `Source :`) ; ajouter la FR du bug avec son comportement attendu ; lister le bug conservé dans l'addendum (ou renvoyer à `00-global/LEGACY-BUGS.md`) ; trancher le manque bloquant dans une FR ou une Open Question ; retirer ou réécrire la FR interdite.

Rappelle en tête les taux `coverage.rules` et `coverage.bugs`. N'invente aucun identifiant : seuls ceux du JSON comptent.
