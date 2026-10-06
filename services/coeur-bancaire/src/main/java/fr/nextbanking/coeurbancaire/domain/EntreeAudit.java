package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée du journal d'audit d'un virement : identifiant d'événement, utilisateur ({@code clientId} en texte),
 * code d'action, adresse IP d'origine, horodatage lu sur le port Horloge, détail, et identifiant du virement (clé du
 * message : l'ordre des entrées d'un virement est gardé par la partition).
 */
public record EntreeAudit(UUID idEvenement, String utilisateur, ActionAudit action, String adresseIp,
        Instant horodatage, String detail, long virementId) {

    public EntreeAudit {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement d'audit est obligatoire.");
        }
        if (utilisateur == null || utilisateur.isBlank()) {
            throw new IllegalArgumentException("L'utilisateur de l'entrée d'audit est obligatoire.");
        }
        if (action == null || horodatage == null || detail == null) {
            throw new IllegalArgumentException("Le code d'action, l'horodatage et le détail sont obligatoires.");
        }
    }
}
