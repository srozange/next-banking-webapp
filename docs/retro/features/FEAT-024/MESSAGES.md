---
feature: FEAT-024
name: "Traitement asynchrone approbation prêts (JMS)"
status: current
generated_by: retro-rules
date: 2026-10-03
---

# Messages et libellés — FEAT-024 « Traitement asynchrone approbation prêts (JMS) »

Sources :
`src/main/java/com/accenture/bank/mq/LoanApprovalMDB.java` (messages de notification dynamiques),
`src/main/java/com/accenture/bank/mq/LoanApprovalProducer.java` (logs internes),
`src/main/java/com/accenture/bank/ejb/LoanService.java` (logs internes),
`src/main/java/com/accenture/bank/mq/NotificationStore.java` (logs internes),
`src/main/java/com/accenture/bank/entity/LoanType.java` (libellés des types de prêt interpolés).

> Note : FEAT-024 est un traitement purement asynchrone en arrière-plan — aucun écran propre.
> Les seuls textes visibles par le client sont les messages de notification déposés dans le
> `NotificationStore` et affichés dans le centre de notifications du tableau de bord (FEAT-002).
> Les messages de log ci-dessous sont strictement internes (opérateur / observabilité).

---

## Messages de notification client (dynamiques, générés par LoanApprovalMDB)

Les gabarits ci-dessous sont construits en Java à l'exécution ; `{libelle}` est la valeur de
`LoanType.getLibelle()` (voir section « Libellés des types de prêt »), `{montant}` est la
représentation décimale du montant en EUR.

| Identifiant | Type | Gabarit du message | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-024-001 | `CONFIRMATION` | `Votre demande de pret {libelle} de {montant} EUR a ete approuvee par le service credit.` | Scoring crédit favorable (plafond et durée respectés) — référencé par RG-024-016 | 68 |
| MSG-024-002 | `ALERTE` | `Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive).` | Scoring crédit défavorable (plafond ou durée dépassés) — référencé par RG-024-017 | 75 |

> Règles associées : RG-024-016 (CONFIRMATION), RG-024-017 (ALERTE).

---

## Libellés des types de prêt (valeurs interpolées dans les notifications)

| Valeur enum `LoanType` | `getLibelle()` interpolé dans les messages | Preuve |
|---|---|---|
| `IMMOBILIER` | `Immobilier` | |
| `AUTO` | `Automobile` | |
| `CONSOMMATION` | `Consommation` | |
| `TRAVAUX` | `Travaux` | |
| `ETUDIANT` | `Etudiant` | |

---

## Messages de log internes (opérateur / observabilité — non visibles par le client)

| Identifiant | Niveau | Gabarit du message | Condition | Preuve |
|---|---|---|---|---|
| MSG-024-010 | `WARN` | `JMS non configure - demande pret non envoyee: {msg}` | `connectionFactory` ou `pretQueue` est null au moment de publier — référencé par RG-024-020 | |
| MSG-024-011 | `INFO` | `LoanApprovalProducer: demande envoyee -> {msg}` | Publication JMS réussie | |
| MSG-024-012 | `INFO` | `Pret soumis au service credit: id={loanId} type={type} montant={montant} duree={dureeMois}mois` | Prêt persisté et message JMS publié | 161 |
| MSG-024-013 | `INFO` | `LoanApprovalMDB: scoring pret id={loanId} type={loanType} montant={montant}` | Réception du message JMS par le MDB | 59 |
| MSG-024-014 | `INFO` | `LoanApprovalMDB: APPROUVE pret id={loanId}` | Approbation par le scorer | |
| MSG-024-015 | `WARN` | `LoanApprovalMDB: REFUSE pret id={loanId}` | Refus par le scorer | |
| MSG-024-016 | `INFO` | `Pret approuve par le service credit: id={loanId}` | Fin de `approuverPret()` | |
| MSG-024-017 | `INFO` | `Pret refuse par le service credit: id={loanId}` | Fin de `refuserPret()` | |
| MSG-024-018 | `ERROR` | `Erreur LoanApprovalMDB` (+ stack trace) | Exception inattendue dans `onMessage()` | |
| MSG-024-019 | `ERROR` | `Erreur publication JMS demande pret` (+ stack trace) | `JMSException` lors de la publication | 39 |
| MSG-024-020 | `INFO` | `NotificationStore: [{username}] +{type} -> {message}` | Chaque ajout de notification dans le store | |
