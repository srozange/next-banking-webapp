package fr.nextbanking.credits.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Topics produits par le service credits, et planification du relais de l'outbox.
 */
@Configuration
@EnableScheduling
public class ConfigurationEvenements {

    static final String TOPIC_PRET_DEMANDE = "credits.pret-demande.v1";

    static final String TOPIC_PRET_DECIDE = "credits.pret-decide.v1";

    static final String TOPIC_AUDIT = "credits.audit.v1";

    @Bean
    NewTopic topicAudit() {
        return TopicBuilder.name(TOPIC_AUDIT).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic topicPretDecide() {
        return TopicBuilder.name(TOPIC_PRET_DECIDE).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic topicPretDemande() {
        return TopicBuilder.name(TOPIC_PRET_DEMANDE).partitions(3).replicas(1).build();
    }
}
