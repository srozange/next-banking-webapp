package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.RevoirAlertesFraudeCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.RevoirAlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage de revue des alertes de fraude par l'administrateur, écrit en Java pur.
 */
@Configuration
public class AssemblageAlertesFraude {

    @Bean
    RevoirAlertesFraude revoirAlertesFraude(AlertesFraude alertes, Horloge horloge) {
        return new RevoirAlertesFraudeCasUsage(alertes, horloge);
    }
}
