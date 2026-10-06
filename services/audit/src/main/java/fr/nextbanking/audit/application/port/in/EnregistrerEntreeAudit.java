package fr.nextbanking.audit.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Port entrant : enregistrer une entrée d'audit reçue d'un service producteur.
 */
public interface EnregistrerEntreeAudit {

    /** Issue de l'enregistrement. */
    enum Issue {
        /** Entrée écrite dans le journal. */
        ENREGISTREE,
        /** Événement déjà traité : rien n'est écrit. */
        DEJA_TRAITEE,
        /** Code hors du catalogue fermé : rien n'est écrit. */
        CODE_INCONNU
    }

    /**
     * Enregistre l'entrée, de façon idempotente, avec l'horodatage de l'enveloppe.
     *
     * @throws IllegalArgumentException identifiant d'événement, horodatage ou utilisateur absent
     */
    Issue enregistrer(UUID idEvenement, String utilisateur, String action, String detail, String adresseIp,
            Instant horodatage);
}
