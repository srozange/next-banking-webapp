package fr.nextbanking.bourse.adapter.out.persistance;

import fr.nextbanking.bourse.application.port.out.DepotWatchlist;
import fr.nextbanking.bourse.domain.AjoutRefuse;
import fr.nextbanking.bourse.domain.NouvelleValeur;
import fr.nextbanking.bourse.domain.ValeurWatchlist;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : valeurs de la table {@code watchlist_items}. L'index unique sur {@code upper(symbol)} tranche
 * une course entre deux ajouts du même symbole : la violation devient le même refus que le contrôle du cas d'usage.
 */
@Component
public class DepotWatchlistJdbc implements DepotWatchlist {

    private final JdbcClient jdbc;

    public DepotWatchlistJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ValeurWatchlist> listerTrie() {
        // Ordre binaire, indépendant de la collation de la base : la ponctuation compte (SG.PA avant SGO.PA).
        return jdbc.sql("select id, symbol, name, market, currency from watchlist_items"
                        + " order by market collate \"C\", symbol collate \"C\"")
                .query(DepotWatchlistJdbc::valeur)
                .list();
    }

    @Override
    public boolean existeSymbole(String symbole) {
        return jdbc.sql("select exists (select 1 from watchlist_items where upper(symbol) = upper(:symbole))")
                .param("symbole", symbole)
                .query(Boolean.class)
                .single();
    }

    @Override
    public ValeurWatchlist ajouter(NouvelleValeur valeur) {
        try {
            long id = jdbc.sql("insert into watchlist_items (symbol, name, market, currency)"
                            + " values (:symbole, :nom, :marche, :devise) returning id")
                    .param("symbole", valeur.symbole())
                    .param("nom", valeur.nom())
                    .param("marche", valeur.marche())
                    .param("devise", valeur.devise())
                    .query(Long.class)
                    .single();
            return new ValeurWatchlist(id, valeur.symbole(), valeur.nom(), valeur.marche(), valeur.devise());
        } catch (DuplicateKeyException doublon) {
            throw AjoutRefuse.doublon(valeur.symbole());
        }
    }

    @Override
    public boolean supprimer(long id) {
        return jdbc.sql("delete from watchlist_items where id = :id").param("id", id).update() > 0;
    }

    private static ValeurWatchlist valeur(ResultSet ligne, int numero) throws SQLException {
        return new ValeurWatchlist(ligne.getLong("id"), ligne.getString("symbol"), ligne.getString("name"),
                ligne.getString("market"), ligne.getString("currency"));
    }
}
