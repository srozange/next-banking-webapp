---
feature: FEAT-021
title: "Exécution des virements programmés"
interface: Batch
status: current
generated_by: retro-rules
date: 2026-10-03
---

# RG-021 — Exécution des virements programmés

_Source principale : `ejb/RecurringTransferScheduler.java`, `ejb/RecurringTransferService.java`_
_Entité : `entity/RecurringTransfer.java`, `entity/RecurringFrequency.java`_

---

RG-021-001 [À confirmer]
WHEN the EJB Timer fires THE SYSTEM SHALL trigger the execution of all due recurring transfers every 5 minutes on all hours.

---

RG-021-002 [À confirmer]
THE SYSTEM SHALL run the recurring transfer timer as non-persistent, losing the timer state on server restart and resuming only at the next 5-minute tick after startup.

---

RG-021-003 [À confirmer]
WHEN the batch runs THE SYSTEM SHALL select all recurring transfers where the transfer is active and the next scheduled execution date is less than or equal to the current timestamp, ordered by ascending scheduled date.

---

RG-021-004 [À confirmer]
IF the source account balance is strictly less than the transfer amount THEN THE SYSTEM SHALL skip the transfer without advancing the next execution date.

---

RG-021-005 [À confirmer]
WHEN a due recurring transfer has sufficient source balance THE SYSTEM SHALL debit the source account by the transfer amount.

---

RG-021-006 [À confirmer]
IF the target account number matches an account in the system THEN THE SYSTEM SHALL credit that target account by the transfer amount.

---

RG-021-007 [Suspect]
IF the target account number does not match any account in the system THEN THE SYSTEM SHALL reject the transfer and refund the source account.

---

RG-021-008 [À confirmer]
WHEN a recurring transfer is executed THE SYSTEM SHALL create a DEBIT transaction record on the source account with a description including the target label and the optional transfer motif.

---

RG-021-009 [À confirmer]
WHEN a recurring transfer is executed and the target account is internal THE SYSTEM SHALL create a CREDIT transaction record on the target account with a description including the source account number and the optional transfer motif.

---

RG-021-010 [À confirmer]
WHEN a recurring transfer is successfully executed THE SYSTEM SHALL record the last execution timestamp and increment the execution counter by 1.

---

RG-021-011 [Suspect]
WHEN computing the next execution date after a successful execution THE SYSTEM SHALL advance the next scheduled execution date computed from the originally scheduled date and the transfer frequency.

---

RG-021-012 [À confirmer]
WHEN computing the next execution date THE SYSTEM SHALL add 7 calendar days for HEBDOMADAIRE, 1 calendar month for MENSUEL, 2 calendar months for BIMESTRIEL, 3 calendar months for TRIMESTRIEL, 6 calendar months for SEMESTRIEL, and 1 calendar year for ANNUEL.

---

RG-021-013 [Suspect]
WHEN the execution of an individual recurring transfer raises an exception THE SYSTEM SHALL isolate the failure and continue processing the remaining due transfers in the same batch run.
