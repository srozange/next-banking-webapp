---
feature: FEAT-016
name: Administration — Gestion globale
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
beans: AdminBean, FraudAlertBean, BatchAdminBean
entry_point: /admin.xhtml
---

# Règles de gestion — FEAT-016 Administration — Gestion globale

---

## 1. Contrôle d'accès (RBAC)

RG-016-001 [Confirmé]
WHEN an unauthenticated user requests admin.xhtml
THE SYSTEM SHALL redirect to login.xhtml.

RG-016-002 [Confirmé]
WHEN a user with role CONSEILLER or CLIENT requests admin.xhtml
THE SYSTEM SHALL redirect to dashboard.xhtml without displaying the administration page.

RG-016-003 [Confirmé]
WHEN a user with role ADMIN requests admin.xhtml
THE SYSTEM SHALL grant access to all administration sections on the page.

---

## 2. Indicateurs KPI de la page d'administration

RG-016-004 [À confirmer]
WHEN admin.xhtml is loaded
THE SYSTEM SHALL display the count of all customers whose role is CLIENT as "Total clients".

RG-016-005 [À confirmer]
WHEN admin.xhtml is loaded
THE SYSTEM SHALL display the count of active customers whose role is CLIENT as the "actifs" sub-indicator.

RG-016-006 [À confirmer]
WHEN admin.xhtml is loaded
THE SYSTEM SHALL display the count of all customers whose role is CONSEILLER.

RG-016-007 [À confirmer]
WHEN admin.xhtml is loaded
THE SYSTEM SHALL display the count of loans with status EN_ATTENTE and their cumulative amount in EUR.

---

## 3. Pipeline Prêts — Approbation

RG-016-008 [Confirmé]
WHEN no loan has status EN_ATTENTE
THE SYSTEM SHALL display the message "Aucun pret en attente" in the loan pipeline section.

RG-016-009 [À confirmer]
IF a loan has status EN_ATTENTE AND the admin clicks "Approuver"
THEN THE SYSTEM SHALL change the loan status to EN_COURS, generate the full repayment schedule (LoanRepayment entries), and display the message "Prêt approuvé".

RG-016-010 [À confirmer]
IF a loan has status EN_ATTENTE AND the admin clicks "Refuser"
THEN THE SYSTEM SHALL change the loan status to REFUSE and display the message "Prêt refusé".

RG-016-011 [À confirmer]
IF a loan does not have status EN_ATTENTE AND an approve or refuse action is invoked
THEN THE SYSTEM SHALL ignore the request without raising an error.

RG-016-012 [À confirmer]
WHEN the admin approves a loan
THE SYSTEM SHALL persist an audit log entry with action "ADMIN_LOAN_APPROVE" and the loan id, in a separate transaction that survives a main-transaction rollback.

RG-016-013 [À confirmer]
WHEN the admin refuses a loan
THE SYSTEM SHALL persist an audit log entry with action "ADMIN_LOAN_REFUSE" and the loan id, in a separate transaction that survives a main-transaction rollback.

RG-016-014 [À confirmer]
IF an exception occurs during loan approval or refusal
THEN THE SYSTEM SHALL display a user-friendly error notification to the admin without exposing internal exception details.

---

## 4. Gestion des clients

RG-016-015 [Confirmé]
THE SYSTEM SHALL display in the "Gestion des clients" section only customers whose role is CLIENT, excluding CONSEILLER and ADMIN users.

RG-016-016 [Confirmé]
WHEN the client filter is empty
THE SYSTEM SHALL display all customers with role CLIENT.

RG-016-017 [Confirmé]
WHEN the admin enters a search term and clicks "Filtrer"
THE SYSTEM SHALL display only clients whose full name, username, or email contains the search term (case-insensitive, partial match).

RG-016-018 [À confirmer]
WHEN the filtered client list is empty
THE SYSTEM SHALL display "Aucun client trouve".

