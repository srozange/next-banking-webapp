package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code plafonds_budget}.
 */
@Component
public class PlafondsJdbc implements Plafonds {

    private final JdbcClient jdbc;

    public PlafondsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long compter(long clientId, String mois) {
        return jdbc.sql("select count(*) from plafonds_budget where client_id = :clientId and mois = :mois")
                .param("clientId", clientId)
                .param("mois", mois)
                .query(Long.class)
                .single();
    }
}
