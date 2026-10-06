package fr.nextbanking.openbankingconseiller.adapter.out.persistance;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotProduits;
import fr.nextbanking.openbankingconseiller.domain.ProduitCredit;
import fr.nextbanking.openbankingconseiller.domain.ProduitEpargne;
import fr.nextbanking.openbankingconseiller.domain.ProduitInvestissement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : produits actifs de {@code bank_products} joints à leur table enfant. Tri par nom en ordre
 * binaire, indépendant de la collation de la base.
 */
@Component
public class DepotProduitsJdbc implements DepotProduits {

    private final JdbcClient jdbc;

    public DepotProduitsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProduitEpargne> listerEpargneActifs() {
        return jdbc.sql("select b.id, b.name, b.description, b.interest_rate, s.min_opening_deposit,"
                        + " s.capital_guaranteed, s.max_duration_months"
                        + " from bank_products b join savings_products s on s.id = b.id"
                        + " where b.active and b.product_type = 'EPARGNE' order by b.name collate \"C\"")
                .query(DepotProduitsJdbc::epargne)
                .list();
    }

    @Override
    public List<ProduitCredit> listerCreditActifs() {
        return jdbc.sql("select b.id, b.name, b.description, b.interest_rate, c.max_duration_months, c.max_ltv,"
                        + " c.requires_insurance"
                        + " from bank_products b join credit_products c on c.id = b.id"
                        + " where b.active and b.product_type = 'CREDIT' order by b.name collate \"C\"")
                .query(DepotProduitsJdbc::credit)
                .list();
    }

    @Override
    public List<ProduitInvestissement> listerInvestissementActifs() {
        return jdbc.sql("select b.id, b.name, b.description, i.market_sector, i.risk_level, i.expected_return"
                        + " from bank_products b join investment_products i on i.id = b.id"
                        + " where b.active and b.product_type = 'INVESTISSEMENT' order by b.name collate \"C\"")
                .query(DepotProduitsJdbc::investissement)
                .list();
    }

    private static ProduitEpargne epargne(ResultSet ligne, int numero) throws SQLException {
        return new ProduitEpargne(ligne.getLong("id"), ligne.getString("name"), ligne.getString("description"),
                ligne.getBigDecimal("interest_rate"), ligne.getBigDecimal("min_opening_deposit"),
                ligne.getObject("capital_guaranteed", Boolean.class),
                ligne.getObject("max_duration_months", Integer.class));
    }

    private static ProduitCredit credit(ResultSet ligne, int numero) throws SQLException {
        return new ProduitCredit(ligne.getLong("id"), ligne.getString("name"), ligne.getString("description"),
                ligne.getBigDecimal("interest_rate"), ligne.getObject("max_duration_months", Integer.class),
                ligne.getBigDecimal("max_ltv"), ligne.getObject("requires_insurance", Boolean.class));
    }

    private static ProduitInvestissement investissement(ResultSet ligne, int numero) throws SQLException {
        return new ProduitInvestissement(ligne.getLong("id"), ligne.getString("name"),
                ligne.getString("description"), ligne.getString("market_sector"),
                ligne.getObject("risk_level", Integer.class), ligne.getBigDecimal("expected_return"));
    }
}
