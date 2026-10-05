---
feature: FEAT-011
name: Catalogue produits bancaires
status: current
extracted_at: 2026-10-03
extractor: retro-rules
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# Règles de gestion — FEAT-011 « Catalogue produits bancaires »

---

## Accès et périmètre

RG-011-001 [Confirmé]
THE SYSTEM SHALL grant access to the product catalogue page to any authenticated user regardless of role (CLIENT, CONSEILLER, ADMIN).

RG-011-002 [Confirmé]
THE SYSTEM SHALL present the product catalogue as a read-only view with no user input fields.

---

## Chargement et rafraîchissement

RG-011-003 [Confirmé]
WHEN an authenticated user session starts, THE SYSTEM SHALL load the product catalogue and the batch job history.

RG-011-004 [Confirmé]
WHEN the user activates the refresh action on the product catalogue page, THE SYSTEM SHALL reload the product catalogue and the batch job history and remain on the same page.

---

## Sélection et tri des produits

RG-011-005 [Confirmé]
WHEN the product catalogue page is displayed, THE SYSTEM SHALL include only bank products whose active flag is true.

RG-011-006 [Confirmé]
WHEN the product catalogue page is displayed, THE SYSTEM SHALL display bank products sorted alphabetically by name.

RG-011-007 [Confirmé]
WHEN displaying the product catalogue, THE SYSTEM SHALL organize products into three sections — Epargne, Credit, Investissement — filtered server-side by the product type label.

---

## Affichage des champs produits

RG-011-008 [Confirmé]
IF a product's optional field (interest rate, min opening deposit, max LTV, market sector, expected return, or max duration for credit products) is null
THEN THE SYSTEM SHALL display "—" in place of the missing value.

RG-011-009 [Confirmé]
IF a SavingsProduct's max duration in months is null THEN THE SYSTEM SHALL display "Illimite".
  Note    : exception à RG-011-008 — la durée max d'un produit d'épargne null affiche "Illimite" et non "—"

RG-011-010 [Suspect]
WHEN displaying a product's interest rate or expected return, THE SYSTEM SHALL display the value as a percentage (e.g. 3,00 %).

RG-011-011 [Confirmé]
WHEN displaying an investment product, THE SYSTEM SHALL show its risk level as the label followed by the score out of 5,
using the scale: 1=Tres faible, 2=Faible, 3=Moyen, 4=Eleve, 5=Tres eleve (format: "{label} ({score}/5)").

RG-011-012 [Confirmé]
THE SYSTEM SHALL create a SavingsProduct with capital guarantee set to true by default.

RG-011-013 [Confirmé]
THE SYSTEM SHALL create a CreditProduct with insurance requirement set to false by default.

---

## États vides

RG-011-014 [Confirmé]
IF no products of a given type (savings, credit, or investment) are available
THEN THE SYSTEM SHALL display an informational empty-state message in the corresponding section.

RG-011-015 [Confirmé]
IF no batch jobs have been recorded THEN THE SYSTEM SHALL display an informational empty-state message in the batch history section.

---

## Historique des traitements nocturnes

RG-011-016 [Confirmé]
WHEN displaying batch job history, THE SYSTEM SHALL show at most 50 records ordered from the most recent to the oldest start time.

RG-011-017 [À confirmer]
WHEN recording a batch job result, THE SYSTEM SHALL set the job status to one of the values SUCCESS, FAILED, or PARTIAL.

RG-011-018 [Confirmé]
WHEN displaying a batch job execution, THE SYSTEM SHALL compute and display the duration as endTime minus startTime in milliseconds.

RG-011-019 [Confirmé]
IF a batch job's startTime or endTime is absent THEN THE SYSTEM SHALL display a duration of 0.

---

## Opérations de service (typage par catégorie)

RG-011-020 [À confirmer]
THE SYSTEM SHALL be able to retrieve bank products filtered by individual type (savings, credit, or investment).
    Portée : information
