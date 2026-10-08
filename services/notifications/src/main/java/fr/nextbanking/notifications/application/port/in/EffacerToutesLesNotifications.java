package fr.nextbanking.notifications.application.port.in;

/**
 * Port entrant : effacer toutes les notifications d'un utilisateur.
 */
public interface EffacerToutesLesNotifications {

    /** Supprime les notifications de l'utilisateur, et de lui seul. */
    void effacer(long clientId);
}
