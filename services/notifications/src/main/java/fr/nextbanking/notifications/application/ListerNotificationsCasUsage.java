package fr.nextbanking.notifications.application;

import fr.nextbanking.notifications.application.port.in.ListerNotifications;
import fr.nextbanking.notifications.application.port.out.Notifications;
import fr.nextbanking.notifications.domain.NotificationDuClient;
import java.util.List;

/**
 * Cas d'usage : notifications du seul utilisateur demandeur, la plus récente d'abord (RG-002-011).
 */
public class ListerNotificationsCasUsage implements ListerNotifications {

    private final Notifications notifications;

    public ListerNotificationsCasUsage(Notifications notifications) {
        this.notifications = notifications;
    }

    @Override
    public List<NotificationDuClient> lister(long clientId) {
        return notifications.duClient(clientId);
    }
}
