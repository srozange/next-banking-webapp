package fr.nextbanking.coeurbancaire.application.port.in;

import java.util.UUID;

/** Port entrant : état d'un client reçu sur {@code identite-clients.client-modifie.v1}. */
public interface RecevoirClientModifie {

    /** Issue du traitement. */
    enum Issue {
        /** Message déjà traité : ignoré (idempotence). */
        DEJA_TRAITE,
        /** Message dont {@code versionAgregat} n'est pas supérieure à celle de la projection : ignoré. */
        IGNORE_ANCIEN,
        /** Client enregistré dans la projection. */
        ENREGISTRE
    }

    /** Met à jour la projection des clients, une seule fois par identifiant d'événement. */
    Issue recevoir(UUID idEvenement, long clientId, String nomComplet, long versionAgregat);
}
