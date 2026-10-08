package fr.nextbanking.notifications.application.port.out;

import fr.nextbanking.notifications.domain.Notification;
import fr.nextbanking.notifications.domain.NotificationDuClient;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : notifications conservées en base, par utilisateur.
 */
public interface Notifications {

    /** Place la notification en tête de la liste de son utilisateur. */
    void ajouter(Notification notification);

    /** Supprime les plus anciennes notifications de l'utilisateur au-delà de {@code plafond}. */
    void conserverLesPlusRecentes(long clientId, int plafond);

    /** Notifications du seul utilisateur, la plus récente d'abord (identifiant décroissant). */
    List<NotificationDuClient> duClient(long clientId);

    /** Notification désignée par son identifiant, quel que soit son utilisateur. */
    Optional<NotificationDuClient> parId(long id);

    /** Marque comme lues toutes les notifications de l'utilisateur, et de lui seul. */
    void marquerToutesLues(long clientId);

    /** Supprime toutes les notifications de l'utilisateur, et de lui seul. */
    void supprimerToutes(long clientId);
}
