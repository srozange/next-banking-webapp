package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.EcrireTemoinCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.EcrireTemoin;
import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage d'écriture du témoin, écrit en Java pur.
 */
@Configuration
public class AssemblageTemoin {

    @Bean
    EcrireTemoin ecrireTemoin(DepotTemoins depotTemoins, EvenementsSortants evenementsSortants,
            Transaction transaction, Horloge horloge) {
        return new EcrireTemoinCasUsage(depotTemoins, evenementsSortants, transaction, horloge);
    }
}
