package fr.nextbanking.credits.infrastructure;

import fr.nextbanking.credits.application.ConsulterPretCasUsage;
import fr.nextbanking.credits.application.ListerPretsCasUsage;
import fr.nextbanking.credits.application.SimulerEmpruntCasUsage;
import fr.nextbanking.credits.application.port.in.ConsulterPret;
import fr.nextbanking.credits.application.port.in.ListerPrets;
import fr.nextbanking.credits.application.port.in.SimulerEmprunt;
import fr.nextbanking.credits.application.port.out.CalculSimulation;
import fr.nextbanking.credits.application.port.out.PretRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de simulation et de consultation des prêts, écrits en Java pur.
 */
@Configuration
public class AssemblageCredits {

    @Bean
    SimulerEmprunt simulerEmprunt(CalculSimulation calcul) {
        return new SimulerEmpruntCasUsage(calcul);
    }

    @Bean
    ListerPrets listerPrets(PretRepository prets) {
        return new ListerPretsCasUsage(prets);
    }

    @Bean
    ConsulterPret consulterPret(PretRepository prets) {
        return new ConsulterPretCasUsage(prets);
    }
}
