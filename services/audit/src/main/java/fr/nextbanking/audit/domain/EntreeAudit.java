package fr.nextbanking.audit.domain;

import java.time.Instant;

/**
 * Ligne du journal d'audit : code d'action du catalogue, horodatage de l'enveloppe (Horloge du producteur),
 * détail, adresse IP d'origine (absente pour une action du système) et utilisateur.
 */
public record EntreeAudit(ActionAudit action, Instant horodatage, String detail, String adresseIp,
        String utilisateur) {

    public EntreeAudit {
        if (action == null) {
            throw new IllegalArgumentException("Le code d'action de l'entrée d'audit est obligatoire.");
        }
        if (horodatage == null) {
            throw new IllegalArgumentException("L'horodatage de l'entrée d'audit est obligatoire.");
        }
        if (utilisateur == null || utilisateur.isBlank()) {
            throw new IllegalArgumentException("L'utilisateur de l'entrée d'audit est obligatoire.");
        }
    }
}
