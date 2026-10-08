package fr.nextbanking.notifications.application;

import fr.nextbanking.notifications.application.port.in.MarquerToutesLues;
import fr.nextbanking.notifications.application.port.out.Notifications;

/**
 * Cas d'usage : « Tout marquer lu » ne touche que les notifications de l'utilisateur (RG-002-016).
 */
public class MarquerToutesLuesCasUsage implements MarquerToutesLues {

    private final Notifications notifications;

    public MarquerToutesLuesCasUsage(Notifications notifications) {
        this.notifications = notifications;
    }

    @Override
    public void marquer(long clientId) {
        notifications.marquerToutesLues(clientId);
    }
}
