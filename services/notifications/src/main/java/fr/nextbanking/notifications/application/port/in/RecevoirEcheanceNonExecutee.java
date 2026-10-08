package fr.nextbanking.notifications.application.port.in;

import java.util.UUID;

/**
 * Port entrant : recevoir l'échéance non exécutée d'un virement programmé, publiée par le cœur bancaire, avec la
 * notification ALERTE rendue (titre compris).
 */
public interface RecevoirEcheanceNonExecutee {

    /** Notification rendue par le producteur. */
    record Notification(long clientId, String type, String titre, String message) {
    }

    /** Stocke la notification ; rend {@code false} si l'événement avait déjà été traité. */
    boolean recevoir(UUID idEvenement, long virementProgrammeId, Notification notification);
}
