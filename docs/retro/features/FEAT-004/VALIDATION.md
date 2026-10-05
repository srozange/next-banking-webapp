---
feature: FEAT-004
name: Virements programmés
step: retro-validation
date: 2026-10-03
sources:
  - retro/features/FEAT-004/fields-raw.json
  - retro/features/FEAT-004/validation-front.json
  - retro/features/FEAT-004/validation-back.json
  - retro/features/FEAT-004/divergence-matrix.json
  - retro/features/FEAT-004/test-harness.json
---

# FEAT-004 — Virements programmés : Validation

---

## 1. Validation front (couche JSF bean — RecurringTransferBean.java)

Toutes les règles front se déclenchent à la **soumission** du formulaire (POST JSF). Aucune activation dynamique (blur, frappe) n'a été observée.

### srcAcc — Compte débiteur

**VAL-004-01-srcAcc-F1**
WHEN user submits the form with no source account selected THE SYSTEM SHALL reject and display "Veuillez selectionner un compte source"
  Source : applicatif
  Message : "Veuillez selectionner un compte source"
  Trigger : soumission

### montant — Montant (EUR)

**VAL-004-04-montant-F1**
WHEN user submits the form with null montant THE SYSTEM SHALL reject and display "Montant invalide"
  Source : applicatif
  Message : "Montant invalide"
  Trigger : soumission

**VAL-004-04-montant-F2**
WHEN user submits the form with montant less than or equal to zero THE SYSTEM SHALL reject and display "Montant invalide"
  Source : applicatif
  Message : "Montant invalide"
  Trigger : soumission

### freq — Fréquence

**VAL-004-06-freq-F1**
WHEN user submits the form THE SYSTEM SHALL accept only frequency values from the fixed list: HEBDOMADAIRE, MENSUEL, BIMESTRIEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL
  Source : applicatif — JSF combobox options
  Trigger : soumission

### targetNum — IBAN / N compte bénéficiaire

**VAL-004-02-targetNum-F1**
WHEN user submits the form with empty targetNum field THE SYSTEM SHALL reject and display "Veuillez saisir le numero de compte destinataire"
  Source : applicatif
  Message : "Veuillez saisir le numero de compte destinataire"
  Trigger : soumission

### targetLabel — Libellé bénéficiaire

**VAL-004-03-targetLabel-F1**
WHEN user submits the form with empty targetLabel field THE SYSTEM SHALL reject and display "Veuillez saisir le libelle du beneficiaire"
  Source : applicatif
  Message : "Veuillez saisir le libelle du beneficiaire"
  Trigger : soumission

### dateExec — Date de première exécution

**VAL-004-05-dateExec-F1**
WHEN user submits the form with empty dateExec field THE SYSTEM SHALL reject and display "Veuillez saisir la date de premiere execution"
  Source : applicatif
  Message : "Veuillez saisir la date de premiere execution"
  Trigger : soumission

**VAL-004-05-dateExec-F2** [dette-legacy]
WHEN user submits a dateExec value not matching the pattern yyyy-MM-dd THE SYSTEM SHALL reject and display "Format de date invalide (attendu : yyyy-MM-dd)"
  Source : applicatif — SimpleDateFormat("yyyy-MM-dd")
  Message : "Format de date invalide (attendu : yyyy-MM-dd)"
  Trigger : soumission

### motif — Motif (optionnel)

_Aucune règle front_ : champ optionnel, aucun contrôle de longueur dans le bean ni dans l'UI.

---

## 2. Validation back (couche EJB service + schéma JPA/H2)

### srcAcc → source_account_id

**VAL-004-01-srcAcc-B1**
THE SYSTEM SHALL require a non-null source account identifier
  Source : schema — `@JoinColumn(name="source_account_id", nullable=false)`
  Colonne : source_account_id

**VAL-004-01-srcAcc-B2**
IF the source account does not belong to the authenticated customer THEN THE SYSTEM SHALL reject the recurring transfer creation
  Source : applicatif — ownership check `source.getCustomer().getId().equals(customerId)`

### montant → MONTANT

**VAL-004-04-montant-B1**
THE SYSTEM SHALL require a non-null montant
  Source : applicatif — double validation back-end

**VAL-004-04-montant-B2**
IF montant is less than or equal to zero THEN THE SYSTEM SHALL reject the recurring transfer creation
  Source : applicatif

### freq → FREQUENCE

**VAL-004-06-freq-B1**
THE SYSTEM SHALL store only valid RecurringFrequency enum values: HEBDOMADAIRE, MENSUEL, BIMESTRIEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL
  Source : schema — `@Enumerated(EnumType.STRING)` + `nullable=false`
  Colonne : frequence

### targetNum → TARGET_ACCOUNT_NUMBER

**VAL-004-02-targetNum-B1**
THE SYSTEM SHALL require a non-null target account number
  Source : schema — `@Column(nullable=false, length=34)`
  Colonne : target_account_number

**VAL-004-02-targetNum-B2** [dette-legacy]
IF targetNum exceeds 34 characters THEN THE SYSTEM SHALL reject the submission
  Source : schema — `@Column(length=34)` — aucune validation bean ni maxlength UI
  Colonne : target_account_number
  Message observé : "Erreur : javax.persistence.PersistenceException: org.hibernate.exception.DataException: could not execute statement"
  Observed : {"outcome": "rejected", "http_status": 200}

### targetLabel → TARGET_LABEL

