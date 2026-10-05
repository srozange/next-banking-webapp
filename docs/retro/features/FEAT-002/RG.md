---
feature: FEAT-002
name: Tableau de bord
status: current
generated_by: retro-rules
date: 2026-10-03
---

# Règles de gestion — FEAT-002 Tableau de bord

Périmètre : `dashboard.xhtml` + `DashboardBean.java` + `NotificationStore.java`
+ `VirementProcessorMDB.java` (génération des notifications affichées ici)
+ `SecurityBean.java` (filtrage navigation)

---

## Cycle de vie de la page

RG-002-001 [À confirmer]
WHEN `dashboard.xhtml` is loaded, THE SYSTEM SHALL re-fetch the current customer's data
from the database using the username stored in the HTTP session and update the session
with the refreshed record.

RG-002-002 [Suspect]
IF the database no longer contains the user whose username is stored in the HTTP session
THEN THE SYSTEM SHALL handle the absence gracefully without exposing an error to the user.

---

## Soldes et comptes Demo Bank

RG-002-003 [À confirmer]
WHEN the dashboard loads, THE SYSTEM SHALL display the total balance as the arithmetic
sum of the balances of all bank accounts belonging to the connected customer.

RG-002-004 [À confirmer]
IF the connected user has no bank accounts THEN THE SYSTEM SHALL display a total balance
of EUR 0.00.

RG-002-005 [À confirmer]
WHEN the dashboard loads, THE SYSTEM SHALL display, for each account of the connected
user, the account type label, its current balance formatted in EUR with 2 decimal places
and thousands separator, and its account number.

---

## Dernières opérations

RG-002-006 [À confirmer]
WHEN the dashboard loads and the connected user has at least one account, THE SYSTEM
SHALL retrieve and display the 5 most recent transactions of the first account returned
by the persistence layer for that user.

RG-002-007 [Suspect]
WHEN displaying recent transactions, THE SYSTEM SHALL draw them from the COURANT account.

RG-002-008 [À confirmer]
WHEN displaying recent transactions, THE SYSTEM SHALL order them from most recent to
oldest (ORDER BY transactionDate DESC).

RG-002-009 [À confirmer]
WHEN displaying a transaction, THE SYSTEM SHALL format its date as dd/MM/yyyy HH:mm.

RG-002-010 [À confirmer]
WHEN displaying a transaction, THE SYSTEM SHALL show the transaction type as either
DEBIT or CREDIT.

---

## Centre de notifications (ActiveMQ Artemis)

RG-002-011 [À confirmer]
WHEN the dashboard loads for a logged-in user, THE SYSTEM SHALL display only the
notifications stored for that user's username, in most-recent-first order.

RG-002-012 [À confirmer]
IF the notification store contains no entry for the current user THEN THE SYSTEM SHALL
display the message "Aucune notification - effectuez un virement pour voir les messages
publiés via ActiveMQ Artemis en temps réel".

RG-002-013 [À confirmer]
WHEN the current user has at least one unread notification, THE SYSTEM SHALL display
a badge showing the count of unread notifications.

RG-002-014 [À confirmer]
WHEN the current user has at least one unread notification, THE SYSTEM SHALL render
a "Tout marquer lu" button.

RG-002-015 [À confirmer]
WHEN the current user has at least one notification (read or unread), THE SYSTEM SHALL
render an "Effacer tout" button.

RG-002-016 [À confirmer]
WHEN the user clicks "Tout marquer lu", THE SYSTEM SHALL mark all stored notifications
for the current user as read.

RG-002-017 [À confirmer]
WHEN the user clicks "Effacer tout", THE SYSTEM SHALL remove all stored notifications
for the current user from the notification store.

---

## Règles du store de notifications (NotificationStore)

RG-002-018 [À confirmer]
WHEN a new notification is added for a user, THE SYSTEM SHALL prepend it at the head
of that user's notification list so that the most recent notification appears first.

RG-002-019 [À confirmer]
IF the number of stored notifications for a user reaches maxNotificationsParUser
(default: 20, configurable via MAX_NOTIFS_PAR_USER environment variable) THEN
THE SYSTEM SHALL evict the oldest notification when adding a new one to stay within
the limit.

RG-002-020 [Suspect]
THE SYSTEM SHALL persist notification data across server restarts.

---

## Génération des notifications (VirementProcessorMDB)

RG-002-021 [À confirmer]
IF a processed transfer amount is strictly less than seuilAlerteEur (default: 5 000 EUR,
configurable via SEUIL_ALERTE_EUR) THEN THE SYSTEM SHALL add one CONFIRMATION notification
to the sender's notification store.

RG-002-022 [À confirmer]
IF a processed transfer amount is greater than or equal to seuilAlerteEur (default:
5 000 EUR) AND strictly less than seuilFraudeEur (default: 10 000 EUR, configurable
via SEUIL_FRAUDE_EUR) THEN THE SYSTEM SHALL add one ALERTE notification to the sender's
notification store.

RG-002-023 [À confirmer]
IF a processed transfer amount is greater than or equal to seuilFraudeEur (default:
10 000 EUR) THEN THE SYSTEM SHALL add one FRAUDE notification to the sender's
notification store.

RG-002-024 [Suspect]
WHEN a transfer is processed, THE SYSTEM SHALL generate exactly one notification per
transfer (CONFIRMATION, ALERTE, or FRAUDE — mutually exclusive, never a combination).

---

## Navigation contextuelle

RG-002-025 [À confirmer]
WHEN the dashboard navigation bar is rendered, THE SYSTEM SHALL display the "Espace
Conseiller" link only to users with role CONSEILLER or ADMIN.

RG-002-026 [À confirmer]
WHEN the dashboard navigation bar is rendered, THE SYSTEM SHALL display the
"Administration" link only to users with role ADMIN.

---

## Affichage patrimoine total (Open Banking Powens)

RG-002-027 [À confirmer]
WHEN the connected user has an active Powens connection, THE SYSTEM SHALL display a
"Patrimoine total" section on the dashboard showing the sum of the Demo Bank balance
and the total balance of all external accounts aggregated via Powens.

RG-002-028 [À confirmer]
WHEN the patrimoine total section is displayed, THE SYSTEM SHALL calculate it as
dashboardBean.totalBalance + powensBean.totalBalance.
