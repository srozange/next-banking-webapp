package fr.nextbanking.notifications.adapter.out.persistance;

import fr.nextbanking.notifications.application.port.out.Notifications;
import fr.nextbanking.notifications.domain.Notification;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code notifications} ; l'ordre est celui de l'identifiant, la plus récente en tête.
 */
@Component
public class NotificationsJdbc implements Notifications {

    private final JdbcClient jdbc;

    public NotificationsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void ajouter(Notification notification) {
        jdbc.sql("""
                insert into notifications (virement_id, client_id, type, titre, message, lue, cree_le)
                values (:virementId, :clientId, :type, :titre, :message, :lue, :creeLe)""")
                .param("virementId", notification.virementId())
                .param("clientId", notification.clientId())
                .param("type", notification.type())
                .param("titre", notification.titre())
                .param("message", notification.message())
                .param("lue", notification.lue())
                .param("creeLe", Timestamp.from(notification.creeLe()))
                .update();
    }

    @Override
    public void conserverLesPlusRecentes(long clientId, int plafond) {
        jdbc.sql("""
                delete from notifications
                where client_id = :clientId
                  and id not in (select id from notifications where client_id = :clientId
                                 order by id desc limit :plafond)""")
                .param("clientId", clientId)
                .param("plafond", plafond)
                .update();
    }
}
