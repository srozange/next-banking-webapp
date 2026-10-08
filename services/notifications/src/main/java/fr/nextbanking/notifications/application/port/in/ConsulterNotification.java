package fr.nextbanking.notifications.application.port.in;

import fr.nextbanking.notifications.domain.NotificationDuClient;

/**
 * Port entrant : consulter une notification de son propriétaire.
 */
public interface ConsulterNotification {

    /**
     * @throws fr.nextbanking.notifications.application.NotificationIntrouvable si la notification n'existe pas
     * @throws fr.nextbanking.notifications.application.AccesNotificationRefuse si elle appartient à un autre utilisateur
     */
    NotificationDuClient consulter(long clientId, long notificationId);
}
