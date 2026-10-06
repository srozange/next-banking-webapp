package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.ConsulterTraitementsCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterTraitements;
import fr.nextbanking.coeurbancaire.application.port.out.DepotTraitements;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage de lecture du journal des traitements, écrit en Java pur.
 */
@Configuration
public class AssemblageTraitements {

    @Bean
    ConsulterTraitements consulterTraitements(DepotTraitements depot) {
        return new ConsulterTraitementsCasUsage(depot);
    }
}
