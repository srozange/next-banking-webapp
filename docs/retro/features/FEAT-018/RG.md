---
feature: FEAT-018
title: "Calcul des intérêts nocturnes"
interface: Batch
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# RG — FEAT-018 Calcul des intérêts nocturnes

> Interface : Batch — aucune interface utilisateur directe.
> Pas de FIELDS.md ni de fields-raw.json (feature batch sans écran) — normal, sans blocage.

---

## Règles de gestion

RG-018-001 [À confirmer]
WHEN the EJB timer fires at 2:00 AM every day THE SYSTEM SHALL execute the interest calculation batch job named "INTEREST_CALCULATION".

RG-018-002 [À confirmer]
WHEN the interest calculation batch runs THE SYSTEM SHALL select all bank accounts of type EPARGNE.

RG-018-003 [Suspect]
WHEN the interest calculation batch runs THE SYSTEM SHALL apply a monthly interest rate of 0.5% / 12 to each selected EPARGNE account once per month.

RG-018-004 [À confirmer]
WHEN calculating the interest amount for an EPARGNE account THE SYSTEM SHALL round the result to 2 decimal places using HALF_UP rounding.

RG-018-005 [À confirmer]
IF the calculated interest amount for an EPARGNE account is zero or negative THEN THE SYSTEM SHALL skip that account without crediting it or creating a transaction.

RG-018-006 [À confirmer]
WHEN interest is credited to an EPARGNE account THE SYSTEM SHALL increase the account balance by the calculated interest amount.

RG-018-007 [À confirmer]
WHEN interest is credited to an EPARGNE account THE SYSTEM SHALL persist a CREDIT transaction recording the interest amount, the resulting account balance, the current date and time as transaction date, and the description "Interets mensuels livret epargne (batch nocturne)".

RG-018-008 [À confirmer]
WHEN the interest calculation batch completes without exception THE SYSTEM SHALL record a BATCH_JOB_LOGS entry with job name "INTEREST_CALCULATION", status "SUCCESS", start time, end time, and the count of EPARGNE accounts evaluated.

RG-018-009 [À confirmer]
IF an exception occurs during the interest calculation batch THEN THE SYSTEM SHALL record a BATCH_JOB_LOGS entry with job name "INTEREST_CALCULATION", status "FAILED", and the exception message as details, and log the error at ERROR level.

RG-018-010 [À confirmer]
WHEN an ADMIN user views the batch administration screen THE SYSTEM SHALL display recent BATCH_JOB_LOGS entries including INTEREST_CALCULATION executions ordered by start time descending.
