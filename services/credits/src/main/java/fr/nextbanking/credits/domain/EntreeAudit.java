package fr.nextbanking.credits.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée du journal d'audit d'une décision de l'administrateur : identifiant d'événement, utilisateur (identifiant de
 * l'administrateur en texte), code d'action, adresse IP d'origine, horodatage lu sur le port Horloge, détail, et
 * identifiant du prêt (clé du message).
 */
public record EntreeAudit(UUID idEvenement, String utilisateur, ActionAudit action, String adresseIp,
        Instant horodatage, String detail, long pretId) {

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

    /** Entrée de l'approbation (ou du refus) du prêt par l'administrateur. */
    public static EntreeAudit decision(UUID idEvenement, String administrateur, boolean approbation,
            String adresseIp, Instant horodatage, long pretId) {
        return new EntreeAudit(idEvenement, administrateur,
                approbation ? ActionAudit.ADMIN_LOAN_APPROVE : ActionAudit.ADMIN_LOAN_REFUSE, adresseIp, horodatage,
                (approbation ? "Approbation prêt id=" : "Refus prêt id=") + pretId, pretId);
    }
}