**VAL-004-03-targetLabel-B1**
THE SYSTEM SHALL require a non-null beneficiary label
  Source : schema — `@Column(nullable=false, length=100)`
  Colonne : target_label

**VAL-004-03-targetLabel-B2** [dette-legacy]
IF targetLabel exceeds 100 characters THEN THE SYSTEM SHALL reject the submission
  Source : schema — `@Column(length=100)` — aucune validation bean ni maxlength UI
  Colonne : target_label
  Message observé : "Erreur : javax.persistence.PersistenceException: org.hibernate.exception.DataException: could not execute statement"
  Observed : {"outcome": "rejected", "http_status": 200}

### dateExec → PROCHAIN_EXECUTION

**VAL-004-05-dateExec-B1**
THE SYSTEM SHALL require a non-null first execution date
  Source : schema — `@Column(name="prochain_execution", nullable=false)`
  Colonne : prochain_execution

### motif → MOTIF

**VAL-004-07-motif-B1** [dette-legacy]
IF motif exceeds 255 characters THEN THE SYSTEM SHALL reject the submission
  Source : schema — `@Column(length=255)` — aucune validation bean ni maxlength UI
  Message observé : "Erreur : javax.persistence.PersistenceException: org.hibernate.exception.DataException: could not execute statement"
  Observed : {"outcome": "rejected", "http_status": 200}

---

## 3. Matrice de divergence

_Générée par build-divergence-matrix.py — 12 lignes._

| Champ | Kind | Verdict | Front | Back | Nature |
|---|---|---|---|---|---|
| srcAcc | required | identique | VAL-004-01-srcAcc-F1 | VAL-004-01-srcAcc-B1 | — |
| srcAcc | custom | a_interpreter | — | VAL-004-01-srcAcc-B2 | métier — voir §3.1 |
| montant | required | identique | VAL-004-04-montant-F1 | VAL-004-04-montant-B1 | — |
| montant | min=0.01 | identique | VAL-004-04-montant-F2 | VAL-004-04-montant-B2 | — |
| freq | enum | identique | VAL-004-06-freq-F1 | VAL-004-06-freq-B1 | — |
| targetNum | required | identique | VAL-004-02-targetNum-F1 | VAL-004-02-targetNum-B1 | — |
| targetNum | max_length=34 | back_seul | — | VAL-004-02-targetNum-B2 | dette-legacy — maxlength absent côté front; rejet avec message technique (HTTP 200) |
| targetLabel | required | identique | VAL-004-03-targetLabel-F1 | VAL-004-03-targetLabel-B1 | — |
| targetLabel | max_length=100 | back_seul | — | VAL-004-03-targetLabel-B2 | dette-legacy — même défaut |
| dateExec | required | identique | VAL-004-05-dateExec-F1 | VAL-004-05-dateExec-B1 | — |
| dateExec | format=yyyy-MM-dd | front_seul | VAL-004-05-dateExec-F2 | — | dette-legacy — format incohérent avec l'app; aucun contournement HTTP possible (conversion bean) |
| motif | max_length=255 | back_seul | — | VAL-004-07-motif-B1 | dette-legacy — même défaut |

**Totaux :** identique 7 | divergent 0 | front_seul 1 | back_seul 3 | a_interpreter 1 | aucune_règle 0

---

### 3.1 Interprétation des lignes `a_interpreter`

**srcAcc / custom / VAL-004-01-srcAcc-B2 — vérification de propriété du compte**

Le front restreint le sélecteur aux comptes du client authentifié (RG-004-002, chargement dynamique). Le back vérifie que `source.getCustomer().getId().equals(customerId)`. Ces deux contrôles portent sur la même règle métier (un client ne peut débiter que ses propres comptes), mais agissent à des niveaux différents : le front filtre l'affichage, le back enforce l'invariant côté serveur.

**Verdict** : cette contrainte est `back_seul` au sens fonctionnel — le front ne valide pas la propriété, il restreint seulement les options visibles. En cas de manipulation de la requête POST (id de compte forgé), seul le back résiste.

**Nature : métier** — la vérification de propriété doit être reproduite côté back dans la réécriture. Le front peut continuer à filtrer le dropdown, mais le contrôle serveur est non négociable (IDOR sans lui).

---

## 4. Harness de tests

**42 cas générés — verified : 12 | probe : 30**

Règles non couvertes par le harness (aucun cas de test généré mécaniquement) :
`VAL-004-01-srcAcc-B1`, `VAL-004-01-srcAcc-B2`, `VAL-004-02-targetNum-B1`, `VAL-004-03-targetLabel-B1`,
`VAL-004-04-montant-B1`, `VAL-004-04-montant-F1`, `VAL-004-04-montant-F2`,
`VAL-004-05-dateExec-B1`, `VAL-004-05-dateExec-F2`, `VAL-004-06-freq-F1`

_Ces règles nécessitent des cas de test manuels (bypass JSF, appels directs) pour atteindre les validations back derrière les contrôles front._

Aucun avertissement (observation contraire à l'attendu).

---

## 5. Corrections apportées par rapport à RG.md

| Règle RG | Constat RG | Correction VALIDATION |
|---|---|---|
| RG-004-070 | "exception JPA propagée en HTTP 500" | Faux : la try/catch de intercepte l'exception → HTTP 200, message "Erreur : javax.persistence.PersistenceException…" affiché dans la page |
| RG-004-071 | "exception JPA propagée" | Même correction : HTTP 200, message technique affiché |
| RG-004-072 | "exception JPA propagée" | Même correction : HTTP 200, message technique affiché |
