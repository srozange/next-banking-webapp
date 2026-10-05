---
feature: FEAT-024
name: "Traitement asynchrone approbation prêts (JMS)"
interface: Transversal
status: current
retro_step: retro-rules
generated_at: "2026-10-03"
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# RG — FEAT-024 « Traitement asynchrone approbation prêts (JMS) »

Pipeline JMS de scoring crédit : le client soumet une demande de prêt, un message est publié sur la queue JMS, le MDB score la demande selon des plafonds par type, puis met à jour le statut du prêt et notifie l'emprunteur. Aucun écran propre — traitement purement asynchrone en arrière-plan.

---

## Phase 1 — Soumission de la demande

RG-024-001 [À confirmer]
WHEN a client submits a loan request
THE SYSTEM SHALL persist the loan with status EN_ATTENTE and publish a LoanApprovalMessage to the JMS credit queue before returning.

RG-024-002 [À confirmer]
THE SYSTEM SHALL flush the loan record to the database before publishing the JMS message, ensuring the loan identifier is available to the credit service.

---

## Phase 2 — Scoring crédit : durée

RG-024-003 [À confirmer]
IF the loan duration exceeds 300 months THEN THE SYSTEM SHALL refuse the loan regardless of type or amount.

---

## Phase 3 — Scoring crédit : plafonds par type

RG-024-004 [À confirmer]
IF the loan type is CONSOMMATION AND the requested amount is less than or equal to 25,000 EUR THEN THE SYSTEM SHALL approve the loan.

RG-024-005 [À confirmer]
IF the loan type is CONSOMMATION AND the requested amount exceeds 25,000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-024-006 [À confirmer]
IF the loan type is AUTO AND the requested amount is less than or equal to 40,000 EUR THEN THE SYSTEM SHALL approve the loan.

RG-024-007 [À confirmer]
IF the loan type is AUTO AND the requested amount exceeds 40,000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-024-008 [À confirmer]
IF the loan type is TRAVAUX AND the requested amount is less than or equal to 80,000 EUR THEN THE SYSTEM SHALL approve the loan.

RG-024-009 [À confirmer]
IF the loan type is TRAVAUX AND the requested amount exceeds 80,000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-024-010 [À confirmer]
IF the loan type is ETUDIANT AND the requested amount is less than or equal to 20,000 EUR THEN THE SYSTEM SHALL approve the loan.

RG-024-011 [À confirmer]
IF the loan type is ETUDIANT AND the requested amount exceeds 20,000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-024-012 [À confirmer]
IF the loan type is IMMOBILIER AND the requested amount is less than or equal to 600,000 EUR THEN THE SYSTEM SHALL approve the loan.

RG-024-013 [À confirmer]
IF the loan type is IMMOBILIER AND the requested amount exceeds 600,000 EUR THEN THE SYSTEM SHALL refuse the loan.

---

## Phase 4 — Conséquences de l'approbation / du refus

RG-024-014 [À confirmer]
WHEN the credit service approves a loan
THE SYSTEM SHALL set the loan status to EN_COURS and generate the complete amortization schedule (one LoanRepayment record per monthly instalment).

RG-024-015 [À confirmer]
WHEN the credit service refuses a loan
THE SYSTEM SHALL set the loan status to REFUSE.

RG-024-016 [À confirmer]
WHEN the credit service approves a loan
THE SYSTEM SHALL send a CONFIRMATION notification to the loan applicant via the notification store.

RG-024-017 [À confirmer]
WHEN the credit service refuses a loan
THE SYSTEM SHALL send an ALERTE notification to the loan applicant indicating that the credit cap or maximum duration was exceeded.

---

## Phase 5 — Gardes et robustesse

RG-024-018 [À confirmer]
WHEN the credit service receives an approval message for a loan whose status is not EN_ATTENTE
THE SYSTEM SHALL ignore the message without modifying any state.

RG-024-019 [À confirmer]
THE SYSTEM SHALL cap per-user notifications at MAX_NOTIFS_PAR_USER (default: 20), removing the oldest notification when the cap is reached.

RG-024-020 [Suspect]
IF the JMS infrastructure is unavailable when publishing a loan approval request
THEN THE SYSTEM SHALL reject the loan submission and inform the client that the credit service is temporarily unavailable.

RG-024-021 [Suspect]
IF the loan type is not recognized by the credit scorer
THEN THE SYSTEM SHALL refuse the loan request.
