package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement : un témoin a été écrit. Identifiant d'événement unique, témoin désigné par son identifiant
 * strictement positif, horodatage lu sur l'horloge.
 */
public record TemoinEcrit(UUID idEvenement, long temoinId, Instant horodatage) {

    public TemoinEcrit {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        if (temoinId <= 0) {
            throw new IllegalArgumentException("L'identifiant du témoin doit être strictement positif.");
        }
        if (horodatage == null) {
            throw new IllegalArgumentException("L'horodatage de l'événement est obligatoire.");
        }
    }
}
