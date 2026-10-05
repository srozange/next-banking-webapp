package fr.nextbanking.coeurbancaire.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Topics produits par le cœur bancaire, et planification du relais de l'outbox.
 */
@Configuration
@EnableScheduling
public class ConfigurationEvenements {

    static final String TOPIC_TEMOIN_ECRIT = "coeur-bancaire.temoin-ecrit.v1";

    @Bean
    NewTopic topicTemoinEcrit() {
        return TopicBuilder.name(TOPIC_TEMOIN_ECRIT).partitions(3).replicas(1).build();
    }
}
