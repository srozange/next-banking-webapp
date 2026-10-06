package fr.nextbanking.coeurbancaire.infrastructure;

import java.util.List;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base et Kafka de test (Testcontainers), démarrés une fois pour toute l'exécution.
 */
public final class Conteneurs {

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("coeur_bancaire").withUsername("coeur_bancaire").withPassword("mot-de-passe-de-test");
    /** Plusieurs tentatives : sur un poste chargé (pile compose en marche), le premier démarrage de Kafka échoue parfois. */
    public static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.2").withStartupAttempts(3);

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    private Conteneurs() {
    }

    /** Propriétés de connexion pour {@code @DynamicPropertySource}. */
    public static void proprietes(DynamicPropertyRegistry registre) {
        registre.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registre.add("spring.datasource.username", POSTGRES::getUsername);
        registre.add("spring.datasource.password", POSTGRES::getPassword);
        registre.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    /** Mêmes propriétés, en arguments de ligne de commande (pour {@code main}). */
    public static List<String> arguments() {
        return List.of(
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers());
    }
}
