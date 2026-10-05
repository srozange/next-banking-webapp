---
feature: FEAT-007
feature_name: Demande et suivi de prets
step: retro-validation
generated_at: 2026-10-03
sources:
  - src/main/java/com/accenture/bank/web/LoanBean.java
  - src/main/java/com/accenture/bank/ejb/LoanService.java
  - src/main/java/com/accenture/bank/entity/Loan.java
  - src/main/webapp/pret.xhtml
  - retro/00-global/DATA-DICTIONARY.md (PUBLIC.LOANS, PUBLIC.LOAN_REPAYMENTS)
  - retro/features/FEAT-007/fields-raw.json
---

# VALIDATION — FEAT-007 Demande et suivi de prets

## 1. Validation front — règles EARS

> Toutes les règles front sont déclenchées par `soumission` (action JSF sur le bouton).
> Aucun listener `blur`, `keyup` ou `focusout` n'est présent dans pret.xhtml.
> Les champs numériques utilisent `<f:convertNumber>` ; les conversions JSF s'appliquent avant les vérifications du bean.

### Section Simulateur de prêt

**simMontant** — Montant du simulateur

VAL-007-01-simMontant-F1
WHEN user submits simulation with null or empty simMontant THE SYSTEM SHALL reject and display "Montant invalide"
  check   : {kind: required}
  trigger : soumission

VAL-007-01-simMontant-F2
IF simMontant is less than or equal to zero THEN THE SYSTEM SHALL reject and display "Montant invalide"
  check   : {kind: min, value: 0.01}
  trigger : soumission

**simTaux** — Taux annuel du simulateur

VAL-007-02-simTaux-F1
IF simTaux is less than zero THEN THE SYSTEM SHALL reject and display "Taux invalide (0-30%)"
  check   : {kind: min, value: 0}
  trigger : soumission

VAL-007-02-simTaux-F2
IF simTaux is greater than 30 THEN THE SYSTEM SHALL reject and display "Taux invalide (0-30%)"
  check   : {kind: max, value: 30}
  trigger : soumission

**simDuree** — Durée du simulateur

VAL-007-03-simDuree-F1
IF simDuree is less than or equal to zero THEN THE SYSTEM SHALL reject and display "Duree invalide (1-360 mois)"
  check   : {kind: min, value: 1}
  trigger : soumission

VAL-007-03-simDuree-F2
IF simDuree is greater than 360 THEN THE SYSTEM SHALL reject and display "Duree invalide (1-360 mois)"
  check   : {kind: max, value: 360}
  trigger : soumission

### Section Demande de prêt

**typePret** — Type de prêt

VAL-007-04-typePret-F1
THE SYSTEM SHALL constrain typePret to enum values IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT via combobox with CONSOMMATION as default
  check   : {kind: enum, value: [IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT]}
  trigger : soumission

**montant** — Montant du prêt

VAL-007-05-montant-F1
WHEN user submits loan request with null or empty montant THE SYSTEM SHALL reject and display "Montant invalide"
  check   : {kind: required}
  trigger : soumission

VAL-007-05-montant-F2
IF montant is less than or equal to zero THEN THE SYSTEM SHALL reject and display "Montant invalide"
  check   : {kind: min, value: 0.01}
  trigger : soumission

**taux** — Taux annuel du prêt

VAL-007-06-taux-F1
IF taux is less than zero THEN THE SYSTEM SHALL reject and display "Taux invalide"
  check   : {kind: min, value: 0}
  trigger : soumission

VAL-007-06-taux-F2
IF taux is greater than 30 THEN THE SYSTEM SHALL reject and display "Taux invalide"
  check   : {kind: max, value: 30}
  trigger : soumission

**duree** — Durée du prêt

VAL-007-07-duree-F1
IF duree is less than or equal to zero THEN THE SYSTEM SHALL reject and display "Duree invalide"
  check   : {kind: min, value: 1}
  trigger : soumission

VAL-007-07-duree-F2
IF duree is greater than 360 THEN THE SYSTEM SHALL reject and display "Duree invalide"
  check   : {kind: max, value: 360}
  trigger : soumission

**objetPret** — Objet du prêt (= colonne MOTIF en base)