RG-016-019 [Confirmé]
WHEN the admin clicks "Detail" on a client row
THE SYSTEM SHALL display an inline detail panel with the following read-only fields: full name, username, email, phone, role, status, assigned advisor (if present), and "Client depuis" date (formatted dd/MM/yyyy).

RG-016-020 [À confirmer]
WHEN the admin clicks "Fermer" in the client detail panel
THE SYSTEM SHALL close the detail panel.

RG-016-021 [À confirmer]
WHEN a client is active AND the admin clicks "Desactiver"
THE SYSTEM SHALL set the client's active flag to false and display "Client désactivé".

RG-016-022 [À confirmer]
WHEN a client is inactive AND the admin clicks "Activer"
THE SYSTEM SHALL set the client's active flag to true and display "Client activé".

RG-016-023 [À confirmer]
WHEN the admin deactivates a client
THE SYSTEM SHALL persist an audit log entry with action "ADMIN_DEACTIVATE" and the customer id.

RG-016-024 [À confirmer]
WHEN the admin activates a client
THE SYSTEM SHALL persist an audit log entry with action "ADMIN_ACTIVATE" and the customer id.

RG-016-025 [Suspect]
WHEN a client displayed in the detail panel has an advisor
THE SYSTEM SHALL display the date labeled "Client depuis" using the field advisorSince from the Customer entity (column advisor_since).

---

## 5. Historique de tous les prêts (lecture seule)

RG-016-026 [À confirmer]
THE SYSTEM SHALL display all loans regardless of status in the "Historique de tous les prets" section, sorted by date of origin (dateDebut) descending.

---

## 6. Tous les utilisateurs du système (lecture seule)

RG-016-027 [À confirmer]
THE SYSTEM SHALL display all users of all roles (CLIENT, CONSEILLER, ADMIN) in the "Tous les utilisateurs du systeme" section.

---

## 7. Alertes Fraude — Revue administrative

RG-016-028 [Confirmé]
WHEN a fraud alert has status OPEN
THE SYSTEM SHALL display action buttons "Confirmer" and "Faux positif" for that alert.

RG-016-029 [Confirmé]
WHEN a fraud alert has status other than OPEN
THE SYSTEM SHALL display "Traité par {reviewedBy}" instead of action buttons.

RG-016-030 [À confirmer]
WHEN the admin clicks "Confirmer" on an OPEN fraud alert
THE SYSTEM SHALL set the alert status to CONFIRMED, record the current admin's full name as reviewer, and record the review timestamp.

RG-016-031 [À confirmer]
WHEN the admin clicks "Faux positif" on an OPEN fraud alert
THE SYSTEM SHALL set the alert status to FALSE_POSITIVE, record the current admin's full name as reviewer, and record the review timestamp.

RG-016-032 [À confirmer]
IF a confirm or dismiss action is invoked with a null alert id
THEN THE SYSTEM SHALL ignore the request without raising an error.

RG-016-033 [À confirmer]
WHEN the fraud alert count badge is displayed
THE SYSTEM SHALL show the count only when the number of OPEN alerts is strictly greater than 0.

RG-016-034 [Suspect]
WHEN the admin views the fraud alert table without a status filter
THE SYSTEM SHALL display all fraud alerts regardless of status.

---

## 8. Traitements nocturnes — Consultation (lecture seule)

RG-016-035 [À confirmer]
THE SYSTEM SHALL display the 50 most recent batch job log entries in the "Traitements nocturnes" section.

RG-016-036 [Confirmé]
WHEN no batch job log entry exists
THE SYSTEM SHALL display "Aucun traitement nocturne enregistre".

---

## 9. Catalogue produits bancaires (lecture seule)

RG-016-037 [À confirmer]
THE SYSTEM SHALL display active bank products grouped by category: Épargne, Crédit, Investissement, with a link to the full product page (/produits.xhtml).

---

## 10. Audit trail transversal

RG-016-038 [Confirmé]
THE SYSTEM SHALL persist every administrative action (loan approve, loan refuse, client activate, client deactivate) in the audit log using REQUIRES_NEW transaction semantics, so that the audit entry is preserved even if the main action transaction rolls back.
