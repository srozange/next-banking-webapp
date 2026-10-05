---
feature: FEAT-013
title: "Gestion des bénéficiaires externes"
generated_by: retro-validation
generated_at: "2026-10-03"
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# VALIDATION — FEAT-013 « Gestion des bénéficiaires externes »

Interface : Écran (`/beneficiaires.xhtml`). Formulaire d'ajout de bénéficiaire SEPA + tableau de liste.
Champs validés : 5 (libelle, nomTitulaire, iban, bic, banque).
Vérifications live exécutées : 3 essais de bypass maxlength (libelle, nomTitulaire, banque > 100 chars) — comportement serveur observé.

---

## 1. Validation front (par champ)

### libelle

VAL-013-01-libelle-F1
WHEN user submits add-beneficiary form with empty libelle THE SYSTEM SHALL reject the request and display "Le libellé est obligatoire."
  check   : {kind: required}
  trigger : soumission
  message : "Le libellé est obligatoire."
  observed: {outcome: rejected}

VAL-013-01-libelle-F2
WHEN user submits add-beneficiary form with libelle exceeding 100 characters THE SYSTEM SHALL prevent submission via HTML maxlength=100 attribute
  check   : {kind: max_length, value: 100}
  trigger : soumission
  observed: {outcome: rejected}

### nomTitulaire

VAL-013-02-nomTitulaire-F1
WHEN user submits add-beneficiary form with empty nomTitulaire THE SYSTEM SHALL reject the request and display "Le nom du titulaire est obligatoire."
  check   : {kind: required}
  trigger : soumission
  message : "Le nom du titulaire est obligatoire."
  observed: {outcome: rejected}

VAL-013-02-nomTitulaire-F2
WHEN user submits add-beneficiary form with nomTitulaire exceeding 100 characters THE SYSTEM SHALL prevent submission via HTML maxlength=100 attribute
  check   : {kind: max_length, value: 100}
  trigger : soumission
  observed: {outcome: rejected}

### iban

VAL-013-03-iban-F1
WHEN user submits add-beneficiary form with empty iban THE SYSTEM SHALL reject the request and display "L'IBAN est obligatoire."
  check   : {kind: required}
  trigger : soumission
  message : "L'IBAN est obligatoire."
  observed: {outcome: rejected}

VAL-013-03-iban-F2
WHEN user submits add-beneficiary form with an iban that fails MOD-97 validation THE SYSTEM SHALL reject the request and display "IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97)."
  check   : {kind: custom}
  trigger : soumission
  message : "IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97)."
  observed: {outcome: rejected}

VAL-013-03-iban-F3
WHEN user enters more than 40 characters in the iban field THE SYSTEM SHALL prevent additional input via HTML maxlength=40 attribute
  check   : {kind: max_length, value: 40}
  trigger : frappe

### bic

VAL-013-04-bic-F1
WHEN user submits add-beneficiary form with a non-empty bic that does not match pattern ^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$ THE SYSTEM SHALL reject the request and display "BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques."
  check   : {kind: pattern, value: "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$"}
  trigger : soumission
  message : "BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques."

VAL-013-04-bic-F2
WHEN user enters more than 11 characters in the bic field THE SYSTEM SHALL prevent additional input via HTML maxlength=11 attribute
  check   : {kind: max_length, value: 11}
  trigger : frappe

### banque

VAL-013-05-banque-F1
WHEN user submits add-beneficiary form with banque exceeding 100 characters THE SYSTEM SHALL prevent submission via HTML maxlength=100 attribute
  check   : {kind: max_length, value: 100}
  trigger : soumission
  observed: {outcome: rejected}

---

## 2. Validation back (schéma + applicatif)

Tableau `PUBLIC.EXTERNAL_BENEFICIARIES` (DATA-DICTIONARY.md) : LABEL NOT NULL, HOLDER_NAME NOT NULL, IBAN NOT NULL, BIC nullable, BANK nullable, SEPA_ZONE nullable, ACTIVE nullable, CREATED_AT nullable, CUSTOMER_ID NOT NULL (FK).

