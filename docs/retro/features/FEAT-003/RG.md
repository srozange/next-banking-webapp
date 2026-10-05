---
feature: FEAT-003
feature_name: Effectuer un virement
extraction_date: 2026-10-03
sources:
  - src/main/java/com/accenture/bank/web/VirementBean.java
  - src/main/java/com/accenture/bank/ejb/TransferService.java
  - src/main/java/com/accenture/bank/ejb/FraudDetectionService.java
  - src/main/java/com/accenture/bank/mq/VirementProcessorMDB.java
  - src/main/java/com/accenture/bank/mq/VirementProducer.java
  - src/main/java/com/accenture/bank/config/AppConfig.java
  - retro/features/FEAT-003/FIELDS.md
  - retro/features/FEAT-003/fields-raw.json
---

# RG — FEAT-003 Effectuer un virement

---

## Accès et chargement du formulaire

---

RG-003-001 [À confirmer]
THE SYSTEM SHALL grant access to the transfer form to any authenticated user regardless of role.

RG-003-002 [À confirmer]
WHEN the transfer form is loaded THE SYSTEM SHALL display only the bank accounts belonging to the connected customer.
  Note    : CONSEILLER et ADMIN n'ont aucun compte propre en base ; le formulaire est rendu mais inutilisable (listes vides).

---

## Validation des champs (contraintes de saisie)

---

RG-003-003 [À confirmer]
IF no source account is selected THEN THE SYSTEM SHALL reject the transfer and prompt the user to select both accounts.
  Champ   : compteSource (FEAT-003/SCR-003-01)
  Message : "Veuillez sélectionner les deux comptes"

RG-003-004 [À confirmer]
IF no target account is selected THEN THE SYSTEM SHALL reject the transfer and prompt the user to select both accounts.
  Champ   : compteDestinataire (FEAT-003/SCR-003-01)
  Message : "Veuillez sélectionner les deux comptes"

RG-003-005 [À confirmer]
IF the source account and the target account are the same THEN THE SYSTEM SHALL reject the transfer and inform the user that the two accounts must differ.
  Message : "Le compte source et destination doivent être différents"

RG-003-006 [À confirmer]
IF the requested transfer amount is less than or equal to zero THEN THE SYSTEM SHALL reject the transfer.
  Champ   : montant (FEAT-003/SCR-003-01)

RG-003-007 [Suspect]
IF the source account balance is insufficient for the requested amount THEN THE SYSTEM SHALL reject the transfer and inform the user that the balance is insufficient.

RG-003-008 [À confirmer]
IF a referenced account (source or target) is not found in the database THEN THE SYSTEM SHALL reject the transfer.

---

## Exécution du virement

---

RG-003-009 [À confirmer]
THE SYSTEM SHALL execute the transfer without requesting user confirmation before the operation.

RG-003-010 [À confirmer]
WHEN a valid transfer is submitted THE SYSTEM SHALL debit the source account by the transfer amount within the same database transaction.

RG-003-011 [À confirmer]
WHEN a valid transfer is submitted THE SYSTEM SHALL credit the target account by the transfer amount within the same database transaction.

RG-003-012 [À confirmer]
WHEN a transfer is executed THE SYSTEM SHALL persist a DEBIT transaction record on the source account and a CREDIT transaction record on the target account, each carrying the balance after the operation.

RG-003-013 [À confirmer]
WHEN a transfer is executed THE SYSTEM SHALL record the transfer with its timestamp.

RG-003-014 [Suspect]
IF the transfer amount is greater than or equal to 5000 EUR THEN THE SYSTEM SHALL mark the transfer status as PENDING.

RG-003-015 [À confirmer]
IF the transfer amount is strictly less than 5000 EUR THEN THE SYSTEM SHALL mark the transfer status as SUCCESS.

RG-003-016 [À confirmer]
WHEN a transfer is successfully executed THE SYSTEM SHALL persist an audit log entry with the username, amount and motif.

