package fr.nextbanking.identiteclients.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Topics produits par identite-clients, et planification du relais de l'outbox d'audit.
 */
@Configuration
@EnableScheduling
public class ConfigurationEvenements {

    static final String TOPIC_AUDIT = "identite-clients.audit.v1";

    @Bean
    NewTopic topicAudit() {
        return TopicBuilder.name(TOPIC_AUDIT).partitions(3).replicas(1).build();
    }
}
