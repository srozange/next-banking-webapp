---
feature: FEAT-004
name: Virements programmés
status: current
step: retro-rules
date: 2026-10-03
sources:
  - src/main/java/com/accenture/bank/web/RecurringTransferBean.java
  - src/main/java/com/accenture/bank/ejb/RecurringTransferService.java
  - src/main/java/com/accenture/bank/ejb/RecurringTransferScheduler.java
  - src/main/java/com/accenture/bank/entity/RecurringTransfer.java
  - src/main/java/com/accenture/bank/entity/RecurringFrequency.java
  - src/main/webapp/virement-programme.xhtml
---

# FEAT-004 — Virements programmés : Règles de gestion

---

## Accès et chargement

RG-004-001 [À confirmer]
WHEN a user accesses the recurring transfers page without an authenticated session (no `currentCustomer` in session)
THE SYSTEM SHALL redirect to `/login?faces-redirect=true`.

RG-004-002 [À confirmer]
WHEN the recurring transfers page loads for an authenticated customer
THE SYSTEM SHALL display only the bank accounts belonging to that customer in the source account selector.

RG-004-003 [À confirmer]
WHEN the recurring transfers page loads for an authenticated customer
THE SYSTEM SHALL display only the recurring transfers belonging to that customer, ordered by next execution date ascending.

---

## Validation du formulaire de création

RG-004-010 [À confirmer]
IF the source account field (`srcAcc`) is not selected THEN THE SYSTEM SHALL reject the submission and display "Veuillez selectionner un compte source".
  Champ   : srcAcc (FEAT-004/SCR-004-01)
  Message : "Veuillez selectionner un compte source"

RG-004-011 [À confirmer]
IF the beneficiary account number field (`targetNum`) is empty or blank THEN THE SYSTEM SHALL reject the submission and display "Veuillez saisir le numero de compte destinataire".
  Champ   : targetNum (FEAT-004/SCR-004-01)
  Message : "Veuillez saisir le numero de compte destinataire"

RG-004-012 [À confirmer]
IF the beneficiary label field (`targetLabel`) is empty or blank THEN THE SYSTEM SHALL reject the submission and display "Veuillez saisir le libelle du beneficiaire".
  Champ   : targetLabel (FEAT-004/SCR-004-01)
  Message : "Veuillez saisir le libelle du beneficiaire"

RG-004-013 [À confirmer]
IF the amount (`montant`) is null or less than or equal to zero THEN THE SYSTEM SHALL reject the submission and display "Montant invalide".
  Champ   : montant (FEAT-004/SCR-004-01)
  Message : "Montant invalide"

RG-004-014 [À confirmer]
IF the first execution date field (`dateExec`) is empty or blank THEN THE SYSTEM SHALL reject the submission and display "Veuillez saisir la date de premiere execution".
  Champ   : dateExec (FEAT-004/SCR-004-01)
  Message : "Veuillez saisir la date de premiere execution"

RG-004-015 [À confirmer]
IF the first execution date field (`dateExec`) does not match the format `yyyy-MM-dd` THEN THE SYSTEM SHALL reject the submission and display "Format de date invalide (attendu : yyyy-MM-dd)".
  Champ   : dateExec (FEAT-004/SCR-004-01)
  Message : "Format de date invalide (attendu : yyyy-MM-dd)"

RG-004-016 [Suspect]
IF the first execution date is submitted in `yyyy-MM-dd` format THEN THE SYSTEM SHALL accept it as the date of first execution.
  Champ   : dateExec (FEAT-004/SCR-004-01)

---

## Validation de service (couche EJB)

RG-004-020 [À confirmer]
IF the customer or source account cannot be found in the database THEN THE SYSTEM SHALL reject the creation of the recurring transfer.

RG-004-021 [À confirmer]
IF the source account does not belong to the authenticated customer THEN THE SYSTEM SHALL reject the creation of the recurring transfer.

RG-004-022 [À confirmer]
IF the amount is null or less than or equal to zero at the service layer THEN THE SYSTEM SHALL reject the creation of the recurring transfer.

---

## Création réussie

RG-004-023 [À confirmer]
WHEN a recurring transfer is successfully created
THE SYSTEM SHALL persist it with actif=true, executionCount=0, and a creation timestamp, then display "Virement programme cree avec succes".
  Message : "Virement programme cree avec succes"

RG-004-024 [À confirmer]
WHEN a recurring transfer is successfully created
THE SYSTEM SHALL reset all form fields and reload the list of recurring transfers.

