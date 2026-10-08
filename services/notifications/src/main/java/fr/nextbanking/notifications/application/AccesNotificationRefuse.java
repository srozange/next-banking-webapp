package fr.nextbanking.notifications.application;

/**
 * La notification demandée appartient à un autre utilisateur.
 */
public class AccesNotificationRefuse extends RuntimeException {

    public AccesNotificationRefuse(long notificationId) {
        super("Accès refusé à la notification " + notificationId);
    }
}
