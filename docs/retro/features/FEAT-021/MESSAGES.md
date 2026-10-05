---
feature: FEAT-021
name: "Exécution des virements programmés"
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/ejb/RecurringTransferScheduler.java
  - src/main/java/com/accenture/bank/ejb/RecurringTransferService.java
  - src/main/java/com/accenture/bank/entity/RecurringFrequency.java
---

# Messages et libellés — FEAT-021 Exécution des virements programmés

Sources : `ejb/RecurringTransferScheduler.java` (déclencheur timer),
`ejb/RecurringTransferService.java` (exécution, descriptions de transactions, logs applicatifs),
`entity/RecurringFrequency.java` (libellés de fréquence).

> Cette fonctionnalité est un batch sans interface utilisateur directe. Les seuls textes
> visibles par les clients sont les descriptions de transactions écrites dans la base lors
> de l'exécution ; les autres messages sont des entrées de log serveur (Log4j 2).
> Les `{variables}` désignent des valeurs interpolées à l'exécution.

---

## Descriptions de transactions (user-visible — relevé de compte)

Ces chaînes sont construites par `executerVirement()` et persistées dans la colonne
`description` de la table `Transaction`. Elles sont ensuite visibles dans le relevé de compte
(FEAT-005) et le tableau de bord (FEAT-001).

> Note d'encodage : le source Java écrit « programme » sans accent — les textes ci-dessous
> reproduisent l'orthographe réelle du source. Le rendu navigateur peut afficher « programmé »
> selon l'encodage de la page ; la preuve `fichier:ligne` fait foi.

### Opération DEBIT (compte source)

| Identifiant | Gabarit exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-021-001 | `Virement programme vers {targetLabel}` | Toujours — si `motif` est null ou absent | 116 — référencé par RG-021-008 |
| MSG-021-002 | `Virement programme vers {targetLabel} - {motif}` | Si `motif != null` | 116 — référencé par RG-021-008 |

> `{targetLabel}` est le libellé libre du bénéficiaire saisi à la création du virement programmé
> (`RecurringTransfer.targetLabel`). `{motif}` est le motif libre optionnel (`RecurringTransfer.motif`).

### Opération CREDIT (compte cible interne)

| Identifiant | Gabarit exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-021-003 | `Virement programme de {sourceAccountNumber}` | Si `motif` est null ou absent — compte cible interne uniquement | 105 — référencé par RG-021-009 |
| MSG-021-004 | `Virement programme de {sourceAccountNumber} - {motif}` | Si `motif != null` — compte cible interne uniquement | 105 — référencé par RG-021-009 |

> `{sourceAccountNumber}` est le numéro de compte de l'émetteur (`BankAccount.accountNumber`).
> Aucune transaction CREDIT n'est créée si le compte cible est externe (IBAN absent de la base) — référencé par RG-021-007.

---

## Messages de log applicatif (Log4j 2 — non user-visible)

Les niveaux sont : `INFO` (opérations normales), `DEBUG` (trace cycle), `WARN` (anomalies non bloquantes).

### RecurringTransferScheduler

| Identifiant | Niveau | Gabarit exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-021-010 | INFO | `RecurringTransferScheduler demarre` | Démarrage du serveur (`@PostConstruct`) | |
| MSG-021-011 | DEBUG | `Verification virements programmes dus` | Début de chaque déclenchement du timer (toutes les 5 minutes) | |
| MSG-021-012 | WARN | `Erreur lors de l execution des virements programmes : {e.getMessage()}` | Exception non attrapée dans `executeAll()` remontée au scheduler — référencé par RG-021-013 | |

### RecurringTransferService

| Identifiant | Niveau | Gabarit exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-021-020 | WARN | `Solde insuffisant pour virement programme id={id}` | Solde du compte source strictement inférieur au montant du virement — référencé par RG-021-004 | |
| MSG-021-021 | INFO | `Virement programme execute id={id} montant={montant}` | Virement exécuté avec succès (débit + crédit ou débit seul + transaction créée) — référencé par RG-021-010 | |
| MSG-021-022 | WARN | `Echec execution virement programme id={id}` | Exception levée pendant `executerVirement()` — le virement est ignoré et la passe continue — référencé par RG-021-013 | |

> `{id}` est l'identifiant technique (`RecurringTransfer.id`). `{montant}` est la valeur `BigDecimal`
> non formatée (ex. `150.00`). Le logger `RecurringTransferService` est `com.accenture.bank.ejb.RecurringTransferService`.

---

## Libellés des énumérations

### Fréquence (RecurringFrequency)

La valeur `libelle` de l'enum est utilisée dans le log `MSG-021-023` (création, hors scope FEAT-021) et sera potentiellement affichée dans d'éventuels rapports. Elle est documentée ici pour constituer l'inventaire complet de la feature.

| Identifiant | Valeur enum | Libelle exact (source) | Preuve |
|---|---|---|---|
| MSG-021-030 | `HEBDOMADAIRE` | `Hebdomadaire` | |
| MSG-021-031 | `MENSUEL` | `Mensuel` | |
| MSG-021-032 | `BIMESTRIEL` | `Bimestriel` | |
| MSG-021-033 | `TRIMESTRIEL` | `Trimestriel` | |
| MSG-021-034 | `SEMESTRIEL` | `Semestriel` | |
| MSG-021-035 | `ANNUEL` | `Annuel` | |
