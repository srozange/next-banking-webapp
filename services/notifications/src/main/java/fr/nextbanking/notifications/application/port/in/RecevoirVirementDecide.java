package fr.nextbanking.notifications.application.port.in;

import java.util.UUID;

/**
 * Port entrant : recevoir la décision d'un virement publiée par le cœur bancaire, avec la notification rendue.
 */
public interface RecevoirVirementDecide {

    /** Notification rendue par le producteur. */
    record Notification(long clientId, String type, String titre, String message) {
    }

    /** Stocke la notification ; rend {@code false} si l'événement avait déjà été traité. */
    boolean recevoir(UUID idEvenement, long virementId, Notification notification);
}
