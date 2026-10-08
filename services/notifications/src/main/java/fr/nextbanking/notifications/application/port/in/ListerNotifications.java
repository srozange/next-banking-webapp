package fr.nextbanking.notifications.application.port.in;

import fr.nextbanking.notifications.domain.NotificationDuClient;
import java.util.List;

/**
 * Port entrant : lister les notifications d'un utilisateur.
 */
public interface ListerNotifications {

    /** Notifications de l'utilisateur, la plus récente d'abord. */
    List<NotificationDuClient> lister(long clientId);
}
