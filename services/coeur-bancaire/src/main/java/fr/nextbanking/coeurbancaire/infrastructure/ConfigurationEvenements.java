package fr.nextbanking.coeurbancaire.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Topics produits par le cœur bancaire, et planification des relais de l'outbox et de l'outbox d'audit.
 */
@Configuration
@EnableScheduling
public class ConfigurationEvenements {

    static final String TOPIC_TEMOIN_ECRIT = "coeur-bancaire.temoin-ecrit.v1";

    static final String TOPIC_AUDIT = "coeur-bancaire.audit.v1";

    /** Topic interne : produit et consommé par le cœur bancaire (remplace la file JMS « virements »). */
    static final String TOPIC_VIREMENT_A_TRAITER = "coeur-bancaire.virement-a-traiter.v1";

    /** Décision d'un virement et notification rendue : consommé par le service notifications. */
    static final String TOPIC_VIREMENT_DECIDE = "coeur-bancaire.virement-decide.v1";

    /** Échéance de virement programmé non exécutée et notification rendue : consommé par notifications. */
    static final String TOPIC_ECHEANCE_NON_EXECUTEE = "coeur-bancaire.echeance-non-executee.v1";

    @Bean
    NewTopic topicTemoinEcrit() {
        return TopicBuilder.name(TOPIC_TEMOIN_ECRIT).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic topicAudit() {
        return TopicBuilder.name(TOPIC_AUDIT).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic topicVirementATraiter() {
        return TopicBuilder.name(TOPIC_VIREMENT_A_TRAITER).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic topicVirementDecide() {
        return TopicBuilder.name(TOPIC_VIREMENT_DECIDE).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic topicEcheanceNonExecutee() {
        return TopicBuilder.name(TOPIC_ECHEANCE_NON_EXECUTEE).partitions(3).replicas(1).build();
    }
}
