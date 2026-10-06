package fr.nextbanking.bourse.infrastructure;

import fr.nextbanking.bourse.application.AjouterValeurCasUsage;
import fr.nextbanking.bourse.application.ConsulterWatchlistCasUsage;
import fr.nextbanking.bourse.application.RetirerValeurCasUsage;
import fr.nextbanking.bourse.application.port.in.AjouterValeur;
import fr.nextbanking.bourse.application.port.in.ConsulterWatchlist;
import fr.nextbanking.bourse.application.port.in.RetirerValeur;
import fr.nextbanking.bourse.application.port.out.DepotWatchlist;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de la watchlist, écrits en Java pur.
 */
@Configuration
public class AssemblageWatchlist {

    @Bean
    ConsulterWatchlist consulterWatchlist(DepotWatchlist depot) {
        return new ConsulterWatchlistCasUsage(depot);
    }

    @Bean
    AjouterValeur ajouterValeur(DepotWatchlist depot) {
        return new AjouterValeurCasUsage(depot);
    }

    @Bean
    RetirerValeur retirerValeur(DepotWatchlist depot) {
        return new RetirerValeurCasUsage(depot);
    }
}
