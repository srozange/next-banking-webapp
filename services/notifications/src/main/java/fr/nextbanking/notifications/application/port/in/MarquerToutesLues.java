package fr.nextbanking.notifications.application.port.in;

/**
 * Port entrant : marquer comme lues toutes les notifications d'un utilisateur.
 */
public interface MarquerToutesLues {

    /** Marque lues les notifications de l'utilisateur, et de lui seul. */
    void marquer(long clientId);
}
