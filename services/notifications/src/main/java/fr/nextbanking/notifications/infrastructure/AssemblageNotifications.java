package fr.nextbanking.notifications.infrastructure;

import fr.nextbanking.notifications.application.ConsulterNotificationCasUsage;
import fr.nextbanking.notifications.application.EffacerToutesLesNotificationsCasUsage;
import fr.nextbanking.notifications.application.ListerNotificationsCasUsage;
import fr.nextbanking.notifications.application.MarquerToutesLuesCasUsage;
import fr.nextbanking.notifications.application.port.in.ConsulterNotification;
import fr.nextbanking.notifications.application.port.in.EffacerToutesLesNotifications;
import fr.nextbanking.notifications.application.port.in.ListerNotifications;
import fr.nextbanking.notifications.application.port.in.MarquerToutesLues;
import fr.nextbanking.notifications.application.port.out.Notifications;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de lecture et de gestion des notifications, écrits en Java pur.
 */
@Configuration
public class AssemblageNotifications {

    @Bean
    ListerNotifications listerNotifications(Notifications notifications) {
        return new ListerNotificationsCasUsage(notifications);
    }

    @Bean
    ConsulterNotification consulterNotification(Notifications notifications) {
        return new ConsulterNotificationCasUsage(notifications);
    }

    @Bean
    MarquerToutesLues marquerToutesLues(Notifications notifications) {
        return new MarquerToutesLuesCasUsage(notifications);
    }

    @Bean
    EffacerToutesLesNotifications effacerToutesLesNotifications(Notifications notifications) {
        return new EffacerToutesLesNotificationsCasUsage(notifications);
    }
}