VAL-007-08-objetPret-F1
WHEN user submits loan request with null or blank objetPret THE SYSTEM SHALL reject and display "Veuillez indiquer un motif"
  check   : {kind: required}
  trigger : soumission

---

## 2. Validation back — contraintes serveur EARS

> Source `applicatif` = code Java (LoanBean/LoanService) ; `schema` = contrainte DB via JPA.
> Toutes les vérifications applicatif ont deux passages : LoanBean (présentation) + LoanService.simuler() (métier) pour les champs numériques.

### Section Simulateur de prêt

VAL-007-01-simMontant-B1
IF simMontant is null THEN THE SYSTEM SHALL reject the simulation
  Source  : applicatif
  check   : {kind: required}

VAL-007-01-simMontant-B2
IF simMontant is less than or equal to zero THEN THE SYSTEM SHALL reject the simulation
  Source  : applicatif
  check   : {kind: min, value: 0.01}

VAL-007-02-simTaux-B1
IF tauxAnnuel is less than zero THEN THE SYSTEM SHALL reject the simulation
  Source  : applicatif
  check   : {kind: min, value: 0}

VAL-007-02-simTaux-B2
IF tauxAnnuel is greater than 30 THEN THE SYSTEM SHALL reject the simulation
  Source  : applicatif
  check   : {kind: max, value: 30}

VAL-007-03-simDuree-B1
IF dureeMois is less than or equal to zero THEN THE SYSTEM SHALL reject the simulation
  Source  : applicatif
  check   : {kind: min, value: 1}

VAL-007-03-simDuree-B2
IF dureeMois is greater than 360 THEN THE SYSTEM SHALL reject the simulation
  Source  : applicatif
  check   : {kind: max, value: 360}

### Section Demande de prêt

VAL-007-04-typePret-B1
THE SYSTEM SHALL store typePret only as one of the valid enum values: IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT
  Source  : schema (TYPE VARCHAR NOT NULL @Enumerated)
  check   : {kind: enum, value: [IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT]}

VAL-007-05-montant-B1
IF montant is null THEN THE SYSTEM SHALL reject the loan request
  Source  : applicatif
  check   : {kind: required}

VAL-007-05-montant-B2
IF montant is less than or equal to zero THEN THE SYSTEM SHALL reject the loan request
  Source  : applicatif
  check   : {kind: min, value: 0.01}

VAL-007-05-montant-B3
THE SYSTEM SHALL store montant as NOT NULL
  Source  : schema
  check   : {kind: required}

VAL-007-06-taux-B1
IF tauxAnnuel is less than zero THEN THE SYSTEM SHALL reject the loan request
  Source  : applicatif
  check   : {kind: min, value: 0}

VAL-007-06-taux-B2
IF tauxAnnuel is greater than 30 THEN THE SYSTEM SHALL reject the loan request
  Source  : applicatif
  check   : {kind: max, value: 30}

VAL-007-07-duree-B1
IF dureeMois is less than or equal to zero THEN THE SYSTEM SHALL reject the loan request
  Source  : applicatif
  check   : {kind: min, value: 1}

VAL-007-07-duree-B2
IF dureeMois is greater than 360 THEN THE SYSTEM SHALL reject the loan request
  Source  : applicatif
  check   : {kind: max, value: 360}

VAL-007-08-objetPret-B1
THE SYSTEM SHALL reject objetPret values exceeding 255 characters
  Source  : schema (MOTIF VARCHAR(255) NULLABLE — DATA-DICTIONARY.md PUBLIC.LOANS)
  check   : {kind: max_length, value: 255}
  observed: {outcome: "rejected"} — message affiché : "Erreur : javax.persistence.PersistenceException: org.hibernate.exception.DataException: could not execute statement"

---

## 3. Matrice de divergence

_Générée par build-divergence-matrix.py depuis validation-front.json et validation-back.json._

