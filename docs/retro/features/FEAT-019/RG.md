---
feature: FEAT-019
name: Détection fraude nocturne par batch
interface: Batch
extracted_at: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# RG-019 — Détection fraude nocturne par batch

Fichier principal : `src/main/java/com/accenture/bank/ejb/NightlyBatchService.java` (méthode `runFraudDetection`).

---

## Règles de gestion

---

RG-019-001 [Confirmé]
WHEN the system clock reaches 02:30:00 (JVM local time) on any day THE SYSTEM SHALL execute the nightly fraud-detection batch job.

---

RG-019-002 [À confirmer]
WHEN the application server JVM restarts after 02:30:00 on a given day THE SYSTEM SHALL NOT re-execute the nightly fraud-detection batch for that day.

---

RG-019-003 [Confirmé]
WHEN the nightly fraud-detection batch runs THE SYSTEM SHALL query all transactions whose amount is greater than or equal to 10 000 EUR and whose transaction date falls within the 24 hours preceding the batch execution time.

---

RG-019-004 [À confirmer]
WHEN the nightly fraud-detection batch queries suspicious transactions THE SYSTEM SHALL include both CREDIT and DEBIT transaction types without filtering by type.

---

RG-019-005 [Suspect]
WHEN the nightly fraud-detection batch queries suspicious transactions THE SYSTEM SHALL apply a threshold of amount >= 10 000 EUR (inclusive).

---

RG-019-006 [Confirmé]
WHEN the nightly fraud-detection batch completes (successfully or not) THE SYSTEM SHALL write exactly one entry in the batch job log table with job_name = "FRAUD_DETECTION", the count of matching transactions in records_processed, and status "SUCCESS" or "FAILED".

---

RG-019-007 [Confirmé]
WHEN the nightly fraud-detection batch runs THE SYSTEM SHALL NOT persist any FraudAlert entity — the batch produces a count-only summary log and does not raise individual fraud alert records.

---

RG-019-008 [Confirmé]
WHEN the nightly fraud-detection batch encounters a database exception THE SYSTEM SHALL catch the exception, record a BatchJobLog entry with status "FAILED" and the exception message, and log the error — without propagating the exception to the EJB container.

---

RG-019-009 [Suspect]
WHEN the nightly fraud-detection batch computes the 24-hour lookback window THE SYSTEM SHALL use the JVM default timezone to determine the boundary time.
