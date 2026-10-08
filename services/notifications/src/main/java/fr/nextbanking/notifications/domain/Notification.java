package fr.nextbanking.notifications.domain;

import java.time.Instant;

/**
 * Notification d'un utilisateur, stockée telle que rendue par le producteur (AD-25) : ce service ne compose aucun
 * texte. Elle se rattache à un virement ou à un virement programmé (exactement un des deux identifiants). Le titre
 * peut être absent ; une notification est créée non lue.
 */
public record Notification(Long virementId, Long virementProgrammeId, long clientId, String type, String titre,
        String message, boolean lue, Instant creeLe) {

    public Notification {
        if ((virementId == null) == (virementProgrammeId == null)) {
            throw new IllegalArgumentException(
                    "La notification se rattache à un virement ou à un virement programmé, et à un seul.");
        }
        if ((virementId != null && virementId <= 0) || (virementProgrammeId != null && virementProgrammeId <= 0)
                || clientId <= 0) {
            throw new IllegalArgumentException("Le virement et l'utilisateur de la notification sont obligatoires.");
        }
        if (type == null || type.isBlank() || message == null || message.isBlank() || creeLe == null) {
            throw new IllegalArgumentException("Le type, le message et la date de la notification sont obligatoires.");
        }
    }

    /** Notification reçue pour un virement : non lue. */
    public static Notification recue(long virementId, long clientId, String type, String titre, String message,
            Instant creeLe) {
        return new Notification(virementId, null, clientId, type, titre, message, false, creeLe);
    }

    /** Notification reçue pour une échéance de virement programmé : non lue. */
    public static Notification recuePourEcheance(long virementProgrammeId, long clientId, String type, String titre,
            String message, Instant creeLe) {
        return new Notification(null, virementProgrammeId, clientId, type, titre, message, false, creeLe);
    }
}
