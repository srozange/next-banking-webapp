---
feature: FEAT-026
name: "Audit trail (journalisation des actions)"
status: current
extracted_at: 2026-10-03
extractor: retro-rules
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# MESSAGES — FEAT-026 « Audit trail (journalisation des actions) »

_Préoccupation transversale sans écran propre — aucun message n'est affiché à l'utilisateur au titre de l'audit trail. Ce document recense les chaînes persistées dans la table `audit_logs` : codes d'action (champ `action`) et gabarits de détail (champ `details`). Sources : `src/main/java/com/accenture/bank/web/LoginBean.java`, `src/main/java/com/accenture/bank/web/AdminBean.java`, `src/main/java/com/accenture/bank/web/VirementBean.java`, `src/main/java/com/accenture/bank/ejb/AuditService.java`, `src/main/java/com/accenture/bank/entity/AuditLog.java`._

---

## Codes d'action audit (champ `action` de `audit_logs`)

| Code | Déclencheur | Règle | Preuve |
|---|---|---|---|
| `LOGIN` | Authentification réussie | RG-026-001 | |
| `LOGIN_FAILED` | Tentative d'authentification échouée | RG-026-002 | |
| `LOGOUT` | Déconnexion de session | RG-026-003 | |
| `ADMIN_DEACTIVATE` | Désactivation d'un compte client par l'administrateur | RG-026-004 | |
| `ADMIN_ACTIVATE` | Activation d'un compte client par l'administrateur | RG-026-005 | |
| `ADMIN_LOAN_APPROVE` | Approbation d'une demande de prêt par l'administrateur | RG-026-006 | |
| `ADMIN_LOAN_REFUSE` | Refus d'une demande de prêt par l'administrateur | RG-026-007 | |
| `VIREMENT` | Soumission d'un virement réussi | RG-026-008 | |

---

## Gabarits de détail (champ `details` de `audit_logs`)

Les `{variables}` désignent des valeurs dynamiques substituées au moment de la persistance.

| Code action | Texte exact du champ `details` | Preuve |
|---|---|---|
| `LOGIN` | `Connexion réussie` | |
| `LOGIN_FAILED` | `Tentative échouée` | |
| `LOGOUT` | `Déconnexion` | |
| `ADMIN_DEACTIVATE` | `Désactivation client id={customerId}` | |
| `ADMIN_ACTIVATE` | `Activation client id={customerId}` | |
| `ADMIN_LOAN_APPROVE` | `Approbation prêt id={loanId}` | |
| `ADMIN_LOAN_REFUSE` | `Refus prêt id={loanId}` | |
| `VIREMENT` | `Montant: {amount} - Motif: {motif}` | |

> **Note** : pour `LOGIN` et `LOGIN_FAILED`, le champ `ipAddress` de `audit_logs` est renseigné (surcharge 4-paramètres de `AuditService.log`). Pour les autres codes, `ipAddress` est `null` — dette-legacy documentée en RG-026-010.
