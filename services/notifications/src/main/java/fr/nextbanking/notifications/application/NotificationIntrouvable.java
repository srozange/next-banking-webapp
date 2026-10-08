package fr.nextbanking.notifications.application;

/**
 * Aucune notification ne porte l'identifiant demandé.
 */
public class NotificationIntrouvable extends RuntimeException {

    public NotificationIntrouvable(long notificationId) {
        super("Notification introuvable : " + notificationId);
    }
}
