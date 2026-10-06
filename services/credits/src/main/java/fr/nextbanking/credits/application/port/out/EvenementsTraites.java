package fr.nextbanking.credits.application.port.out;

import java.util.UUID;

/**
 * Port sortant : événements déjà consommés (idempotence).
 */
public interface EvenementsTraites {

    /** Marque l'événement traité dans la transaction courante ; faux s'il l'était déjà. */
    boolean marquerTraite(UUID idEvenement);
}