---

## Fréquences supportées

RG-004-030 [À confirmer]
THE SYSTEM SHALL support six recurrence frequencies for recurring transfers: HEBDOMADAIRE (7 calendar days), MENSUEL (1 calendar month), BIMESTRIEL (2 calendar months), TRIMESTRIEL (3 calendar months), SEMESTRIEL (6 calendar months), ANNUEL (1 calendar year).

---

## Gestion de l'état (suspension / réactivation / suppression)

RG-004-040 [À confirmer]
WHEN an active recurring transfer is suspended ("Suspendre")
THE SYSTEM SHALL toggle its `actif` flag to false and reload the list.

RG-004-041 [À confirmer]
WHEN an inactive recurring transfer is reactivated ("Reactiver")
THE SYSTEM SHALL toggle its `actif` flag to true and reload the list.

RG-004-042 [À confirmer]
WHEN a recurring transfer is deleted ("Supprimer")
THE SYSTEM SHALL permanently remove it from the database.

RG-004-043 [Suspect]
WHEN toggle (suspend/reactivate) or delete is requested for a recurring transfer
THE SYSTEM SHALL verify that the transfer belongs to the authenticated customer before executing the action.

---

## Affichage de la liste

RG-004-050 [À confirmer]
WHEN displaying the list of recurring transfers
THE SYSTEM SHALL show buttons "Suspendre" and "Supprimer" for active transfers, and "Reactiver" and "Supprimer" for inactive transfers.

RG-004-051 [À confirmer]
WHEN displaying the next execution date of a recurring transfer
THE SYSTEM SHALL format it as `dd/MM/yyyy`.

RG-004-052 [À confirmer]
WHEN the list of recurring transfers is empty
THE SYSTEM SHALL display "Aucun virement programme configure".
  Message : "Aucun virement programme configure"

---

## Exécution automatique (scheduler EJB — FEAT-021)

RG-004-060 [À confirmer]
WHEN the EJB timer fires every 5 minutes
THE SYSTEM SHALL query all recurring transfers with `actif = true` and `prochainExecution <= now` and attempt to execute each one in sequence.

RG-004-061 [Suspect]
IF the source account balance is strictly less than the transfer amount at execution time
THEN THE SYSTEM SHALL skip the execution without changing the transfer state or notifying the customer.

RG-004-062 [Suspect]
WHEN a recurring transfer is executed and the target account number matches an internal account
THE SYSTEM SHALL debit the source account, credit the target account, and persist both a DEBIT and a CREDIT transaction record.

RG-004-063 [Suspect]
WHEN a recurring transfer is executed and the target account number does not match any internal account
THE SYSTEM SHALL debit the source account and persist a DEBIT transaction record without crediting any account.
    Portée : bloquant
    Résolution : Virements internes uniquement. Si le compte bénéficiaire ne correspond à aucun compte interne (IBAN externe ou faute de frappe), le virement est refusé avant tout débit, avec un message d'erreur au client, sans débit ni écriture. Le débit sans crédit du système actuel est un bug à corriger. Le routage vers des IBAN externes n'existe pas aujourd'hui et n'est pas demandé pour la réécriture.

RG-004-064 [À confirmer]
WHEN a recurring transfer is executed successfully
THE SYSTEM SHALL set `lastExecuted` to the current timestamp, increment `executionCount` by 1, and compute the next execution date from the current time using the configured frequency.

RG-004-065 [À confirmer]
IF the execution of a single recurring transfer fails with an exception
THEN THE SYSTEM SHALL log a warning and continue processing remaining due transfers.

RG-004-066 [À confirmer]
IF the first execution date is set to a past date at creation time
THEN THE SYSTEM SHALL accept the recurring transfer and execute it at the next scheduler tick (within 5 minutes).

---

## Contraintes de champ (DB)

RG-004-070 [Suspect]
IF the motif field exceeds 255 characters THEN THE SYSTEM SHALL reject the submission.
  Champ   : motif (FEAT-004/SCR-004-01)

RG-004-071 [À confirmer]
IF the beneficiary account number exceeds 34 characters THEN THE SYSTEM SHALL reject the submission.
  Champ   : targetNum (FEAT-004/SCR-004-01)

RG-004-072 [À confirmer]
IF the beneficiary label exceeds 100 characters THEN THE SYSTEM SHALL reject the submission.
  Champ   : targetLabel (FEAT-004/SCR-004-01)
