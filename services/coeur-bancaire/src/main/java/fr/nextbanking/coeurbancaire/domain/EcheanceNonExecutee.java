package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement d'une échéance de virement programmé non exécutée (solde insuffisant, compte destinataire introuvable) et
 * la notification ALERTE à en faire, rendue. Topic {@code coeur-bancaire.echeance-non-executee.v1}, clé
 * {@code virementProgrammeId}.
 */
public record EcheanceNonExecutee(UUID idEvenement, long virementProgrammeId, NotificationVirement notification,
        Instant horodatage) {

    public EcheanceNonExecutee {
        if (idEvenement == null || notification == null || horodatage == null) {
            throw new IllegalArgumentException("L'identifiant, la notification et l'horodatage sont obligatoires.");
        }
        if (notification.titre() == null) {
            throw new IllegalArgumentException("Le titre de la notification d'une échéance est obligatoire.");
        }
    }
}
