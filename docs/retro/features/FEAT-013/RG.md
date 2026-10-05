---
feature: FEAT-013
title: "Gestion des bénéficiaires externes"
generated_by: retro-rules
generated_at: "2026-10-03"
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# Règles de gestion — FEAT-013 « Gestion des bénéficiaires externes »

---

## Contrôle d'accès

RG-013-001 [À confirmer]
IF the user is not authenticated THEN THE SYSTEM SHALL redirect to /login.xhtml and deny access to /beneficiaires.xhtml.

RG-013-002 [À confirmer]
THE SYSTEM SHALL grant access to /beneficiaires.xhtml to all authenticated users regardless of role (CLIENT, CONSEILLER, ADMIN).

---

## Affichage de la liste des bénéficiaires

RG-013-003 [À confirmer]
WHEN the beneficiaires page loads THE SYSTEM SHALL display only the beneficiaries associated with the authenticated user's customer account.

RG-013-004 [À confirmer]
THE SYSTEM SHALL exclude beneficiaries with active flag set to false from the displayed list.

RG-013-005 [À confirmer]
THE SYSTEM SHALL display the beneficiaries list sorted alphabetically ascending by label.

RG-013-006 [À confirmer]
WHEN the authenticated user has no active beneficiaries THE SYSTEM SHALL display the message "Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un."

RG-013-007 [À confirmer]
WHEN displaying an IBAN in the beneficiaries table THE SYSTEM SHALL show the first 4 characters followed by "***" followed by the last 4 characters of the stored IBAN.

RG-013-008 [À confirmer]
WHEN a beneficiary's BIC is not set THE SYSTEM SHALL display "—" in the BIC column.

RG-013-009 [À confirmer]
WHEN a beneficiary's bank name is not set THE SYSTEM SHALL display "—" in the bank column.

RG-013-010 [À confirmer]
WHEN displaying a beneficiary THE SYSTEM SHALL show a SEPA zone badge computed from the IBAN country code: "✓ SEPA" if the country code belongs to the SEPA zone, "Hors SEPA" otherwise.

---

## Vérification IBAN pré-soumission

RG-013-011 [À confirmer]
WHEN the user requests IBAN verification with no IBAN entered THE SYSTEM SHALL display "Veuillez saisir un IBAN."

RG-013-012 [À confirmer]
WHEN the user requests IBAN verification with an IBAN that fails MOD-97 THE SYSTEM SHALL display "IBAN invalide (echec MOD-97). Verifiez la saisie."

RG-013-013 [À confirmer]
WHEN the user requests IBAN verification with a valid IBAN THE SYSTEM SHALL display "IBAN valide — {country_code} (Zone SEPA)" if the IBAN country code belongs to the SEPA zone, or "IBAN valide — {country_code} (Hors zone SEPA)" otherwise.

RG-013-014 [À confirmer]
WHEN the user requests IBAN verification THE SYSTEM SHALL execute the check via Ajax partial submit limited to the IBAN field without submitting the add-beneficiary form.

---

## Validation à l'ajout d'un bénéficiaire

RG-013-015 [À confirmer]
IF libelle is empty or null at submission THEN THE SYSTEM SHALL reject the add request and display "Le libellé est obligatoire."
  Champ   : libelle (FEAT-013/SCR-013-01)

RG-013-016 [À confirmer]
IF IBAN is empty or null at submission THEN THE SYSTEM SHALL reject the add request and display "L'IBAN est obligatoire."
  Champ   : iban (FEAT-013/SCR-013-01)

RG-013-017 [À confirmer]
IF the IBAN fails MOD-97 validation at submission THEN THE SYSTEM SHALL reject the add request and display "IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97)."
  Champ   : iban (FEAT-013/SCR-013-01)

RG-013-018 [À confirmer]
IF nomTitulaire is empty or null at submission THEN THE SYSTEM SHALL reject the add request and display "Le nom du titulaire est obligatoire."
  Champ   : nomTitulaire (FEAT-013/SCR-013-01)

RG-013-019 [À confirmer]
IF BIC is provided at submission and fails the format check THEN THE SYSTEM SHALL reject the add request and display "BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques."
  Champ   : bic (FEAT-013/SCR-013-01)

RG-013-020 [À confirmer]
IF the authenticated user's session contains no customer at submission THEN THE SYSTEM SHALL reject the add request and display "Session expirée, veuillez vous reconnecter."

---

## Persistance à l'ajout

RG-013-021 [À confirmer]
WHEN storing a beneficiary's IBAN THE SYSTEM SHALL normalize it by removing all whitespace characters and converting to uppercase before persistence.

RG-013-022 [À confirmer]
WHEN storing a beneficiary's BIC THE SYSTEM SHALL normalize it by trimming whitespace and converting to uppercase; if the BIC is blank after trimming THE SYSTEM SHALL store null.

RG-013-023 [À confirmer]
WHEN storing a beneficiary's bank name THE SYSTEM SHALL trim whitespace; if the bank name is blank after trimming THE SYSTEM SHALL store null.

RG-013-024 [À confirmer]
WHEN a beneficiary is persisted THE SYSTEM SHALL compute and store the SEPA zone flag from the normalized IBAN country code.

RG-013-025 [À confirmer]
WHEN a beneficiary is persisted THE SYSTEM SHALL record the creation date as the current server date.

RG-013-026 [À confirmer]
WHEN a beneficiary is persisted THE SYSTEM SHALL set the active flag to true.

RG-013-027 [À confirmer]
WHEN a beneficiary is successfully added THE SYSTEM SHALL display "Bénéficiaire ajouté avec succès.", reload the beneficiaries list, and reset the add form to empty.

---

## Contraintes de champs

RG-013-028 [À confirmer]
IF libelle length exceeds 100 characters THEN THE SYSTEM SHALL reject the add request.
  Champ   : libelle (FEAT-013/SCR-013-01)

RG-013-029 [Suspect]
IF libelle length exceeds 100 characters THEN THE SYSTEM SHALL inform the user of the rejection.

RG-013-030 [À confirmer]
IF nomTitulaire length exceeds 100 characters THEN THE SYSTEM SHALL reject the add request.
  Champ   : nomTitulaire (FEAT-013/SCR-013-01)

RG-013-031 [Suspect]
IF nomTitulaire length exceeds 100 characters THEN THE SYSTEM SHALL inform the user of the rejection.

RG-013-032 [À confirmer]
IF bank name length exceeds 100 characters THEN THE SYSTEM SHALL reject the add request.
  Champ   : banque (FEAT-013/SCR-013-01)

---

## Suppression d'un bénéficiaire

RG-013-033 [À confirmer]
WHEN the user deletes a beneficiary THE SYSTEM SHALL soft-delete it by setting its active flag to false; the record is not physically removed from the database.

RG-013-034 [À confirmer]
WHEN a beneficiary is deleted THE SYSTEM SHALL immediately refresh the displayed list without full page reload.

RG-013-035 [Suspect]
WHEN the user deletes a beneficiary THE SYSTEM SHALL verify that the beneficiary belongs to the authenticated user before executing the deletion.
