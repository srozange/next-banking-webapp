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
        DEJA_EXECUTE,
        /** Virement dans un autre état que LIBÉRÉ ou EXÉCUTÉ : ignoré, sans effet. */
        ETAT_INATTENDU,
        /** Virement libéré, débité et passé à EXÉCUTÉ avec ses deux opérations ; aucune notification. */
        EXECUTE_APRES_LIBERATION,
        /** Virement libéré dont le solde est devenu insuffisant : REJETÉ, sans débit ni notification. */
        REJETE_SOLDE_INSUFFISANT
    }

    /** Traite le message, une seule fois par identifiant d'événement. */
    Issue traiter(UUID idEvenement, long virementId);
}
