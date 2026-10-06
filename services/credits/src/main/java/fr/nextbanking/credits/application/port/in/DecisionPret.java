package fr.nextbanking.credits.application.port.in;

import java.util.UUID;

/**
 * Port entrant : décision automatique sur une demande de prêt transmise (FR-024-06 à FR-024-08).
 */
public interface DecisionPret {

    /**
     * Décide de la demande ; rend faux si l'événement avait déjà été traité (rien n'est refait).
     */
    boolean decider(UUID idEvenement, long pretId);
}
