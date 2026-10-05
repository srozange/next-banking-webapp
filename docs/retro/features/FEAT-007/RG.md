---
feature: FEAT-007
feature_name: Demande et suivi de prêts
extraction_date: 2026-10-03
sources:
  - src/main/java/com/accenture/bank/web/LoanBean.java
  - src/main/java/com/accenture/bank/ejb/LoanService.java
  - src/main/java/com/accenture/bank/mq/LoanApprovalMDB.java
  - src/main/java/com/accenture/bank/entity/Loan.java
  - src/main/java/com/accenture/bank/entity/LoanType.java
  - src/main/java/com/accenture/bank/entity/LoanStatus.java
  - retro/features/FEAT-007/FIELDS.md
  - retro/features/FEAT-007/fields-raw.json
  - retro/features/FEAT-007/STATES.md
---

# RG — FEAT-007 Demande et suivi de prêts

---

## Accès

---

RG-007-001 [À confirmer]
IF the session has no authenticated customer THEN THE SYSTEM SHALL redirect to /login.

RG-007-002 [À confirmer]
THE SYSTEM SHALL grant access to the loan page to all authenticated roles (CLIENT, CONSEILLER, ADMIN) with the same interface and no role-specific restrictions.

---

## Simulateur de prêt — Validation

---

RG-007-003 [À confirmer]
IF simMontant is null or less than or equal to zero THEN THE SYSTEM SHALL reject the simulation and display "Montant invalide".
  Champ   : simMontant (FEAT-007/SCR-007-02)
  Message : "Montant invalide"

RG-007-004 [À confirmer]
IF simTaux is less than zero or greater than 30 THEN THE SYSTEM SHALL reject the simulation and display "Taux invalide (0-30%)".
  Champ   : simTaux (FEAT-007/SCR-007-02)

RG-007-005 [À confirmer]
IF simDuree is less than or equal to zero or greater than 360 THEN THE SYSTEM SHALL reject the simulation and display "Duree invalide (1-360 mois)".
  Champ   : simDuree (FEAT-007/SCR-007-02)

RG-007-006 [À confirmer]
THE SYSTEM SHALL pre-populate the simulation form with the following default values: montant = 10 000 EUR, taux annuel = 3,50 %, duree = 36 mois.

---

## Simulateur de prêt — Calcul

---

RG-007-007 [À confirmer]
WHEN a simulation is requested with valid inputs (montant > 0, taux ∈ [0, 30], duree ∈ [1, 360]) THE SYSTEM SHALL compute and display mensualite, cout total, and total interets.

RG-007-008 [À confirmer]
WHEN the annual rate is zero THE SYSTEM SHALL compute the monthly installment as montant divided equally by dureeMois.

RG-007-009 [À confirmer]
WHEN the annual rate is greater than zero THE SYSTEM SHALL compute the monthly installment using the standard annuity formula (PMT).

RG-007-010 [À confirmer]
WHEN a simulation is computed THE SYSTEM SHALL display a complete amortization schedule showing for each period: installment number, capital repaid, interest, total payment, and remaining capital.

---

## Demande de prêt — Validation des champs

---

RG-007-011 [À confirmer]
THE SYSTEM SHALL offer the following loan types with CONSOMMATION as default: IMMOBILIER, AUTO (Automobile), CONSOMMATION, TRAVAUX, ETUDIANT.
  Champ   : typePret (FEAT-007/Section Demande) — combobox

RG-007-012 [À confirmer]
IF formMontant is null or less than or equal to zero THEN THE SYSTEM SHALL reject the loan request and display "Montant invalide".
  Champ   : montant (FEAT-007/Section Demande) — required=true
  Message : "Montant invalide"

RG-007-013 [À confirmer]
IF formTaux is less than zero or greater than 30 THEN THE SYSTEM SHALL reject the loan request and display "Taux invalide".
  Champ   : taux (FEAT-007/Section Demande)

RG-007-014 [À confirmer]
IF formDuree is less than or equal to zero or greater than 360 THEN THE SYSTEM SHALL reject the loan request and display "Duree invalide".
  Champ   : duree (FEAT-007/Section Demande)

RG-007-015 [À confirmer]
IF formMotif is null or blank THEN THE SYSTEM SHALL reject the loan request and display "Veuillez indiquer un motif".
  Champ   : objetPret (FEAT-007/Section Demande)

