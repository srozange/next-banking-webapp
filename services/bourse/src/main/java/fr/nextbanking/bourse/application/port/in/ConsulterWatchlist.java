package fr.nextbanking.bourse.application.port.in;

import fr.nextbanking.bourse.domain.ValeurWatchlist;
import java.util.List;

/**
 * Port entrant : la watchlist commune, triée par marché puis par symbole (FR-009-10, FR-029-11).
 */
public interface ConsulterWatchlist {

    List<ValeurWatchlist> consulter();
}
