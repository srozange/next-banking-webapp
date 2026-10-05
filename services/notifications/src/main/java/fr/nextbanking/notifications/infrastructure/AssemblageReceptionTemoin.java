package fr.nextbanking.notifications.infrastructure;

import fr.nextbanking.notifications.application.RecevoirTemoinCasUsage;
import fr.nextbanking.notifications.application.port.in.RecevoirTemoin;
import fr.nextbanking.notifications.application.port.out.EvenementsTraites;
import fr.nextbanking.notifications.application.port.out.RegistreTemoinsRecus;
import fr.nextbanking.notifications.application.port.out.Transaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage de réception du témoin, écrit en Java pur.
 */
@Configuration
public class AssemblageReceptionTemoin {

    @Bean
    RecevoirTemoin recevoirTemoin(RegistreTemoinsRecus registreTemoinsRecus, EvenementsTraites evenementsTraites,
            Transaction transaction) {
        return new RecevoirTemoinCasUsage(registreTemoinsRecus, evenementsTraites, transaction);
    }
}
