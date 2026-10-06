package fr.nextbanking.coeurbancaire.application.port.in;

import java.util.UUID;

/**
 * Port entrant : traiter un virement remis au traitement asynchrone ({@code coeur-bancaire.virement-a-traiter.v1}).
 */
public interface TraiterVirementATraiter {

    /** Issue du traitement. */
    enum Issue {
        /** Message déjà traité : ignoré (idempotence). */
        DEJA_TRAITE,
        /** Virement inconnu : ignoré. */
        VIREMENT_INCONNU,
        /** Virement déjà exécuté, déjà débité : aucun effet et aucune notification. */
        DEJA_EXECUTE
    }

    /** Traite le message, une seule fois par identifiant d'événement. */
    Issue traiter(UUID idEvenement, long virementId);
}
