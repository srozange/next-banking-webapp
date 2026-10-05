---
feature: FEAT-023
title: "Traitement asynchrone des virements (JMS)"
interface: Transversal
status: current
retro_step: retro-rules
date: 2026-10-03
---

# Règles de gestion — FEAT-023 « Traitement asynchrone des virements (JMS) »

Flux transversal sans écran propre. Déclencheur : VirementBean.executeVirement → TransferService.executeTransfer → VirementProducer.publish → queue JMS `java:/queue/virements` → VirementProcessorMDB.onMessage → NotificationStore. Les seuils sont configurables via variables d'environnement ; les valeurs par défaut du code sont les valeurs de référence documentées ici.

---

## Publication JMS

RG-023-001 [Confirmé]
WHEN a transfer is successfully committed via TransferService.executeTransfer
THE SYSTEM SHALL publish a VirementMessage to the JMS queue `java:/queue/virements`
after flushing the persistence context (transfer id available).
             (queue virements déclarée : entries="java:/queue/virements")

RG-023-002 [Suspect]
IF VirementProducer cannot establish a JMS connection (ConnectionFactory or Queue is null)
THEN THE SYSTEM SHALL inform the caller of the publication failure.

---

## Traitement dans le MDB

RG-023-003 [Suspect]
WHEN VirementProcessorMDB receives a JMS message that is not an ObjectMessage
THE SYSTEM SHALL reject it and route it to the dead-letter queue.

RG-023-004 [Confirmé]
IF virement amount >= seuilFraudeEur (default: 10 000 EUR, env: SEUIL_FRAUDE_EUR)
THEN THE SYSTEM SHALL generate a FRAUDE-type notification for the transfer initiator.
             (seuilFraudeEur = resolve("SEUIL_FRAUDE_EUR", "10000"))
  (dependance-externe) Les seuils SEUIL_FRAUDE_EUR, SEUIL_ALERTE_EUR et SEUIL_PENDING_EUR sont-ils surchargés en production (valeurs par défaut code : 10 000, 5 000, 5 000 EUR) ?
    Portée : information

RG-023-005 [Confirmé]
IF seuilAlerteEur (default: 5 000 EUR, env: SEUIL_ALERTE_EUR) <= virement amount
   AND virement amount < seuilFraudeEur (default: 10 000 EUR)
THEN THE SYSTEM SHALL generate an ALERTE-type notification for the transfer initiator.

RG-023-006 [Confirmé]
IF virement amount < seuilAlerteEur (default: 5 000 EUR, env: SEUIL_ALERTE_EUR)
THEN THE SYSTEM SHALL generate a CONFIRMATION-type notification for the transfer initiator.

RG-023-007 [Confirmé]
WHEN a CONFIRMATION notification is generated
   AND the transfer motif is non-null and non-empty
THE SYSTEM SHALL include the motif text in the notification message.

RG-023-008 [Suspect]
WHEN VirementProcessorMDB encounters an exception during message processing
THE SYSTEM SHALL route the unprocessed message to the dead-letter queue for reprocessing.

---

## Comportement suspect — notification FRAUDE vs exécution effective

RG-023-009 [Suspect]
IF virement amount >= seuilFraudeEur (default: 10 000 EUR)
THEN THE SYSTEM SHALL suspend the transfer pending manual fraud verification before
applying any balance change.
             (message utilisateur : "bloqué pour vérification")

---

## Persistance des notifications

RG-023-010 [Confirmé]
THE SYSTEM SHALL retain at most maxNotificationsParUser notifications per user
(default: 20, env: MAX_NOTIFS_PAR_USER), discarding the oldest notification when
the limit is reached and inserting the new one at the head of the list.
             (maxNotificationsParUser = resolve("MAX_NOTIFS_PAR_USER", "20"))

RG-023-011 [Confirmé]
THE SYSTEM SHALL store notifications in a non-persistent in-memory store scoped to the
application lifecycle; all notifications are lost on server restart.
