package fr.nextbanking.notifications.application.port.in;

import java.util.UUID;

/**
 * Port entrant : recevoir un témoin publié par le cœur bancaire.
 */
public interface RecevoirTemoin {

    /** Enregistre le témoin reçu ; rend {@code false} si l'événement avait déjà été traité. */
    boolean recevoir(UUID idEvenement, long temoinId);
}
