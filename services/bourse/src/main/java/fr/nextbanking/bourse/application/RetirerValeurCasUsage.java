package fr.nextbanking.bourse.application;

import fr.nextbanking.bourse.application.port.in.RetirerValeur;
import fr.nextbanking.bourse.application.port.out.DepotWatchlist;
import fr.nextbanking.bourse.domain.ValeurIntrouvable;

/**
 * Retire une valeur de la watchlist commune, immédiatement (FR-009-13).
 */
public class RetirerValeurCasUsage implements RetirerValeur {

    private final DepotWatchlist depot;

    public RetirerValeurCasUsage(DepotWatchlist depot) {
        this.depot = depot;
    }

    @Override
    public void retirer(long id) {
        if (!depot.supprimer(id)) {
            throw new ValeurIntrouvable(id);
        }
    }
}
