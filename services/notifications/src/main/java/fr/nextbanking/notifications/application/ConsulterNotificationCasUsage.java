package fr.nextbanking.notifications.application;

import fr.nextbanking.notifications.application.port.in.ConsulterNotification;
import fr.nextbanking.notifications.application.port.out.Notifications;
import fr.nextbanking.notifications.domain.NotificationDuClient;

/**
 * Cas d'usage : une notification n'est consultable que par son propriétaire (NFR-SEC-03).
 */
public class ConsulterNotificationCasUsage implements ConsulterNotification {

    private final Notifications notifications;

    public ConsulterNotificationCasUsage(Notifications notifications) {
        this.notifications = notifications;
    }

    @Override
    public NotificationDuClient consulter(long clientId, long notificationId) {
        NotificationDuClient notification = notifications.parId(notificationId)
                .orElseThrow(() -> new NotificationIntrouvable(notificationId));
        if (!notification.appartientA(clientId)) {
            throw new AccesNotificationRefuse(notificationId);
        }
        return notification;
    }
}
