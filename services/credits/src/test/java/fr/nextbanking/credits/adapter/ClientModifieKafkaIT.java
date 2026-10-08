package fr.nextbanking.credits.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.sql.DataSource;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Consommation de {@code identite-clients.client-modifie.v1} sur une vraie base et un vrai Kafka : la projection n'est
 * mise à jour que par une {@code versionAgregat} strictement supérieure, et un message livré deux fois n'a qu'un effet.
 */
@SpringBootTest(classes = ApplicationCredits.class)
@DirtiesContext
@Tag("FR-016-03")
class ClientModifieKafkaIT {

    private static final String TOPIC = "identite-clients.client-modifie.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @BeforeAll
    static void topic() throws ExecutionException, InterruptedException {
        try (Admin admin = Admin.create(Map.<String, Object>of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                Conteneurs.KAFKA.getBootstrapServers()))) {
            admin.createTopics(java.util.List.of(new NewTopic(TOPIC, 3, (short) 1))).all().get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw e;
            }
        }
    }

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    @Test
    void la_projection_n_est_mise_a_jour_que_par_une_version_strictement_superieure_et_une_double_livraison_n_a_qu_un_effet()
            throws Exception {
        long clientId = 9000L + (System.nanoTime() % 1000);
        UUID premier = UUID.randomUUID();
        UUID egale = UUID.randomUUID();
        UUID ancienne = UUID.randomUUID();
        UUID superieure = UUID.randomUUID();

        // Même clé : même partition, donc traités dans l'ordre d'envoi.
        envoyer(Long.toString(clientId),
                message(premier, clientId, "Nom un", 2),
                message(premier, clientId, "Nom un", 2),
                message(egale, clientId, "Nom égal", 2),
                message(ancienne, clientId, "Nom ancien", 1));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(ancienne));

        assertThat(nom(clientId)).isEqualTo("Nom un");
        assertThat(version(clientId)).isEqualTo(2L);
        assertThat(jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", premier)
                .query(Long.class).single()).isEqualTo(1L);

        envoyer(Long.toString(clientId), message(superieure, clientId, "Nom trois", 3));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(superieure));

        assertThat(nom(clientId)).isEqualTo("Nom trois");
        assertThat(version(clientId)).isEqualTo(3L);
        assertThat(jdbc.sql("select role from clients where client_id = :id").param("id", clientId)
                .query(String.class).single()).isEqualTo("CLIENT");
        assertThat(jdbc.sql("select conseiller_id from clients where client_id = :id").param("id", clientId)
                .query(Long.class).single()).isEqualTo(11L);
    }

    private boolean traite(UUID idEvenement) {
        return jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", idEvenement)
                .query(Long.class).single() == 1L;
    }

    private String nom(long clientId) {
        return jdbc.sql("select nom_complet from clients where client_id = :id").param("id", clientId)
                .query(String.class).single();
    }

    private long version(long clientId) {
        return jdbc.sql("select version_agregat from clients where client_id = :id").param("id", clientId)
                .query(Long.class).single();
    }

    private static void envoyer(String cle, String... messages) throws Exception {
        try (KafkaProducer<String, String> producteur = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Conteneurs.KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            for (String message : messages) {
                producteur.send(new ProducerRecord<>(TOPIC, cle, message)).get();
            }
        }
    }

    private static String message(UUID idEvenement, long clientId, String nomComplet, long version) {
        return """
                {"idEvenement":"%s","type":"client-modifie","version":1,"horodatage":"2026-10-09T09:00:00Z",\
                "producteur":"identite-clients","donnees":{"clientId":%d,"login":"login%d","nomComplet":"%s",\
                "role":"CLIENT","actif":true,"conseillerId":11,"dateCreation":"2026-10-06","supprime":false,\
                "versionAgregat":%d}}""".formatted(idEvenement, clientId, clientId, nomComplet, version);
    }
}
