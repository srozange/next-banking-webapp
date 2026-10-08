package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import java.util.List;
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

    @Override
    public List<Plafond> duMois(long clientId, String mois) {
        return jdbc.sql("select id, categorie, plafond from plafonds_budget where client_id = :clientId and mois = :mois")
                .param("clientId", clientId)
                .param("mois", mois)
                .query((rs, n) -> new Plafond(rs.getLong("id"), CategorieBudget.valueOf(rs.getString("categorie")), mois,
                        new Montant(rs.getBigDecimal("plafond").setScale(Montant.DECIMALES))))
                .list();
    }

    @Override
    public Plafond enregistrer(long clientId, CategorieBudget categorie, String mois, Montant plafond) {
        long id = jdbc.sql("""
                insert into plafonds_budget (client_id, categorie, mois, plafond)
                values (:clientId, :categorie, :mois, :plafond)
                on conflict (client_id, categorie, mois) do update set plafond = excluded.plafond
                returning id""")
                .param("clientId", clientId)
                .param("categorie", categorie.name())
                .param("mois", mois)
                .param("plafond", plafond.valeur())
                .query(Long.class)
                .single();
        return new Plafond(id, categorie, mois, plafond);
    }

    @Override
    public boolean retirer(long clientId, long plafondId) {
        return jdbc.sql("delete from plafonds_budget where id = :id and client_id = :clientId")
                .param("id", plafondId)
                .param("clientId", clientId)
                .update() > 0;
    }
}
