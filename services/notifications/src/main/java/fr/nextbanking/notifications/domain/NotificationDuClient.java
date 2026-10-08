package fr.nextbanking.notifications.domain;

import java.time.Instant;

/**
 * Notification conservée, telle que relue pour son utilisateur : porte l'identifiant attribué par la base (la plus
 * récente a le plus grand). Le titre est nul pour une notification de virement.
 */
public record NotificationDuClient(long id, long clientId, String type, String titre, String message, boolean lue,
        Instant creeLe) {

    /** Vrai si la notification appartient à l'utilisateur donné. */
    public boolean appartientA(long utilisateurId) {
        return clientId == utilisateurId;
    }
}
