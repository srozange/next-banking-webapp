---
feature: FEAT-015
name: "Espace Conseiller — Portefeuille clients"
status: current
extracted: 2026-10-03
---

# Règles de gestion — FEAT-015 Espace Conseiller — Portefeuille clients

## Contrôle d'accès

RG-015-001 [Confirmé]
IF an unauthenticated user requests conseiller-workspace.xhtml THEN THE SYSTEM SHALL redirect to login.xhtml.

RG-015-002 [À confirmer]
IF a user with role CLIENT requests conseiller-workspace.xhtml THEN THE SYSTEM SHALL silently redirect to dashboard.xhtml without displaying an error message.

RG-015-003 [À confirmer]
IF a user with role CONSEILLER or role ADMIN requests conseiller-workspace.xhtml THEN THE SYSTEM SHALL display the Espace Conseiller portfolio page.

## Chargement du portefeuille

RG-015-004 [À confirmer]
WHEN a CONSEILLER or ADMIN user opens conseiller-workspace.xhtml for the first time in a session THE SYSTEM SHALL load the list of clients whose assigned adviser matches the authenticated user, ordered alphabetically by fullName.

RG-015-005 [À confirmer]
THE SYSTEM SHALL include in the portfolio only clients whose advisor field references the authenticated user's customer record.

## Tableau de bord KPI

RG-015-006 [À confirmer]
WHEN the portfolio is loaded THE SYSTEM SHALL display the count of clients in the portfolio as KPI "Mes clients".

RG-015-007 [À confirmer]
WHEN the portfolio is loaded THE SYSTEM SHALL display the cumulative total of all account balances of all portfolio clients as KPI "Encours total".

RG-015-008 [À confirmer]
WHEN the portfolio is loaded THE SYSTEM SHALL display the total count of loans with status EN_ATTENTE across all portfolio clients as KPI "Prets en attente".

## Affichage du portefeuille — liste des clients

RG-015-009 [À confirmer]
IF the adviser's portfolio is empty THEN THE SYSTEM SHALL display the message "Aucun client dans votre portefeuille. Contactez l administrateur pour l affectation des clients."

RG-015-010 [À confirmer]
WHEN the portfolio is not empty THE SYSTEM SHALL display one card per client showing: the first character of fullName as avatar, fullName, @username, and an active/inactive status badge.

RG-015-011 [À confirmer]
WHEN the portfolio is displayed THE SYSTEM SHALL show on each client card the client's total balance, number of bank accounts, count of EN_COURS loans, and count of EN_ATTENTE loans.

RG-015-012 [À confirmer]
IF a client has at least one loan with status EN_ATTENTE THEN THE SYSTEM SHALL display a warning indicator showing the count of pending loans on that client's card.

RG-015-013 [À confirmer]
IF a client has no loans with status EN_ATTENTE AND has at least one loan with status EN_COURS THEN THE SYSTEM SHALL display a positive indicator showing the count of active loans on that client's card.

RG-015-014 [À confirmer]
IF a client's active status is false THEN THE SYSTEM SHALL apply a visual inactive distinction to that client's card.

## Panneau de détail client

RG-015-015 [À confirmer]
WHEN the user selects a client by clicking "Voir detail" THE SYSTEM SHALL display the client detail panel inline on the same page without navigation.

RG-015-016 [À confirmer]
WHEN a client detail panel is open THE SYSTEM SHALL display the client's fullName, @username, email, and phone.

RG-015-017 [À confirmer]
WHEN a client detail panel is open THE SYSTEM SHALL display the client's bank accounts with accountNumber, accountType, and balance.

RG-015-018 [À confirmer]
WHEN a client detail panel is open THE SYSTEM SHALL display the client's loans with type, montant, mensualite, and status.

RG-015-019 [À confirmer]
IF the selected client has no bank accounts THEN THE SYSTEM SHALL display "Aucun compte" in the accounts section of the detail panel.

RG-015-020 [À confirmer]
IF the selected client has no loans THEN THE SYSTEM SHALL display "Aucun pret" in the loans section of the detail panel.

RG-015-021 [À confirmer]
WHEN the user clicks "Fermer" in the client detail panel THE SYSTEM SHALL hide the detail panel and return to the portfolio list view without navigation.

## Valeurs de référence

RG-015-022 [À confirmer]
THE SYSTEM SHALL support the following bank account types: COURANT, EPARGNE.

RG-015-023 [À confirmer]
THE SYSTEM SHALL support the following loan types: IMMOBILIER, CONSOMMATION, TRAVAUX.

RG-015-024 [À confirmer]
THE SYSTEM SHALL support the following loan statuses: EN_ATTENTE, EN_COURS, REMBOURSE, DEFAUT, REFUSE.

## Rafraîchissement et dette technique

RG-015-025 [Suspect]
THE SYSTEM SHALL allow the adviser to refresh their portfolio during the session to reflect changes made since the initial page load.

RG-015-026 [Suspect]
THE SYSTEM SHALL display each loan status with a consistent visual badge covering all supported statuses.
