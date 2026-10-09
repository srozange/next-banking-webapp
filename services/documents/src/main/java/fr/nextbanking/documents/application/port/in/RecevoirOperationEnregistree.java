package fr.nextbanking.documents.application.port.in;

import fr.nextbanking.documents.domain.OperationProjetee;
import java.util.UUID;

/** Port entrant : opération reçue sur {@code coeur-bancaire.operation-enregistree.v1}. */
public interface RecevoirOperationEnregistree {

    /** Issue du traitement. */
    enum Issue {
        /** Message déjà traité : ignoré (idempotence). */
        DEJA_TRAITE,
        /** Opération déjà présente dans la projection sous un autre événement : ignorée. */
        DEJA_PRESENTE,
        /** Opération enregistrée dans la projection. */
        ENREGISTRE
    }

    /** Alimente la projection des opérations, une seule fois par identifiant d'événement et d'opération. */
    Issue recevoir(UUID idEvenement, OperationProjetee operation);
}
