---
feature: FEAT-023
name: Traitement asynchrone des virements (JMS)
status: current
step: retro-rules
date: 2026-10-03
sources:
  - src/main/java/com/accenture/bank/mq/VirementProcessorMDB.java
  - src/main/java/com/accenture/bank/mq/VirementProducer.java
  - src/main/java/com/accenture/bank/mq/BankNotification.java
  - src/main/java/com/accenture/bank/mq/NotificationStore.java
  - src/main/java/com/accenture/bank/config/AppConfig.java
---

# FEAT-023 — Traitement asynchrone des virements (JMS) : Messages et libellés

Feature transversale sans écran propre. Les seuls textes visibles à l'utilisateur sont les messages de notification affichés dans le tableau de bord (FEAT-001). Tous les autres textes ci-dessous sont des messages de log internes (non visibles par l'utilisateur).

---

## Messages de notification affichés à l'utilisateur (tableau de bord)

Ces textes sont construits par le MDB et stockés dans NotificationStore ; ils apparaissent dans le tableau de bord de l'utilisateur initiateur du virement.

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Notification FRAUDE | Virement de {amount} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation. | Montant >= SEUIL_FRAUDE_EUR (défaut : 10 000 EUR) |
| Notification ALERTE | Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. | Montant >= SEUIL_ALERTE_EUR (défaut : 5 000 EUR) et < SEUIL_FRAUDE_EUR |
| Notification CONFIRMATION | Virement de {amount} EUR vers le compte {targetAccountNumber} confirme | Montant < SEUIL_ALERTE_EUR (défaut : 5 000 EUR), motif absent ou vide |
| Notification CONFIRMATION (avec motif) | Virement de {amount} EUR vers le compte {targetAccountNumber} confirme (motif : {motif}) | Idem, lorsque le motif est renseigné |

---

## Icônes et classes CSS des notifications (rendu tableau de bord)

Ces valeurs sont utilisées par le composant d'affichage (FEAT-001) pour styliser les notifications.

| Type de notification | Icône | Classe CSS |
| --- | --- | --- |
| CONFIRMATION | ✅ | notif-confirmation |
| ALERTE | ⚠️ | notif-alerte |
| FRAUDE | 🚨 | notif-fraude |


---

## Messages de log internes (non visibles par l'utilisateur)

| Niveau | Texte exact | Contexte observé |
| --- | --- | --- |
| INFO | VirementProducer: message publie -> {VirementMessage.toString()} | Publication JMS réussie |
| WARN | JMS non configure - message non publie: {VirementMessage.toString()} | ConnectionFactory ou Queue null |
| ERROR | Erreur publication JMS virement | JMSException lors de l'envoi (avec stack trace) |
| INFO | VirementProcessorMDB: traitement virement id={transferId} montant={amount} EUR user={username} | Entrée dans onMessage |
| WARN | VirementProcessorMDB: FRAUDE suspectee - {VirementMessage.toString()} | Montant >= SEUIL_FRAUDE_EUR |
| INFO | VirementProcessorMDB: transaction importante - {VirementMessage.toString()} | Montant >= SEUIL_ALERTE_EUR |
| INFO | VirementProcessorMDB: confirmation - {VirementMessage.toString()} | Montant normal |
| ERROR | Erreur VirementProcessorMDB | Exception non gérée dans onMessage (avec stack trace) |
| INFO | NotificationStore: [{username}] +{type} -> {message} | Ajout d'une notification en mémoire |
