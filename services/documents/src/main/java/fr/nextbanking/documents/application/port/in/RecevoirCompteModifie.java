package fr.nextbanking.documents.application.port.in;

import fr.nextbanking.documents.domain.CompteProjete;
import java.util.UUID;

/** Port entrant : état d'un compte reçu sur {@code coeur-bancaire.compte-modifie.v1}. */
public interface RecevoirCompteModifie {

    /** Issue du traitement. */
    enum Issue {
        /** Message déjà traité : ignoré (idempotence). */
        DEJA_TRAITE,
        /** Message dont {@code versionAgregat} n'est pas supérieure à celle de la projection : ignoré. */
        IGNORE_ANCIEN,
        /** Compte enregistré dans la projection. */
        ENREGISTRE
    }

    /** Met à jour la projection des comptes, une seule fois par identifiant d'événement. */
    Issue recevoir(UUID idEvenement, CompteProjete compte);
}
