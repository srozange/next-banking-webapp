package fr.nextbanking.audit.adapter.out.persistance;

import fr.nextbanking.audit.application.port.out.EvenementsTraites;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code evenements_traites}, clé primaire {@code id_evenement}.
 */
@Component
public class EvenementsTraitesJdbc implements EvenementsTraites {

    private final JdbcClient jdbc;

    public EvenementsTraitesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean marquerTraite(UUID idEvenement) {
        return jdbc.sql("insert into evenements_traites (id_evenement) values (:id) on conflict do nothing")
                .param("id", idEvenement)
                .update() == 1;
    }
}
