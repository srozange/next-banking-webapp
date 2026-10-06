package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import java.sql.Timestamp;
import java.time.Instant;
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
    public boolean marquerTraite(UUID idEvenement, Instant traiteLe) {
        return jdbc.sql("""
                insert into evenements_traites (id_evenement, traite_le) values (:id, :traiteLe)
                on conflict do nothing""")
                .param("id", idEvenement)
                .param("traiteLe", Timestamp.from(traiteLe))
                .update() == 1;
    }
}
