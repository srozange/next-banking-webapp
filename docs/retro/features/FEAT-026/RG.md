---
feature: FEAT-026
title: "Audit trail (journalisation des actions)"
interface: Transversal
status: current
retro_step: retro-rules
date: 2026-10-03
---

# Règles de gestion — FEAT-026 « Audit trail (journalisation des actions) »

Préoccupation transversale sans écran propre. Déclencheur : toute action utilisateur significative (authentification, administration, virement). Implémentation centrale : `AuditService.log()` appelé depuis LoginBean, AdminBean, VirementBean. Persistance en table `audit_logs` via JPA, transaction indépendante.

---

## Authentification

RG-026-001 [Confirmé]
WHEN a user successfully authenticates
THE SYSTEM SHALL persist an audit entry recording the username, the action code "LOGIN", the client IP address, and the server timestamp.

RG-026-002 [Confirmé]
IF a user authentication attempt fails
THEN THE SYSTEM SHALL persist an audit entry recording the attempted username, the action code "LOGIN_FAILED", the client IP address, and the server timestamp.

RG-026-003 [Confirmé]
WHEN an authenticated session ends (user logout)
THE SYSTEM SHALL persist an audit entry recording the username, the action code "LOGOUT", and the server timestamp.
  Note    : audit conditionnel sur currentCustomer != null ; IP non capturée (voir RG-026-010)

---

## Administration

RG-026-004 [Confirmé]
WHEN an administrator deactivates a client account
THE SYSTEM SHALL persist an audit entry recording the administrator username, the action code "ADMIN_DEACTIVATE", and the target client ID.

RG-026-005 [Confirmé]
WHEN an administrator activates a client account
THE SYSTEM SHALL persist an audit entry recording the administrator username, the action code "ADMIN_ACTIVATE", and the target client ID.

RG-026-006 [Confirmé]
WHEN an administrator approves a loan application
THE SYSTEM SHALL persist an audit entry recording the administrator username, the action code "ADMIN_LOAN_APPROVE", and the loan ID.

RG-026-007 [Confirmé]
WHEN an administrator refuses a loan application
THE SYSTEM SHALL persist an audit entry recording the administrator username, the action code "ADMIN_LOAN_REFUSE", and the loan ID.

---

## Virement

RG-026-008 [Confirmé]
WHEN a fund transfer is successfully submitted
THE SYSTEM SHALL persist an audit entry recording the username, the action code "VIREMENT", the transfer amount, and the transfer motif.

---

## Contraintes techniques transversales

RG-026-009 [Confirmé]
THE SYSTEM SHALL persist each audit entry within an independent database transaction, separate from the enclosing operation transaction, so that an audit entry is retained even if the triggering operation rolls back.

RG-026-010 [Suspect]
WHEN any user action is audited
THE SYSTEM SHALL record the originating client IP address in the audit entry.

RG-026-011 [Confirmé]
THE SYSTEM SHALL retain audit entries as a write-only store; no retrieval, filtering, or display API for audit log entries is provided within the application.
