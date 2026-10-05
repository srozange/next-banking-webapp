package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.EcrireTemoinCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.EcrireTemoin;
import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage d'écriture du témoin, écrit en Java pur.
 */
@Configuration
public class AssemblageTemoin {

    @Bean
    EcrireTemoin ecrireTemoin(DepotTemoins depotTemoins) {
        return new EcrireTemoinCasUsage(depotTemoins);
    }
}
