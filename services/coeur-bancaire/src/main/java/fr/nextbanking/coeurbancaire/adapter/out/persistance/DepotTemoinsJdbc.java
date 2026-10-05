package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : dépôt des témoins dans la table {@code temoins}.
 */
@Component
public class DepotTemoinsJdbc implements DepotTemoins {

    private final JdbcClient jdbc;

    public DepotTemoinsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long enregistrer(Temoin temoin) {
        return jdbc.sql("insert into temoins (libelle) values (:libelle) returning id")
                .param("libelle", temoin.libelle())
                .query(Long.class)
                .single();
    }
}
