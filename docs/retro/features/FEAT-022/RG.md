---
feature: FEAT-022
name: "Surveillance soldes faibles et génération d'alertes"
interface: Batch
status: current
extracted_at: "2026-10-03"
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# RG-022 — Surveillance soldes faibles et génération d'alertes

> Feature batch sans interface utilisateur — aucun FIELDS.md ni fields-raw.json attendu.
> Source principale : `src/main/java/com/accenture/bank/ejb/NotificationService.java`

---

## Règles de gestion

RG-022-001 [À confirmer]
WHEN the EJB timer fires every 30 minutes
THE SYSTEM SHALL scan all active customers' accounts for low-balance conditions.
  Note    : le timer est non-persistant (`persistent=false`) — il ne survit pas à un redémarrage du serveur

RG-022-002 [À confirmer]
WHEN the periodic scan runs
THE SYSTEM SHALL restrict the scan to customers with the `active` flag set to `true`.

RG-022-003 [À confirmer]
WHEN scanning a customer's accounts for low balances
THE SYSTEM SHALL inspect all accounts linked to that customer, regardless of account type.

RG-022-004 [À confirmer]
IF an account's balance is strictly below 100 €
THEN THE SYSTEM SHALL create a low-balance alert notification for the account owner.
  Note    : seuil hardcodé — TODO JIRA-7001 : rendre configurable par client

RG-022-005 [À confirmer]
WHEN a low-balance notification is created
THE SYSTEM SHALL assign it the type `ACCOUNT_BALANCE`, the priority `HIGH`, the title "Solde insuffisant sur votre compte", and a message body that includes the current balance and the threshold value in euros.

RG-022-006 [À confirmer]
IF an account's balance is null
THEN THE SYSTEM SHALL skip that account without generating an alert.

RG-022-007 [À confirmer]
IF loading the list of active customers fails
THEN THE SYSTEM SHALL abort the entire batch scan for that execution and log the error.

RG-022-008 [À confirmer]
IF loading accounts for a specific customer fails
THEN THE SYSTEM SHALL skip that customer, log a warning, and continue processing remaining customers.

RG-022-009 [À confirmer]
IF creating a low-balance notification for an account fails
THEN THE SYSTEM SHALL log a warning, skip that account, and continue processing remaining accounts.

RG-022-010 [Suspect]
WHEN a customer's account balance is below the threshold
THE SYSTEM SHALL generate at most 20 low-balance notifications per account per day.

RG-022-011 [Suspect]
WHEN the periodic low-balance scan completes
THE SYSTEM SHALL record the execution result in the batch job execution log.
