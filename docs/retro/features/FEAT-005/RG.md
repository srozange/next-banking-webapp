---
feature: FEAT-005
name: Relevé de compte
step: retro-rules
doc_status: current
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# RG — FEAT-005 Relevé de compte

_Extrait de : `ReleveBean.java`, `AuditService.java`, `AccountService.java`, `Transaction.java`,
`BankAccount.java`, `AppFilter.java`, `SecurityBean.java`, `releve.xhtml`, `TransferService.java`._
_Entrées : `FIELDS.md`, `fields-raw.json`._

---

## Accès et initialisation

RG-005-001 [Confirmé]
WHEN a user requests releve.xhtml without an active session
THE SYSTEM SHALL redirect the user to login.xhtml.

RG-005-002 [Confirmé]
WHEN releve.xhtml is initialised for an authenticated user
THE SYSTEM SHALL load only the bank accounts belonging to that user's customer record.

RG-005-003 [Confirmé]
WHEN releve.xhtml is initialised and the customer has at least one account
THE SYSTEM SHALL auto-select the first account in the list and load its transactions.

RG-005-004 [Confirmé]
WHEN releve.xhtml is initialised and the customer has no account
THE SYSTEM SHALL render the account selector with an empty options list and display no transactions.

---

## Sélection de compte

RG-005-005 [Confirmé]
WHEN the user changes the account selection
THE SYSTEM SHALL reload the customer's account list (refreshed balances), then load the transactions
for the newly selected account.
  Champ   : releveForm:accountSelect (FEAT-005/SCR-005-01)

RG-005-006 [Confirmé]
WHEN the user changes the account selection
THE SYSTEM SHALL refresh the accountInfo panel and the transactionPanel without a full page reload.
  Champ   : releveForm:accountSelect (FEAT-005/SCR-005-01)

---

## Panneau d'informations du compte

RG-005-007 [Confirmé]
IF a selected account exists THEN THE SYSTEM SHALL display the account type, account number,
and current balance in the accountInfo panel.

RG-005-008 [Confirmé]
WHEN the current balance is displayed in the accountInfo panel
THE SYSTEM SHALL format it as a currency amount in EUR with at least 2 decimal places.

---

## Chargement des transactions

RG-005-009 [À confirmer]
WHEN transactions are loaded for an account
THE SYSTEM SHALL retrieve all transactions for that account without any result limit.

RG-005-010 [À confirmer]
WHEN transactions are loaded for an account
THE SYSTEM SHALL order them by transaction date descending (most recent first).

---

## Affichage du tableau des transactions

RG-005-011 [Confirmé]
IF the transaction list for the selected account is empty
THEN THE SYSTEM SHALL display the message "Aucune transaction pour ce compte.".
  Message : "Aucune transaction pour ce compte."

RG-005-012 [Confirmé]
WHEN the transaction list is non-empty
THE SYSTEM SHALL display a counter "N opération(s)" above the transaction table,
where N is the total number of transactions loaded.

RG-005-013 [Confirmé]
WHEN a transaction date is displayed
THE SYSTEM SHALL format it as dd/MM/yyyy HH:mm.

RG-005-014 [Confirmé]
WHEN a transaction amount is displayed
THE SYSTEM SHALL prefix it with "+" for CREDIT transactions and "−" for DEBIT transactions,
and format it in EUR with at least 2 decimal places.
            `f:convertNumber type="currency" currencySymbol="€" minFractionDigits="2"`

RG-005-015 [Confirmé]
WHEN the balance-after value is displayed for a transaction
THE SYSTEM SHALL format it in EUR with at least 2 decimal places, without a sign prefix.

RG-005-016 [Confirmé]
WHEN the type of a transaction is displayed
THE SYSTEM SHALL render "CREDIT" in green (#2e7d32) and "DEBIT" in red (#E3000F).

---

## Transactions en attente

RG-005-017 [Confirmé]
IF a transaction description starts with the prefix "[EN ATTENTE] " (12 chars + 1 trailing space)
THEN THE SYSTEM SHALL display a badge "En attente" before the description,
strip the 13-character prefix from the displayed description text,
and render the row with a yellow background.

---

## Navigation conditionnelle

RG-005-018 [Confirmé]
IF the authenticated user has role CONSEILLER or ADMIN
THEN THE SYSTEM SHALL display the "Espace Conseiller" navigation link on the relevé page.

RG-005-019 [Confirmé]
IF the authenticated user has role ADMIN
THEN THE SYSTEM SHALL display the "Administration" navigation link on the relevé page.