### libelle (→ colonne LABEL)

VAL-013-01-libelle-B1
IF libelle is null or empty at submission THEN THE SYSTEM SHALL reject the add request
  source  : applicatif
  check   : {kind: required}
  message : "Le libellé est obligatoire."
  observed: {outcome: rejected}

VAL-013-01-libelle-B2
IF libelle length exceeds 100 characters THEN THE SYSTEM SHALL reject the add request
  source  : schema
  check   : {kind: max_length, value: 100}
  observed: {outcome: rejected}

### nomTitulaire (→ colonne HOLDER_NAME)

VAL-013-02-nomTitulaire-B1
IF nomTitulaire is null or empty at submission THEN THE SYSTEM SHALL reject the add request
  source  : applicatif
  check   : {kind: required}
  message : "Le nom du titulaire est obligatoire."
  observed: {outcome: rejected}

VAL-013-02-nomTitulaire-B2
IF nomTitulaire length exceeds 100 characters THEN THE SYSTEM SHALL reject the add request
  source  : schema
  check   : {kind: max_length, value: 100}
  observed: {outcome: rejected}

### iban (→ colonne IBAN)

VAL-013-03-iban-B1
IF iban is null or empty at submission THEN THE SYSTEM SHALL reject the add request
  source  : applicatif
  check   : {kind: required}
  message : "L'IBAN est obligatoire."
  observed: {outcome: rejected}

VAL-013-03-iban-B2
IF iban fails MOD-97 validation (ISO 13616, length 15-34 chars ignoring spaces) THEN THE SYSTEM SHALL reject the add request
  source  : applicatif
  check   : {kind: custom}
  message : "IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97)."
  observed: {outcome: rejected}

### bic (→ colonne BIC)

VAL-013-04-bic-B1
IF bic is non-empty at submission and does not match pattern ^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$ THEN THE SYSTEM SHALL reject the add request
  source  : applicatif
  check   : {kind: pattern, value: "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$"}
  message : "BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques."

### banque (→ colonne BANK)

VAL-013-05-banque-B1
IF banque length exceeds 100 characters THEN THE SYSTEM SHALL reject the add request
  source  : schema
  check   : {kind: max_length, value: 100}
  observed: {outcome: rejected}

---

## 3. Matrice de divergence

| Champ | Kind | Verdict | Front IDs | Back IDs | Nature |
|---|---|---|---|---|---|
| libelle | required | identique | VAL-013-01-libelle-F1 | VAL-013-01-libelle-B1 | — |
| libelle | max_length | identique | VAL-013-01-libelle-F2 | VAL-013-01-libelle-B2 | dette-legacy — back schema-only : exception exposée si UI contournée |
| nomTitulaire | required | identique | VAL-013-02-nomTitulaire-F1 | VAL-013-02-nomTitulaire-B1 | — |
| nomTitulaire | max_length | identique | VAL-013-02-nomTitulaire-F2 | VAL-013-02-nomTitulaire-B2 | dette-legacy — back schema-only : exception exposée si UI contournée |
| iban | required | identique | VAL-013-03-iban-F1 | VAL-013-03-iban-B1 | — |
| iban | custom | identique (a_interpreter — voir note) | VAL-013-03-iban-F2 | VAL-013-03-iban-B2 | métier — les deux côtés implémentent le même algorithme MOD-97 via SepaValidator.isValidIban() |
| iban | max_length | front_seul | VAL-013-03-iban-F3 | — | métier — contrainte HTML redundante : le back rejette tout IBAN invalide via MOD-97 (qui borne la longueur à 15-34 chars) |
| bic | pattern | identique | VAL-013-04-bic-F1 | VAL-013-04-bic-B1 | — |
| bic | max_length | front_seul | VAL-013-04-bic-F2 | — | métier — contrainte HTML redondante : le back rejette tout BIC invalide via le pattern (qui borne à 11 chars max) |
| banque | max_length | identique | VAL-013-05-banque-F1 | VAL-013-05-banque-B1 | dette-legacy — back schema-only : exception exposée si UI contournée |

