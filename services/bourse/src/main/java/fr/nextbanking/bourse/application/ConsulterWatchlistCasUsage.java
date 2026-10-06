package fr.nextbanking.bourse.application;

import fr.nextbanking.bourse.application.port.in.ConsulterWatchlist;
import fr.nextbanking.bourse.application.port.out.DepotWatchlist;
import fr.nextbanking.bourse.domain.ValeurWatchlist;
import java.util.List;

/**
 * Lit la watchlist commune, la même pour tout utilisateur authentifié, sans filtrage (FR-009-10).
 */
public class ConsulterWatchlistCasUsage implements ConsulterWatchlist {

    private final DepotWatchlist depot;

    public ConsulterWatchlistCasUsage(DepotWatchlist depot) {
        this.depot = depot;
    }

    @Override
    public List<ValeurWatchlist> consulter() {
        return depot.listerTrie();
    }
}
