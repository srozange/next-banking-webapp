---
feature: FEAT-025
name: "Système de notifications internes"
interface: Transversal
status: current
generated_by: retro-rules
date: 2026-10-03
---

# RG — FEAT-025 Système de notifications internes

## Contexte architectural

Deux pipelines coexistent pour les notifications :

1. **Pipeline in-memory actif** : VirementProcessorMDB / LoanApprovalMDB → `NotificationStore` (CDI `@ApplicationScoped`, `ConcurrentHashMap`) → `DashboardBean` → `dashboard.xhtml`. Ce pipeline est celui affiché aux utilisateurs.
2. **Pipeline JPA (partiellement mort)** : `NotificationService` (`@Stateless` EJB) → table `notifications` (JPA, `SINGLE_TABLE`). Seul `checkLowBalances()` (`@Schedule` toutes les 30 min) alimente ce pipeline. Le consommateur prévu (`NotificationBean`, FEAT-032) est du code mort — aucune page XHTML ne le référence.

---

## Règles de gestion

### Notifications virement (VirementProcessorMDB → NotificationStore)

RG-025-001 [Confirmé]
WHEN a virement is processed asynchronously via the JMS queue AND
the amount is strictly less than SEUIL_ALERTE_EUR (default 5000 EUR, configurable via env var `SEUIL_ALERTE_EUR`)
THE SYSTEM SHALL add a CONFIRMATION notification to the originating user's in-memory notification store,
with a message including the amount and the target account number.

RG-025-002 [Confirmé]
WHEN a virement is processed asynchronously AND
the amount is greater than or equal to SEUIL_ALERTE_EUR (default 5000 EUR) AND
strictly less than SEUIL_FRAUDE_EUR (default 10000 EUR)
THE SYSTEM SHALL add an ALERTE notification indicating the transaction is pending validation.

RG-025-003 [Confirmé]
WHEN a virement is processed asynchronously AND
the amount is greater than or equal to SEUIL_FRAUDE_EUR (default 10000 EUR)
THE SYSTEM SHALL add a FRAUDE notification indicating the transfer is blocked for fraud verification.

RG-025-004 [Confirmé]
IF the virement amount is below SEUIL_ALERTE_EUR (CONFIRMATION case) AND the motif field is non-empty
THEN THE SYSTEM SHALL append the motif to the confirmation notification message.

### Notifications prêt (LoanApprovalMDB → NotificationStore)

RG-025-005 [Confirmé]
WHEN a loan request is approved by the credit scoring MDB
THE SYSTEM SHALL add a CONFIRMATION notification to the requesting user's in-memory notification store,
including the loan type and requested amount.

RG-025-006 [Confirmé]
WHEN a loan request is refused by the credit scoring MDB
THE SYSTEM SHALL add an ALERTE notification to the requesting user's in-memory notification store,
stating that the request was refused due to exceeded ceiling or excessive duration.

### Gestion du store et affichage (NotificationStore / DashboardBean)

RG-025-007 [Confirmé]
WHEN a notification is added to a user's store AND
the total count exceeds MAX_NOTIFS_PAR_USER (default 20, configurable via env var `MAX_NOTIFS_PAR_USER`)
THE SYSTEM SHALL discard the oldest notification to keep the list within the cap.

RG-025-008 [Confirmé]
WHEN the dashboard is displayed
THE SYSTEM SHALL show the authenticated user's in-memory notifications in reverse chronological order
(most recent first).

RG-025-009 [Confirmé]
WHEN the user marks all notifications as read
THE SYSTEM SHALL set the read flag to true on every in-memory notification for that user.

RG-025-010 [Confirmé]
WHEN the user clears all notifications
THE SYSTEM SHALL remove all in-memory notifications for that user from the store.

### Surveillance soldes faibles (NotificationService @Schedule — pipeline JPA)

RG-025-011 [Suspect]
WHEN the periodic low-balance scan runs (every 30 minutes, `@Schedule(hour="*", minute="*/30")`) AND
an active customer's account balance is strictly less than 100 EUR (hardcoded)
THE SYSTEM SHALL notify the customer of a low balance with HIGH priority.

RG-025-012 [Suspect]
IF the low-balance threshold is intended to be configurable per customer or via environment variable
THEN THE SYSTEM SHALL apply the configured value.

### Code mort (intention connue)

RG-025-013 [Suspect]
WHEN a caller invokes the deprecated `sendNotification(customerId, message)` method
THE SYSTEM SHALL route the request to `createSystemNotification()` and process the notification.

RG-025-014 [Suspect]
IF a transaction notification is created AND the amount is greater than 1000 EUR
THEN THE SYSTEM SHALL assign HIGH priority to the notification.

---

## Notes d'architecture (non-règles)

- `maxNotificationsPerDay = 20` déclaré en champ d'instance de NotificationService avec commentaire « HACK: prevent spam but hardcoded — FIXME » : champ jamais lu dans la classe — aucune limite par jour n'est effectivement appliquée par le pipeline JPA.
- La limite effective est `maxNotificationsParUser` (AppConfig) appliquée par NotificationStore (pipeline in-memory uniquement).
- L'héritage JPA utilise `SINGLE_TABLE` avec discriminateur `notif_disc` : sous-types `TransactionNotification`, `LoanNotification`, `SystemNotification`.
