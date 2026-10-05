---
feature: FEAT-022
name: "Surveillance soldes faibles et génération d'alertes"
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/ejb/NotificationService.java
---

# Messages et libellés — FEAT-022 Surveillance soldes faibles et génération d'alertes

Source unique : `src/main/java/com/accenture/bank/ejb/NotificationService.java`.

> Feature batch sans interface utilisateur. Les seuls messages visibles par l'utilisateur
> final sont les notifications persistées en base et affichées dans le centre de
> notifications (FEAT-002). Les journaux système (log4j) sont listés séparément pour
> traçabilité ; ils ne sont pas affichés à l'utilisateur.

---

## Notification persistée en base (type ACCOUNT_BALANCE)

Générée par `notifyLowBalance()` lorsque le solde d'un compte est strictement inférieur
au seuil (100 €). Référencée par RG-022-005.

| Identifiant | Champ | Valeur exacte (source) | Condition | Preuve |
|---|---|---|---|---|
| MSG-022-001 | Titre (`title`) | `Solde insuffisant sur votre compte` | Solde < 100 € — toujours | |
| MSG-022-002 | Corps (`message`) | `Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte.` | Solde < 100 € — toujours ; `{currentBalance}` = valeur du solde au moment du scan, `{lowBalanceThreshold}` = 100 (hardcodé) | 177 |
| MSG-022-003 | Type (`type`) | `ACCOUNT_BALANCE` | Toujours | |
| MSG-022-004 | Priorité (`priority`) | `HIGH` | Toujours | |

> `{currentBalance}` est interpolé par concaténation Java depuis la valeur `BigDecimal`
> telle quelle — aucun formatage `NumberFormat` appliqué à ce stade (ex. `50.00`).
> Le formatage éventuel est délégué à la couche d'affichage (FEAT-002).

---

## Journaux système (log4j — non visibles par l'utilisateur)

### Journaux INFO — déroulement normal du batch

| Identifiant | Niveau | Gabarit exact (source) | Déclencheur | Preuve |
|---|---|---|---|---|
| MSG-022-010 | INFO | `checkLowBalances: starting periodic low-balance scan` | Démarrage du timer EJB (toutes les 30 min) — référencé par RG-022-001 | |
| MSG-022-011 | INFO | `checkLowBalances: processing {N} customers` | Après chargement de la liste des clients actifs — `{N}` = nombre de clients | |
| MSG-022-012 | INFO | `Low balance detected for customerId={customerId} balance={currentBalance} threshold={lowBalanceThreshold}` | Avant création d'une notification de solde faible — référencé par RG-022-004 | 168 |
| MSG-022-013 | INFO | `checkLowBalances: scan complete` | Fin du scan, avant retour de la méthode — référencé par RG-022-001 | |

### Journaux WARN — anomalies non bloquantes

| Identifiant | Niveau | Gabarit exact (source) | Déclencheur | Preuve |
|---|---|---|---|---|
| MSG-022-020 | WARN | `notifyLowBalance called with null balance for customerId={customerId}` | Solde null reçu par `notifyLowBalance()` — référencé par RG-022-006 | |
| MSG-022-021 | WARN | `checkLowBalances: null balance for accountId={accountId}` | Solde null constaté dans la boucle de scan — référencé par RG-022-006 | |
| MSG-022-022 | WARN | `checkLowBalances: failed to load accounts for customerId={customerId}` | Exception lors du chargement des comptes d'un client — référencé par RG-022-008 | 355 |
| MSG-022-023 | WARN | `checkLowBalances: error notifying customerId={customerId} accountId={accountId}` | Exception lors de la création de la notification pour un compte — référencé par RG-022-009 | 378 |

### Journaux ERROR — erreur bloquante du batch

| Identifiant | Niveau | Gabarit exact (source) | Déclencheur | Preuve |
|---|---|---|---|---|
| MSG-022-030 | ERROR | `checkLowBalances: failed to load customers` | Exception lors du chargement de la liste des clients — référencé par RG-022-007 | |

### Journaux DEBUG — cycle de vie des notifications

| Identifiant | Niveau | Gabarit exact (source) | Déclencheur | Preuve |
|---|---|---|---|---|
| MSG-022-040 | DEBUG | `Low-balance notification persisted id={notifId}` | Après persistance réussie de la notification | |
