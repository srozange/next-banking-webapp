---
feature: FEAT-006
name: Budget et catégories de dépenses
produced_by: retro-validation
date: 2026-10-03
---

# VALIDATION — FEAT-006 : Budget et catégories de dépenses

Sources primaires : `BudgetBean.java`, `BudgetService.java`, `Budget.java`, `BudgetCategory.java`, `budget.xhtml`, `retro/00-global/DATA-DICTIONARY.md`.

Champs validables (formulaire `budgetForm`) : `editCategory`, `editPlafond`.
Champs sans validation : `selectedMonth` (combobox avec options serveur — aucune validation possible par l'utilisateur).

---

## Validation front (règles UI)

| ID | Champ | Déclencheur | Règle EARS | Check | Message observé |
|---|---|---|---|---|---|
| VAL-006-01-editPlafond-F1 | `editPlafond` | soumission | WHEN user submits budgetForm with non-numeric text in editPlafond THE SYSTEM SHALL display "budgetForm:editPlafond: '{value}' is not a number." | format: numeric | `budgetForm:editPlafond: 'abc' is not a number.` (JSF NumberConverter, observé live) |

Aucune validation HTML5 cliente (attributs `required`, `min`, `maxlength` absents dans le XHTML — confirmé par FIELDS.md et budget.xhtml).

---

## Validation back (règles serveur)

### `editCategory`

| ID | Source | Règle EARS | Check | Message |
|---|---|---|---|---|
| VAL-006-01-editCategory-B1 | applicatif | IF editCategory IS NULL THEN THE SYSTEM SHALL display "Veuillez selectionner une categorie" and abort the save | required | `Veuillez selectionner une categorie` |
| VAL-006-01-editCategory-B2 | schema | THE SYSTEM SHALL reject budget records where CATEGORY IS NULL | required | (erreur DB — masquée par B1) |

Preuve B1 : `editCategory == null` guard avant tout appel service.
Preuve B2 : `DATA-DICTIONARY.md` — `PUBLIC.BUDGETS.CATEGORY VARCHAR NOT NULL`.

### `editPlafond`

| ID | Source | Règle EARS | Check | Message |
|---|---|---|---|---|
| VAL-006-01-editPlafond-B1 | applicatif | IF editPlafond IS NULL THEN THE SYSTEM SHALL display "Veuillez saisir un plafond valide" and abort the save | required | `Veuillez saisir un plafond valide` |
| VAL-006-01-editPlafond-B2 | applicatif | IF editPlafond IS NOT NULL AND editPlafond <= 0 THEN THE SYSTEM SHALL display "Veuillez saisir un plafond valide" and abort the save | custom | `Veuillez saisir un plafond valide` |
| VAL-006-01-editPlafond-B3 | schema | THE SYSTEM SHALL reject budget records where PLAFOND IS NULL | required | (erreur DB — masquée par B1) |
| VAL-006-01-editPlafond-B4 | schema | THE SYSTEM SHALL reject plafond values that exceed DECIMAL(15,2) precision | custom | (erreur DB brute via catch : "Erreur : [message DB]") |

Preuve B1/B2 : `editPlafond == null || editPlafond.compareTo(BigDecimal.ZERO) <= 0`.
Preuve B3/B4 : `DATA-DICTIONARY.md` — `PUBLIC.BUDGETS.PLAFOND DECIMAL(15,2) NOT NULL`.

### `selectedMonth`

Aucune contrainte de validation : le champ est un sélecteur d'options générées côté serveur (`budgetBean.availableMonths`). Toute valeur soumise provient de la liste fournie par le serveur.

---

## Matrice de divergence

| Champ | Verdict | Front | Back | Consequence | Nature |
|---|---|---|---|---|---|
| `editCategory` | `back_seul` | — | VAL-006-01-editCategory-B1, B2 (required) | front accepte ce que back rejette — rejet par le serveur (contrôle présent dans le code) | métier — le champ est obligatoire pour créer un budget ; une réécriture propre ajouterait une validation front explicite |
| `editPlafond` | `back_seul` | — | VAL-006-01-editPlafond-B1, B3 (required) | front accepte ce que back rejette — rejet par le serveur (contrôle présent dans le code) | métier — champ obligatoire ; la réécriture doit garder la règle |
| `editPlafond` | `a_interpreter` | — | VAL-006-01-editPlafond-B2 (strictly > 0), B4 (DECIMAL 15,2) | contraintes `custom` — voir interprétation ci-dessous | voir interprétation |
| `editPlafond` | `front_seul` | VAL-006-01-editPlafond-F1 (format numeric) | — | back accepte ce que front refuse — rejetée par le serveur (observé à l'écran) | métier — le converter JSF est la seule garde explicite pour les saisies texte ; la réécriture doit conserver la validation de format |
| `selectedMonth` | `aucune_règle` | — | — | — | — |

### Interprétation des lignes `a_interpreter` (editPlafond, custom)

**VAL-006-01-editPlafond-B2 — plafond strictement positif (applicatif)**
- Règle : `editPlafond.compareTo(BigDecimal.ZERO) <= 0` → reject.
- Verdict local : `back_seul` — le front n'a pas de contrôle équivalent (pas de `min="0.01"` ou `f:validateDoubleRange`).

**VAL-006-01-editPlafond-B4 — précision DECIMAL(15,2) (schema)**
- Règle : le type DECIMAL(15,2) limite les valeurs à 13 chiffres entiers maximum. Aucun contrôle applicatif de borne supérieure.
- Conséquence legacy : si un utilisateur saisit une valeur dépassant la précision (ex. 99 999 999 999 999,99), le contrôleur ne bloque pas et la violation de schéma DB remonte via le catch générique → `addError("Erreur : [message DB brut]")`.
- Verdict local : `back_seul`, Nature : **dette-legacy** — message d'erreur brut exposé au lieu d'une validation métier propre. Ne pas reproduire. La réécriture doit ajouter une validation explicite de borne supérieure avec un message clair.

---

## Harness de test

- 15 cas générés : 2 `verified`, 13 `probe`.
- Règles non couvertes par un cas harness : VAL-006-01-editCategory-B2, VAL-006-01-editPlafond-B2, VAL-006-01-editPlafond-B3, VAL-006-01-editPlafond-B4, VAL-006-01-editPlafond-F1.
  - B2 et B3 (schema) sont belt-and-suspenders : l'applicatif B1 les masque en conditions normales.
  - B2 (positive custom) et F1 (format) nécessitent des cas dédiés que le harness de sondes standard ne génère pas automatiquement (valeurs 0, négatives, non-numériques).
