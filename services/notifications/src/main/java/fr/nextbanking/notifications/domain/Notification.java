package fr.nextbanking.notifications.domain;

import java.time.Instant;

/**
 * Notification d'un utilisateur, stockée telle que rendue par le producteur (AD-25) : ce service ne compose aucun
 * texte. Le titre peut être absent ; une notification est créée non lue.
 */
public record Notification(long virementId, long clientId, String type, String titre, String message, boolean lue,
        Instant creeLe) {

    public Notification {
        if (virementId <= 0 || clientId <= 0) {
            throw new IllegalArgumentException("Le virement et l'utilisateur de la notification sont obligatoires.");
        }
        if (type == null || type.isBlank() || message == null || message.isBlank() || creeLe == null) {
            throw new IllegalArgumentException("Le type, le message et la date de la notification sont obligatoires.");
        }
    }

    /** Notification reçue : non lue. */
    public static Notification recue(long virementId, long clientId, String type, String titre, String message,
            Instant creeLe) {
        return new Notification(virementId, clientId, type, titre, message, false, creeLe);
    }
}