RG-007-016 [À confirmer]
IF formMotif exceeds 255 characters THEN THE SYSTEM SHALL reject the loan request.
  Champ   : objetPret (FEAT-007/Section Demande) — @Column(length=255)

---

## Demande de prêt — Soumission

---

RG-007-017 [À confirmer]
WHEN a valid loan request is submitted THE SYSTEM SHALL create a loan record with status EN_ATTENTE, computed mensualite/coutTotal/totalInterets/dateDebut/dateFin, and publish a LoanApprovalMessage to queue/pretDemandesQueue.

RG-007-018 [À confirmer]
WHEN a loan is created THE SYSTEM SHALL set dateDebut to the first day of the current month and dateFin to dateDebut plus dureeMois calendar months.

RG-007-019 [À confirmer]
WHEN a loan request is successfully submitted THE SYSTEM SHALL display "Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes)" and refresh the loan list.
  Message : "Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes)"

RG-007-020 [À confirmer]
WHEN no loan exists for the authenticated customer THE SYSTEM SHALL display "Aucun emprunt enregistré".
  Message : "Aucun emprunt enregistré"

---

## Scoring et transitions d'état

---

RG-007-021 [À confirmer]
WHEN a LoanApprovalMessage is received by the scoring service THE SYSTEM SHALL approve the loan if the scorer returns true, otherwise refuse it.

RG-007-022 [À confirmer]
IF loan type is CONSOMMATION AND montant is greater than 25 000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-007-023 [À confirmer]
IF loan type is AUTO AND montant is greater than 40 000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-007-024 [À confirmer]
IF loan type is TRAVAUX AND montant is greater than 80 000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-007-025 [À confirmer]
IF loan type is ETUDIANT AND montant is greater than 20 000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-007-026 [À confirmer]
IF loan type is IMMOBILIER AND montant is greater than 600 000 EUR THEN THE SYSTEM SHALL refuse the loan.

RG-007-027 [À confirmer]
IF dureeMois is greater than 300 THEN THE SYSTEM SHALL refuse the loan regardless of type or amount.

RG-007-028 [Suspect]
IF formDuree is between 301 and 360 months (inclusive) THEN THE SYSTEM SHALL reject the loan request as invalid.

RG-007-029 [À confirmer]
WHEN a loan is approved THE SYSTEM SHALL transition its status from EN_ATTENTE to EN_COURS and generate N LoanRepayment records (N = dureeMois), one per monthly installment.

RG-007-030 [À confirmer]
WHEN a loan is refused THE SYSTEM SHALL transition its status from EN_ATTENTE to REFUSE.

RG-007-031 [À confirmer]
WHEN loan status changes to EN_COURS or REFUSE THE SYSTEM SHALL notify the customer with a BankNotification via the in-memory notification store.

RG-007-032 [À confirmer]
IF a loan is not in EN_ATTENTE status THEN THE SYSTEM SHALL silently ignore approuverPret and refuserPret calls.

---

## Paiement d'échéance

---

RG-007-033 [À confirmer]
WHEN a CLIENT pays an unpaid installment THE SYSTEM SHALL mark it as paid and record the payment date.

RG-007-034 [À confirmer]
IF all installments of a loan are marked as paid THEN THE SYSTEM SHALL transition the loan status to REMBOURSE.

RG-007-035 [À confirmer]
IF an installment is already marked as paid THEN THE SYSTEM SHALL silently skip re-payment without error.

RG-007-036 [À confirmer]
WHEN an installment payment is recorded THE SYSTEM SHALL display "Echeance marquee comme payee".
  Message : "Echeance marquee comme payee"

---

## Consultation de l'écheancier

---

RG-007-037 [À confirmer]
WHEN a customer consults the loan page THE SYSTEM SHALL display only that customer's own loans, ordered by dateDebut descending.

RG-007-038 [À confirmer]
WHEN a loan has status EN_COURS or REMBOURSE THE SYSTEM SHALL make its amortization schedule viewable.
    Portée : information

RG-007-039 [À confirmer]
WHEN the amortization schedule is displayed THE SYSTEM SHALL show for each installment: numero, date echeance (dd/MM/yyyy), capital rembourse, interets, total, capital restant, and statut (Paye / En cours).

---

## Suppression d'un prêt

---

RG-007-040 [À confirmer]
WHEN a customer deletes a loan THE SYSTEM SHALL permanently remove the loan and all its installments without displaying a confirmation dialog.
