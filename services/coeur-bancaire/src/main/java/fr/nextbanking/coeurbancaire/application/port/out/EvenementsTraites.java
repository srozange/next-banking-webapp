package fr.nextbanking.coeurbancaire.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Port sortant : événements déjà traités par les consommateurs du service (idempotence).
 */
public interface EvenementsTraites {

    /** Marque l'événement traité, dans la transaction courante ; faux s'il l'était déjà. */
    boolean marquerTraite(UUID idEvenement, Instant traiteLe);
}
