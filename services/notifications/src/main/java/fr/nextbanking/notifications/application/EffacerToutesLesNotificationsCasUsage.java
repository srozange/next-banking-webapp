package fr.nextbanking.notifications.application;

import fr.nextbanking.notifications.application.port.in.EffacerToutesLesNotifications;
import fr.nextbanking.notifications.application.port.out.Notifications;

/**
 * Cas d'usage : « Effacer tout » ne supprime que les notifications de l'utilisateur (RG-002-017).
 */
public class EffacerToutesLesNotificationsCasUsage implements EffacerToutesLesNotifications {

    private final Notifications notifications;

    public EffacerToutesLesNotificationsCasUsage(Notifications notifications) {
        this.notifications = notifications;
    }

    @Override
    public void effacer(long clientId) {
        notifications.supprimerToutes(clientId);
    }
}
