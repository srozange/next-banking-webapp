package fr.nextbanking.audit.application.port.out;

import java.util.UUID;

/**
 * Port sortant : registre des événements déjà traités (idempotence des consommateurs).
 */
public interface EvenementsTraites {

    /** Marque l'événement traité ; rend {@code false} s'il l'était déjà. */
    boolean marquerTraite(UUID idEvenement);
}
