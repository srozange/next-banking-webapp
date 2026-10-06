package fr.nextbanking.openbankingconseiller.infrastructure;

import fr.nextbanking.openbankingconseiller.application.ConsulterCatalogueCasUsage;
import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterCatalogue;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotProduits;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage du catalogue, écrit en Java pur.
 */
@Configuration
public class AssemblageCatalogue {

    @Bean
    ConsulterCatalogue consulterCatalogue(DepotProduits depot) {
        return new ConsulterCatalogueCasUsage(depot);
    }
}
