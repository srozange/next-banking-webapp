---
feature: FEAT-027
name: Détection de fraude en temps réel
interface: Transversal
status: current
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
generated_by: retro-rules
---

# RG — FEAT-027 « Détection de fraude en temps réel »

Déclencheur transversal appliqué à chaque virement soumis par un CLIENT.
Fichier principal : `ejb/FraudDetectionService.java`.
Entité de statut : `FRAUD_ALERTS` (voir `STATES.md`).

---

## Règles de gestion

### Orchestration

RG-027-001 [Confirmé]
WHEN a customer submits a transfer (VirementBean.executeVirement completes without exception)
THE SYSTEM SHALL invoke FraudDetectionService.checkTransaction() and run all eight fraud detection rules (LARGE_AMOUNT, HIGH_FREQUENCY, NEW_BENEFICIARY, UNUSUAL_HOURS, ROUND_AMOUNT, FOREIGN_IBAN, RAPID_SUCCESSION, VELOCITY_CHECK) for that transaction.

RG-027-002 [Confirmé]
IF the sum of risk scores of all triggered fraud rules exceeds 75
THEN THE SYSTEM SHALL persist one FraudAlert per triggered rule with status OPEN.

RG-027-003 [Suspect]
WHEN fraud alerts are detected for a transfer
THE SYSTEM SHALL reject or flag the transfer before it is committed to the customer's account.
    Portée : bloquant
    Résolution : La détection de fraude ne bloque pas le virement : elle le signale (alertes ouvertes, revues par un administrateur). Elle doit en revanche s'exécuter avant la validation définitive du virement, et une erreur du contrôle ne doit plus être ignorée en silence : elle est journalisée.

RG-027-013 [Confirmé]
WHEN at least one fraud alert is triggered for a completed transfer
THE SYSTEM SHALL display a generic warning message to the customer indicating how many alerts were detected.

---

### Règles de détection — signaux individuels

RG-027-004 [Confirmé]
IF the transfer amount is greater than or equal to 5 000 EUR and strictly less than 15 000 EUR
THEN THE SYSTEM SHALL raise a LARGE_AMOUNT fraud signal with risk score 40.

RG-027-005 [Confirmé]
IF the transfer amount is greater than or equal to 15 000 EUR
THEN THE SYSTEM SHALL raise a LARGE_AMOUNT fraud signal with risk score 80.

RG-027-006 [Suspect]
IF the number of transactions executed by the customer on the current day exceeds 8
THEN THE SYSTEM SHALL raise a HIGH_FREQUENCY fraud signal with risk score 50.

RG-027-007 [Confirmé]
IF the target IBAN is not registered as an active beneficiary for the customer in the external_beneficiaries table
THEN THE SYSTEM SHALL raise a NEW_BENEFICIARY fraud signal with risk score 35.

RG-027-008 [Confirmé]
IF the server hour at transaction time (JVM timezone) is before 06:00 or after 23:00
THEN THE SYSTEM SHALL raise an UNUSUAL_HOURS fraud signal with risk score 45.
  Note    : le fuseau utilisé est celui de la JVM et non le fuseau du client (JIRA-7703) — à corriger dans la cible

RG-027-009 [Confirmé]
IF the transfer amount is greater than or equal to 1 000 EUR and is an exact multiple of 1 000
THEN THE SYSTEM SHALL raise a ROUND_AMOUNT fraud signal with risk score 20.

RG-027-010 [Confirmé]
IF the target IBAN does not start with the prefix "FR" (case-insensitive)
THEN THE SYSTEM SHALL raise a FOREIGN_IBAN fraud signal with risk score 30.

RG-027-011 [À confirmer]
IF the previous transfer from the same customer occurred less than 30 seconds before the current one
THEN THE SYSTEM SHALL raise a RAPID_SUCCESSION fraud signal with risk score 55.

RG-027-012 [À confirmer]
IF the number of transfers above 500 EUR executed by the customer in the last 60 minutes is greater than or equal to 5
THEN THE SYSTEM SHALL raise a VELOCITY_CHECK fraud signal with risk score 65.

---

### Cycle de vie des alertes (gestion ADMIN)

RG-027-014 [Confirmé]
WHEN an ADMIN accesses the fraud alert management panel (admin.xhtml)
THE SYSTEM SHALL display all fraud alerts currently in OPEN status, ordered by creation date descending.

RG-027-015 [Confirmé]
WHEN an ADMIN confirms a fraud alert
THE SYSTEM SHALL update the alert status to CONFIRMED, record the reviewer username and set the review timestamp to the current server time.

RG-027-016 [Confirmé]
WHEN an ADMIN rejects a fraud alert as a false positive
THE SYSTEM SHALL update the alert status to FALSE_POSITIVE, record the reviewer username and set the review timestamp to the current server time.

---

### Anomalies et dette

RG-027-017 [Suspect]
WHEN multiple authentication failures are detected for a customer account
THE SYSTEM SHALL raise a MULTIPLE_FAILED_AUTH fraud signal with risk score 80.
    Portée : bloquant
    Résolution : Oui, à implémenter : signal de fraude de score 80 après 5 échecs de connexion consécutifs sur un même compte client.

RG-027-018 [Suspect]
THE SYSTEM SHALL mask the target IBAN in all server log entries (e.g. FR76****1234) to avoid exposing sensitive payment data.

RG-027-019 [Suspect]
THE SYSTEM SHALL NOT expose evaluateSingleScore() as a public API entry point for fraud detection.
    Portée : information
