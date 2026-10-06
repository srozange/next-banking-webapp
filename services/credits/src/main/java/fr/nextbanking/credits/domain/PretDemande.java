package fr.nextbanking.credits.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement : une demande de prêt a été déposée. Identifiant d'événement unique, prêt désigné par son identifiant
 * strictement positif, horodatage lu sur l'horloge.
 */
public record PretDemande(UUID idEvenement, long pretId, Instant horodatage) {

    public PretDemande {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        if (pretId <= 0) {
            throw new IllegalArgumentException("L'identifiant du prêt doit être strictement positif.");
        }
        if (horodatage == null) {
            throw new IllegalArgumentException("L'horodatage de l'événement est obligatoire.");
        }
    }
}
