package fr.nextbanking.notifications.adapter.out.persistance;

import fr.nextbanking.notifications.application.port.out.Notifications;
import fr.nextbanking.notifications.domain.Notification;
import fr.nextbanking.notifications.domain.NotificationDuClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code notifications} ; l'ordre est celui de l'identifiant, la plus récente en tête.
 */
@Component
public class NotificationsJdbc implements Notifications {

    private static final String LECTURE = "select id, client_id, type, titre, message, lue, cree_le from notifications";

    private final JdbcClient jdbc;

    public NotificationsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void ajouter(Notification notification) {
        jdbc.sql("""
                insert into notifications (virement_id, virement_programme_id, client_id, type, titre, message, lue,
                    cree_le)
                values (:virementId, :virementProgrammeId, :clientId, :type, :titre, :message, :lue, :creeLe)""")
                .param("virementId", notification.virementId())
                .param("virementProgrammeId", notification.virementProgrammeId())
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

    @Override
    public List<NotificationDuClient> duClient(long clientId) {
        return jdbc.sql(LECTURE + " where client_id = :clientId order by id desc")
                .param("clientId", clientId)
                .query(NotificationsJdbc::notification)
                .list();
    }

    @Override
    public Optional<NotificationDuClient> parId(long id) {
        return jdbc.sql(LECTURE + " where id = :id")
                .param("id", id)
                .query(NotificationsJdbc::notification)
                .optional();
    }

    @Override
    public void marquerToutesLues(long clientId) {
        jdbc.sql("update notifications set lue = true where client_id = :clientId and not lue")
                .param("clientId", clientId)
                .update();
    }

    @Override
    public void supprimerToutes(long clientId) {
        jdbc.sql("delete from notifications where client_id = :clientId")
                .param("clientId", clientId)
                .update();
    }

    private static NotificationDuClient notification(ResultSet ligne, int rang) throws SQLException {
        return new NotificationDuClient(ligne.getLong("id"), ligne.getLong("client_id"), ligne.getString("type"),
                ligne.getString("titre"), ligne.getString("message"), ligne.getBoolean("lue"),
                ligne.getObject("cree_le", OffsetDateTime.class).toInstant());
    }
}
