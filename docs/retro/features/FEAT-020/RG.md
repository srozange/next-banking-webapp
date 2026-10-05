---
feature: FEAT-020
name: "Relevés mensuels — batch stub (dette-legacy)"
interface: Batch
status: current
generated_by: retro-rules
---

# RG — FEAT-020 « Relevés mensuels — batch stub (dette-legacy) »

_Source principale :._
_Interface : Batch — aucun écran, aucun champ de saisie._

---

## Règles de gestion

RG-020-001 [À confirmer]
WHEN the 1st day of each month at 03:00 AM is reached
THE SYSTEM SHALL trigger the monthly statements batch job.

---

RG-020-002 [Suspect]
WHEN the monthly statements batch runs
THE SYSTEM SHALL generate a monthly statement document for each active CLIENT-role customer.

---

RG-020-003 [À confirmer]
WHEN the monthly statements batch completes without error
THE SYSTEM SHALL record a job execution log entry with job name MONTHLY_STATEMENTS, the count of active CLIENT-role customers as the number of records processed, and status SUCCESS.

---

RG-020-004 [À confirmer]
IF an error occurs during the monthly statements batch
THEN THE SYSTEM SHALL record a job execution log entry with job name MONTHLY_STATEMENTS, zero records processed, and status FAILED.

---

RG-020-005 [À confirmer]
THE SYSTEM SHALL not replay monthly statements batch executions missed while the application server was not running.
