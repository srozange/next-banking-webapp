package fr.nextbanking.notifications.infrastructure;

import fr.nextbanking.notifications.application.RecevoirEcheanceNonExecuteeCasUsage;
import fr.nextbanking.notifications.application.RecevoirVirementDecideCasUsage;
import fr.nextbanking.notifications.application.port.in.RecevoirEcheanceNonExecutee;
import fr.nextbanking.notifications.application.port.in.RecevoirVirementDecide;
import fr.nextbanking.notifications.application.port.out.EvenementsTraites;
import fr.nextbanking.notifications.application.port.out.Horloge;
import fr.nextbanking.notifications.application.port.out.Notifications;
import fr.nextbanking.notifications.application.port.out.Transaction;
import fr.nextbanking.notifications.domain.PlafondNotifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de réception de la décision d'un virement et d'une échéance non exécutée, écrit en Java pur, et valide au démarrage le
 * plafond {@code nb.notifications.plafond} : zéro ou négatif empêche le service de démarrer.
 */
@Configuration
public class AssemblageReceptionVirementDecide {

    @Bean
    PlafondNotifications plafondNotifications(@Value("${nb.notifications.plafond}") int plafond) {
        return new PlafondNotifications(plafond);
    }

    @Bean
    RecevoirVirementDecide recevoirVirementDecide(Notifications notifications, EvenementsTraites evenementsTraites,
            Transaction transaction, Horloge horloge, PlafondNotifications plafond) {
        return new RecevoirVirementDecideCasUsage(notifications, evenementsTraites, transaction, horloge, plafond);
    }

    @Bean
    RecevoirEcheanceNonExecutee recevoirEcheanceNonExecutee(Notifications notifications,
            EvenementsTraites evenementsTraites, Transaction transaction, Horloge horloge,
            PlafondNotifications plafond) {
        return new RecevoirEcheanceNonExecuteeCasUsage(notifications, evenementsTraites, transaction, horloge,
                plafond);
    }
}