**Note a_interpreter (iban / custom)** : les deux côtés front et back implémentent le même algorithme MOD-97 via `SepaValidator.isValidIban()`. Le verdict mécanique `a_interpreter` est levé parce que `custom` ne se compare pas par valeur. Interprétation : **identique** — les deux côtés partagent la même implémentation de référence.

### Décompte
- identique : 7 (dont 3 avec annotation dette-legacy sur les max_length schema-only)
- front_seul : 2 (iban/max_length et bic/max_length — contraintes HTML redondantes avec le back)
- a_interpreter : 1 (iban/custom — tranché identique ci-dessus)
- divergent : 0 | back_seul : 0 | aucune_règle : 0

### Nature des lignes non-identique/aucune_règle
- métier : 3 (iban/custom identique, iban/max_length front_seul, bic/max_length front_seul)
- dette-legacy : 3 (libelle/max_length, nomTitulaire/max_length, banque/max_length)

---

## 4. Dettes legacy — max_length sans contrôle applicatif

Les champs libelle, nomTitulaire et banque ont chacun une contrainte DB `@Column(length=100)` mais aucune validation applicative de longueur dans `BeneficiaireBean` ni dans `ExternalBeneficiaryService`. Si le maxlength HTML (100) est contourné (ex : POST forgé ou modification DOM) :

- Le serveur tente de persister la valeur trop longue
- JPA déclenche `javax.persistence.PersistenceException: org.hibernate.exception.DataException: could not execute statement`
- Le bean affiche cette exception brute via `"Erreur lors de l'ajout du bénéficiaire : " + e.getMessage()` — la stack trace n'est pas visible mais le nom de la classe d'exception interne est exposé
- L'enregistrement n'est pas créé (rollback)

Comportement observé live : outcome `rejected` dans les trois cas, message exposant l'exception.
Règles concernées : VAL-013-01-libelle-B2, VAL-013-02-nomTitulaire-B2, VAL-013-05-banque-B1.

**À reproduire dans la cible** : ajouter une validation applicative explicite de longueur (avant la persistance) retournant un message métier, en plus de la contrainte DB.

---

## 5. Dette legacy — ownership non vérifié à la suppression

RG-013-035 [Suspect] documente l'absence de vérification d'appartenance dans `ExternalBeneficiaryService.deleteBeneficiary(id)`. Un POST HTTP forgé avec l'id d'un bénéficiaire d'un autre client permet la suppression. Non reproduit dans la cible.

---

## 6. Grille de sondes (test-harness.json)

Fichier : `retro/features/FEAT-013/test-harness.json` — généré par `generate-test-harness.py` avec `--validation-back` et `--validation-front`.
Total : 36 cas — verified : 12 | probe : 24.

Règles non couvertes par un cas automatique (attendu) :
- VAL-013-01-libelle-F1, VAL-013-02-nomTitulaire-F1, VAL-013-03-iban-F1 : règles front required — le back-B1 correspondant couvre le même cas (`source_ids` → B1) ; F1 non référencé par priorité back
- VAL-013-01-libelle-F2, VAL-013-02-nomTitulaire-F2 : règles front max_length — le back-B2 correspondant couvre le même cas ; F2 non référencé
- VAL-013-03-iban-B2, VAL-013-03-iban-F2 : contrainte `custom` (MOD-97) — le script ne peut pas générer automatiquement de valeur invalide MOD-97 standard ; à tester avec un IBAN forgé (ex. FR0000000000000000000000000)
- VAL-013-04-bic-F1 : contrainte `pattern` — pas de cas généré automatiquement pour le format BIC invalide ; à tester avec ex. BADFORMAT
- VAL-013-05-banque-F1 : contrainte front max_length=100 — aucun cas généré car `maxlength` est null dans fields-raw.json (snapshot n'a pas capturé l'attr HTML) ; couvert en pratique via B1
