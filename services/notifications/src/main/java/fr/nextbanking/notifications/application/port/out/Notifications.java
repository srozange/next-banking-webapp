package fr.nextbanking.notifications.application.port.out;

import fr.nextbanking.notifications.domain.Notification;

/**
 * Port sortant : notifications conservées en base, par utilisateur.
 */
public interface Notifications {

    /** Place la notification en tête de la liste de son utilisateur. */
    void ajouter(Notification notification);

    /** Supprime les plus anciennes notifications de l'utilisateur au-delà de {@code plafond}. */
    void conserverLesPlusRecentes(long clientId, int plafond);
}
