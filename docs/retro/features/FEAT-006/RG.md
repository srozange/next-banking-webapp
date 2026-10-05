---
feature: FEAT-006
name: Budget et catégories de dépenses
produced_by: retro-rules
date: 2026-10-03
---

# RG — FEAT-006 : Budget et catégories de dépenses

Sources primaires : `src/main/java/com/accenture/bank/web/BudgetBean.java`,
`src/main/java/com/accenture/bank/ejb/BudgetService.java`,
`src/main/java/com/accenture/bank/entity/Budget.java`,
`src/main/java/com/accenture/bank/entity/BudgetCategory.java`,
`src/main/webapp/budget.xhtml`.

---

## Périmètre et données

RG-006-001 [À confirmer]
THE SYSTEM SHALL scope all budget data (plafonds, spending breakdown, totals)
to the authenticated user's own customer account only.

---

## Sélection de la période

RG-006-002 [À confirmer]
WHEN the budget page initializes AND the authenticated user has no transaction history,
THE SYSTEM SHALL set the selected period to the current calendar month.

RG-006-003 [À confirmer]
WHEN the budget page initializes AND the authenticated user has transaction history,
THE SYSTEM SHALL set the selected period to the most recent month with transactions.

RG-006-004 [À confirmer]
THE SYSTEM SHALL limit the selectable period list to the 24 most recent months
that contain at least one transaction for the authenticated user.

RG-006-005 [À confirmer]
WHEN the user changes the period selector value,
THE SYSTEM SHALL immediately reload all budget and spending data
without requiring an explicit submit action.

---

## Ventilation des dépenses par catégorie

RG-006-006 [À confirmer]
THE SYSTEM SHALL compute the category spending breakdown
from DEBIT transactions only for the selected customer and month.

RG-006-007 [À confirmer]
WHEN computing the category of a transaction,
THE SYSTEM SHALL match the transaction description against each BudgetCategory's keywords
case-insensitively and assign the transaction to the first matching category.

RG-006-008 [À confirmer]
WHEN a transaction description matches no category keywords,
THE SYSTEM SHALL assign the transaction to the "Autres" category.

RG-006-009 [À confirmer]
THE SYSTEM SHALL display only categories with a non-zero spending amount
in the monthly breakdown view.

RG-006-010 [À confirmer]
WHEN the authenticated user has no spending for the selected period,
THE SYSTEM SHALL display "Aucune depense enregistree sur cette periode".

RG-006-011 [À confirmer]
THE SYSTEM SHALL compute and display the total monthly expenses
as the sum of all DEBIT transaction amounts for the authenticated user and selected month.

RG-006-012 [À confirmer]
THE SYSTEM SHALL compute and display the total monthly revenues
as the sum of all CREDIT transaction amounts for the authenticated user and selected month.

---

## Définition et gestion des plafonds

RG-006-013 [À confirmer]
IF the user submits the plafond form without selecting a category,
THEN THE SYSTEM SHALL reject the submission and display
"Veuillez selectionner une categorie".
  Champ   : editCategory (FEAT-006/budgetForm)
  Message : "Veuillez selectionner une categorie"

RG-006-014 [À confirmer]
IF the user submits the plafond form with an empty or non-positive plafond value,
THEN THE SYSTEM SHALL reject the submission and display
"Veuillez saisir un plafond valide".
  Champ   : editPlafond (FEAT-006/budgetForm)
  Message : "Veuillez saisir un plafond valide"

RG-006-015 [À confirmer]
THE SYSTEM SHALL enforce uniqueness of at most one budget per combination of
(customer, category, month).

RG-006-016 [À confirmer]
IF a budget already exists for the given customer, category and month,
THEN THE SYSTEM SHALL update its plafond with the new value.

RG-006-017 [À confirmer]
IF no budget exists for the given customer, category and month,
THEN THE SYSTEM SHALL create a new budget entry with the provided plafond.

RG-006-018 [À confirmer]
WHEN a plafond is saved successfully,
THE SYSTEM SHALL display "Plafond enregistre pour {categorie}",
reset the category and plafond form fields, and refresh the budgets list.

RG-006-019 [À confirmer]
WHEN the user activates the "Supprimer" action on a budget entry,
THE SYSTEM SHALL delete the entry immediately without displaying a confirmation dialog.

---

## Indicateurs visuels de progression

RG-006-020 [À confirmer]
WHEN a category has a plafond defined and its spending is below 80% of the plafond,
THE SYSTEM SHALL display the category progress bar in green.

RG-006-021 [À confirmer]
WHEN a category has a plafond defined and its spending is between 80% and 99%
of the plafond inclusive,
THE SYSTEM SHALL display the category progress bar in orange (warning state).

RG-006-022 [À confirmer]
WHEN a category has a plafond defined and spending reaches or exceeds 100%
of the plafond,
THE SYSTEM SHALL display the category progress bar in red (over-budget state).

RG-006-023 [À confirmer]
THE SYSTEM SHALL cap the displayed progress bar fill percentage at 100%
even when spending exceeds the plafond.

RG-006-024 [À confirmer]
WHEN a category's spending strictly exceeds its plafond,
THE SYSTEM SHALL display a "Depassement" badge, highlight the row with a pink background,
and display the spending amount in red.

RG-006-025 [À confirmer]
WHEN a category has no plafond defined,
THE SYSTEM SHALL display "Aucun plafond defini" in the progress indicator area.

---

## Tableau des plafonds

RG-006-026 [À confirmer]
THE SYSTEM SHALL display the plafonds summary table only when at least one budget
is defined for the selected month.

RG-006-027 [À confirmer]
WHEN the plafonds summary table is displayed, THE SYSTEM SHALL show for each budget row
the ecart (plafond minus spending) in red when negative and in green when non-negative.

---

## Suspect

RG-006-028 [Suspect]
WHEN the user defines a plafond for the "Revenus" budget category,
THE SYSTEM SHALL compare it against spending computed from DEBIT transactions matching
revenue keywords — resulting in a spending value of zero under normal conditions.
