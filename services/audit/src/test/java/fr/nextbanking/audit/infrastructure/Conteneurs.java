package fr.nextbanking.audit.infrastructure;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base et Kafka de test (Testcontainers), démarrés une fois pour toute l'exécution, et paire RSA du jeton générée en
 * mémoire : aucune clé n'est écrite dans un fichier.
 */
public final class Conteneurs {

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("audit").withUsername("audit").withPassword("mot-de-passe-de-test");
    /** Plusieurs tentatives : sur un poste chargé (pile compose en marche), le premier démarrage de Kafka échoue parfois. */
    public static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.2").withStartupAttempts(3);
    public static final KeyPair CLES = paireRsa();

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    private Conteneurs() {
    }

    /** Clé publique X.509 DER en base64, comme {@code NB_JETON_CLE_PUBLIQUE}. */
    public static String clePublique() {
        return Base64.getEncoder().encodeToString(CLES.getPublic().getEncoded());
    }

    /** Propriétés de connexion (base, Kafka) et clé publique pour {@code @DynamicPropertySource}. */
    public static void proprietes(DynamicPropertyRegistry registre) {
        registre.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registre.add("spring.datasource.username", POSTGRES::getUsername);
        registre.add("spring.datasource.password", POSTGRES::getPassword);
        registre.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registre.add("nb.jeton.cle-publique", Conteneurs::clePublique);
        registre.add("management.tracing.export.enabled", () -> "false");
    }

    /** Mêmes propriétés, en arguments de ligne de commande (pour {@code main}). */
    public static List<String> arguments() {
        return List.of(
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                "--nb.jeton.cle-publique=" + clePublique(),
                "--management.tracing.export.enabled=false");
    }

    private static KeyPair paireRsa() {
        try {
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
            generateur.initialize(2048);
            return generateur.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