| Champ | Kind | Verdict | IDs front | IDs back | Conséquence | Nature |
|---|---|---|---|---|---|---|
| duree | max | identique | VAL-007-07-duree-F2 | VAL-007-07-duree-B2 | — | — |
| duree | min | identique | VAL-007-07-duree-F1 | VAL-007-07-duree-B1 | — | — |
| montant | min | identique | VAL-007-05-montant-F2 | VAL-007-05-montant-B2 | — | — |
| montant | required | identique | VAL-007-05-montant-F1 | VAL-007-05-montant-B1 | — | — |
| objetPret | max_length | back_seul | — | VAL-007-08-objetPret-B1 | front accepte ce que back rejette — rejetée par le serveur (observé à l'écran) | dette-legacy — exception Hibernate exposée ; pas de message clair pour l'utilisateur ; à corriger dans la réécriture par une validation au niveau service |
| objetPret | required | front_seul | VAL-007-08-objetPret-F1 | — | back accepte ce que front refuse — inféré (appel direct non effectué) : LoanService ne vérifie pas le motif ; schéma NULLABLE → accepté si contournement | métier — le champ doit être obligatoire dans la réécriture à la fois côté service et côté schéma (NOT NULL) |
| simDuree | max | identique | VAL-007-03-simDuree-F2 | VAL-007-03-simDuree-B2 | — | — |
| simDuree | min | identique | VAL-007-03-simDuree-F1 | VAL-007-03-simDuree-B1 | — | — |
| simMontant | min | identique | VAL-007-01-simMontant-F2 | VAL-007-01-simMontant-B2 | — | — |
| simMontant | required | identique | VAL-007-01-simMontant-F1 | VAL-007-01-simMontant-B1 | — | — |
| simTaux | max | identique | VAL-007-02-simTaux-F2 | VAL-007-02-simTaux-B2 | — | — |
| simTaux | min | identique | VAL-007-02-simTaux-F1 | VAL-007-02-simTaux-B1 | — | — |
| taux | max | identique | VAL-007-06-taux-F2 | VAL-007-06-taux-B2 | — | — |
| taux | min | identique | VAL-007-06-taux-F1 | VAL-007-06-taux-B1 | — | — |
| typePret | enum | identique | VAL-007-04-typePret-F1 | VAL-007-04-typePret-B1 | — | — |

**Totaux** : 15 lignes — identique : 13 | back_seul : 1 | front_seul : 1 | divergent : 0 | a_interpreter : 0 | aucune_règle : 0

### Qualification des divergences

**objetPret / max_length / back_seul** — Nature : dette-legacy
- Contexte : aucune règle front ni service ne contrôle la longueur du motif. La contrainte est uniquement au niveau schéma (@Column length=255). Le dépassement provoque une PersistenceException Hibernate non gérée, exposée telle quelle à l'utilisateur.
- Inférence live (vérification 2026-10-03) : soumission avec 256 chars → "Erreur : javax.persistence.PersistenceException: org.hibernate.exception.DataException: could not execute statement"
- Action réécriture : ajouter validation `@Size(max=255)` au niveau service + schéma cible NOT NULL (si motif devient obligatoire) + message utilisateur clair ("Le motif ne doit pas dépasser 255 caractères").

**objetPret / required / front_seul** — Nature : métier
- Contexte : la vérification required est uniquement dans LoanBean.demanderPret() (présentation). LoanService.demanderPret() ne vérifie pas le motif. La colonne MOTIF est NULLABLE en base.
- Inférence code (bypass non testable via navigateur — aucun attribut HTML required à contourner) : si contournement, la LoanService crée le prêt avec motif null. Outcome back : accepted.
- Action réécriture : le motif doit être obligatoire côté service (validation @NotBlank au niveau @Service) et la colonne doit être NOT NULL en base.

---

## 4. Harness de test réconcilié

_Fichier_ : `retro/features/FEAT-007/test-harness.json`

| Statut | Nb |
|---|---|
| verified | 16 |
| probe | 37 |
| **Total** | **53** |

**Règles non couvertes** : VAL-007-01-simMontant-F1, F2 · VAL-007-02-simTaux-F1, F2 · VAL-007-03-simDuree-F1, F2 · VAL-007-04-typePret-F1 · VAL-007-05-montant-B3, F1, F2 · VAL-007-06-taux-F1, F2 · VAL-007-07-duree-F1, F2

> Explication : les règles front citées ont un homologue back de même `kind` et `source: applicatif`, qui prend la priorité dans le harness (règle back > règle front). Elles sont documentées pour la réécriture mais ne génèrent pas de cas de test supplémentaires. `VAL-007-05-montant-B3` (schema NOT NULL) est redondant avec B1 (applicatif required) qui prend la priorité.
