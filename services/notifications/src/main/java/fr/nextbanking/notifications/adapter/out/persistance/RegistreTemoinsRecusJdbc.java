package fr.nextbanking.notifications.adapter.out.persistance;

import fr.nextbanking.notifications.application.port.out.RegistreTemoinsRecus;
import fr.nextbanking.notifications.domain.TemoinRecu;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : registre des témoins reçus dans la table {@code temoins_recus}.
 */
@Component
public class RegistreTemoinsRecusJdbc implements RegistreTemoinsRecus {

    private final JdbcClient jdbc;

    public RegistreTemoinsRecusJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enregistrer(TemoinRecu temoin) {
        jdbc.sql("insert into temoins_recus (temoin_id) values (:temoinId)")
                .param("temoinId", temoin.temoinId())
                .update();
    }
}
