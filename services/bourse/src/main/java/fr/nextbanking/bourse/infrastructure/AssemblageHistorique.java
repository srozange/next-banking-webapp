package fr.nextbanking.bourse.infrastructure;

import fr.nextbanking.bourse.application.ConsulterHistoriqueCasUsage;
import fr.nextbanking.bourse.application.port.in.ConsulterHistorique;
import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.application.port.out.Horloge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage de l'historique des cours, écrit en Java pur ; l'adaptateur {@link Cotations} est choisi
 * par la configuration du fournisseur.
 */
@Configuration
public class AssemblageHistorique {

    @Bean
    ConsulterHistorique consulterHistorique(Cotations cotations, Horloge horloge) {
        return new ConsulterHistoriqueCasUsage(cotations, horloge);
    }
}