RG-003-017 [À confirmer]
WHEN a transfer is successfully executed THE SYSTEM SHALL redirect the user to the dashboard page.

---

## Publication JMS et notifications asynchrones

---

RG-003-018 [À confirmer]
WHEN a transfer is persisted THE SYSTEM SHALL publish a VirementMessage to the JMS queue queue/virements.
  Note : publication best-effort — si JMS non configuré, le message est silencieusement abandonné ; pas de transaction distribuée XA

RG-003-019 [À confirmer]
IF the transfer amount is strictly less than 5000 EUR THEN THE SYSTEM SHALL send a CONFIRMATION notification to the connected user.

RG-003-020 [À confirmer]
WHEN the CONFIRMATION notification is generated AND the motif field is not empty THE SYSTEM SHALL include the motif in the notification message.

RG-003-021 [À confirmer]
IF the transfer amount is greater than or equal to 5000 EUR AND strictly less than 10000 EUR THEN THE SYSTEM SHALL send an ALERTE notification to the connected user.

RG-003-022 [À confirmer]
IF the transfer amount is greater than or equal to 10000 EUR THEN THE SYSTEM SHALL send a FRAUDE notification to the connected user.

---

## Détection de fraude en temps réel

---

RG-003-023 [À confirmer]
WHEN a transfer is successfully executed THE SYSTEM SHALL run a fraud detection check after the transfer is committed.
  Note : exceptions du contrôle fraude silencieusement ignorées ; le virement est déjà effectué — see JIRA-9202

RG-003-024 [À confirmer]
IF fraud alerts are detected for a transfer THEN THE SYSTEM SHALL warn the user of the number of alerts without blocking the transfer.

RG-003-025 [À confirmer]
IF the transfer amount is greater than or equal to 5000 EUR THEN THE SYSTEM SHALL raise a LARGE_AMOUNT fraud alert with a risk score of 40.

RG-003-026 [À confirmer]
IF the transfer amount is greater than or equal to 15000 EUR THEN THE SYSTEM SHALL raise a LARGE_AMOUNT fraud alert with a risk score of 80.

RG-003-027 [Suspect]
IF the customer's total transaction count exceeds 8 THEN THE SYSTEM SHALL raise a HIGH_FREQUENCY fraud alert with a risk score of 50.

RG-003-028 [À confirmer]
IF the target account is not registered as an active beneficiary for the customer THEN THE SYSTEM SHALL raise a NEW_BENEFICIARY fraud alert with a risk score of 35.

RG-003-029 [À confirmer]
IF the transfer is initiated before 06:00 or after 23:00 server time THEN THE SYSTEM SHALL raise an UNUSUAL_HOURS fraud alert with a risk score of 45.

RG-003-030 [À confirmer]
IF the transfer amount is greater than or equal to 1000 EUR AND is an exact multiple of 1000 THEN THE SYSTEM SHALL raise a ROUND_AMOUNT fraud alert with a risk score of 20.

RG-003-031 [À confirmer]
IF the target account number does not start with "FR" THEN THE SYSTEM SHALL raise a FOREIGN_IBAN fraud alert with a risk score of 30.

RG-003-032 [À confirmer]
IF a previous transaction from the same customer occurred less than 30 seconds ago THEN THE SYSTEM SHALL raise a RAPID_SUCCESSION fraud alert with a risk score of 55.
  Note    : cache in-process ConcurrentHashMap — race condition TOCTOU possible sous forte concurrence (JIRA-8910)

RG-003-033 [À confirmer]
IF the customer has performed 5 or more transactions exceeding 500 EUR in the last 60 minutes THEN THE SYSTEM SHALL raise a VELOCITY_CHECK fraud alert with a risk score of 65.

RG-003-034 [À confirmer]
IF the composite fraud risk score across all triggered fraud rules exceeds 75 THEN THE SYSTEM SHALL persist all triggered fraud alerts in the database.
