# STATES — Loan

_Généré par retro-states depuis les transitions observées en base._
_Entité : `PUBLIC.LOANS` — Feature : FEAT-007 « Demande et suivi de prêts »_
_Source : commit c05512e — CodeGraph + ENUMS.md (SELECT DISTINCT)_

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE : demanderPret\n[montant > 0, taux ∈ [0,30], durée ∈ [1,360], motif non vide]
    EN_ATTENTE --> EN_COURS : approuverPret\n[scorer == true]
    EN_ATTENTE --> REFUSE : refuserPret\n[scorer == false]
    EN_COURS --> REMBOURSE : payerEcheance (dernière échéance)\n[COUNT(paye=false) == 0 après paiement]
    REFUSE --> [*]
    REMBOURSE --> [*]

    note right of EN_ATTENTE
      État transitoire
      Durée : quelques secondes (JMS async)
      Absent de la base de test
    end note

    note right of DEFAUT
      Défini dans l'enum LoanStatus
      Jamais atteint par le code
      État mort (transition manquante)
    end note
```

## Légende

| État | Libellé métier | Occurrences en base | Classification | Terminal |
| --- | --- | --- | --- | --- |
| `EN_ATTENTE` | En attente de traitement crédit | 0 | jamais atteint (transitoire) | Non |
| `EN_COURS` | Prêt actif en cours de remboursement | 2 | atteint | Non |
| `REFUSE` | Demande refusée par le service crédit | 0 | jamais atteint | Oui |
| `REMBOURSE` | Prêt intégralement remboursé | 0 | jamais atteint | Oui |
| `DEFAUT` | En défaut de paiement | 0 | jamais atteint — état mort (aucune transition) | Non |

## Détail des transitions

### T1 — `[*] → EN_ATTENTE` via `demanderPret`
- **Déclencheur** : soumission du formulaire pret.xhtml par le CLIENT
- **Acteur** : CLIENT (rôle requis par AppFilter)
- **Garde** : montant > 0 ; taux ∈ [0,30] ; durée ∈ [1,360] mois ; motif non vide
- **Effets de bord** : persist Loan, em.flush() (génère l'id), publish LoanApprovalMessage sur queue JMS

### T2 — `EN_ATTENTE → EN_COURS` via `approuverPret`
- **Déclencheur** : traitement JMS par LoanApprovalMDB (automatique) OU action ADMIN depuis admin.xhtml
- **Acteur** : LoanApprovalMDB (système) ou ADMIN
- **Garde** : `loan.status == EN_ATTENTE` ; scorer retourne vrai (montant ≤ plafond par type, durée ≤ 300 mois)
- **Règles scorer** : CONSOMMATION ≤ 25 000 €, AUTO ≤ 40 000 €, TRAVAUX ≤ 80 000 €, ETUDIANT ≤ 20 000 €, IMMOBILIER ≤ 600 000 €
- **Effets de bord** : génération de l'échéancier complet (N entrées LOAN_REPAYMENTS), notification client (NotificationStore), audit log `ADMIN_LOAN_APPROVE` si déclencheur ADMIN

### T3 — `EN_ATTENTE → REFUSE` via `refuserPret`
- **Déclencheur** : traitement JMS par LoanApprovalMDB (automatique) OU action ADMIN depuis admin.xhtml
- **Acteur** : LoanApprovalMDB (système) ou ADMIN
- **Garde** : `loan.status == EN_ATTENTE` ; scorer retourne faux (dépassement plafond ou durée > 300 mois)
- **Effets de bord** : notification client (NotificationStore), audit log `ADMIN_LOAN_REFUSE` si déclencheur ADMIN

### T4 — `EN_COURS → REMBOURSE` via `payerEcheance` (dernière échéance)
- **Déclencheur** : paiement de la dernière échéance depuis pret.xhtml par le CLIENT
- **Acteur** : CLIENT
- **Garde** : `echeance.paye == false` ; après paiement `COUNT(LoanRepayment WHERE paye=false AND loan_id=this) == 0`
- **Effets de bord** : `LoanRepayment.paye = true`, `LoanRepayment.datePaiement = now()`, `Loan.status = REMBOURSE`

## Transitions non observées en base

| De | Vers | Déclencheur | Note |
| --- | --- | --- | --- |
| `[*]` | `EN_ATTENTE` | `demanderPret` | État transitoire : absent de la base de test car le MDB traite la demande avant le chargement des données de test |
| `EN_ATTENTE` | `EN_COURS` | `approuverPret` | Aucun prêt EN_ATTENTE en base |
| `EN_ATTENTE` | `REFUSE` | `refuserPret` | Aucun prêt EN_ATTENTE en base |
| `EN_COURS` | `REMBOURSE` | `payerEcheance` | Aucun prêt totalement remboursé en base de test |
| `–` | `DEFAUT` | aucune | État mort : défini dans l'enum, CSS et filtre UI, mais aucune transition ne mène à cet état dans le code |

## État mort : DEFAUT

L'état `DEFAUT` est déclaré dans l'enum `LoanStatus` et référencé dans :
- `LoanBean.java` : filtre d'affichage `return s == EN_COURS || s == REMBOURSE || s == DEFAUT`
- `pret.xhtml` : classe CSS `.status-DEFAUT { background: #fce4ec; color: #b71c1c; }`

Aucune méthode `setStatus(LoanStatus.DEFAUT)` n'existe dans le code. L'état est préparé pour une future fonctionnalité de gestion des impayés (prêts en souffrance), non encore implémentée.

**Classification** : `intention-code-mort` — l'UI anticipe cet état mais aucun chemin de code ne l'atteint.

## Table liée : LOAN_REPAYMENTS

La table `LOAN_REPAYMENTS` est une sous-machine à états binaire (micro-état par échéance) :

| Colonne | Valeurs | Sémantique |
| --- | --- | --- |
| `PAYE` | `false` / `true` | Échéance non payée → payée |
| `DATE_PAIEMENT` | `NULL` / date | NULL tant que non payée |

Transition unique : `PAYE=false → PAYE=true` via `payerEcheance`, acteur CLIENT. Non-réversible.

## Table liée : CREDIT_SCORES

La table `CREDIT_SCORES` classe le client en catégorie de risque (`EXCELLENT`, `GOOD`, `MEDIUM`). Elle n'est pas utilisée par le scorer de `LoanApprovalMDB` — le scoring utilise uniquement le montant, le type et la durée. Aucune transition de prêt ne dépend du score en base.

## Questions métier

(intention-code-mort) L'état `DEFAUT` est prévu dans l'enum et l'UI mais sans transition dans le code : était-il implémenté dans une version antérieure ou est-ce une fonctionnalité à venir pour la gestion des impayés ?
  Portée : information
