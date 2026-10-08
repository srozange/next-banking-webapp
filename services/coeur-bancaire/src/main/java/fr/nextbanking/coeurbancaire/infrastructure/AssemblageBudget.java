package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.ConsulterBudgetCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBudget;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage du budget, écrit en Java pur.
 */
@Configuration
public class AssemblageBudget {

    @Bean
    ConsulterBudget consulterBudget(Operations operations, Plafonds plafonds, Horloge horloge) {
        return new ConsulterBudgetCasUsage(operations, plafonds, horloge);
    }
}
