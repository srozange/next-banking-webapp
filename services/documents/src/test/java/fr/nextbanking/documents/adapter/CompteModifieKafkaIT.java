package fr.nextbanking.documents.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.documents.infrastructure.ApplicationDocuments;
import fr.nextbanking.documents.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Consommation de {@code coeur-bancaire.compte-modifie.v1} sur une vraie base et un vrai Kafka : la projection n'est
 * mise à jour que par une {@code versionAgregat} strictement supérieure, un compte inconnu est inséré, et un message
 * livré deux fois n'a qu'un effet.
 */
@SpringBootTest(classes = ApplicationDocuments.class)
@DirtiesContext
class CompteModifieKafkaIT {

    private static final String TOPIC = "coeur-bancaire.compte-modifie.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @BeforeAll
    static void topic() throws ExecutionException, InterruptedException {
        try (Admin admin = Admin.create(Map.<String, Object>of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                Conteneurs.KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 3, (short) 1))).all().get();
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
        long compteId = 9000L + (System.nanoTime() % 1000);
        UUID premier = UUID.randomUUID();
        UUID egale = UUID.randomUUID();
        UUID ancienne = UUID.randomUUID();
        UUID superieure = UUID.randomUUID();

        // Même clé : même partition, donc traités dans l'ordre d'envoi. Le compte est inconnu : il est inséré.
        envoyer(Long.toString(compteId),
                message(premier, compteId, "1900.00", 2),
                message(premier, compteId, "1900.00", 2),
                message(egale, compteId, "1.00", 2),
                message(ancienne, compteId, "2.00", 1));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(ancienne));

        assertThat(solde(compteId)).isEqualByComparingTo("1900.00");
        assertThat(version(compteId)).isEqualTo(2L);
        assertThat(jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", premier)
                .query(Long.class).single()).isEqualTo(1L);
        assertThat(jdbc.sql("select count(*) from comptes where compte_id = :id").param("id", compteId)
                .query(Long.class).single()).isEqualTo(1L);

        envoyer(Long.toString(compteId), message(superieure, compteId, "1800.50", 3));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(superieure));

        assertThat(solde(compteId)).isEqualByComparingTo("1800.50");
        assertThat(version(compteId)).isEqualTo(3L);
        assertThat(jdbc.sql("select numero || '|' || type || '|' || iban || '|' || bic || '|' || client_id"
                + " from comptes where compte_id = :id").param("id", compteId).query(String.class).single())
                .isEqualTo("FR76-0001-0001|COURANT|FR76-0001-0001|DEMOFR21XXX|1");
    }

    private boolean traite(UUID idEvenement) {
        return jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", idEvenement)
                .query(Long.class).single() == 1L;
    }

    private BigDecimal solde(long compteId) {
        return jdbc.sql("select solde from comptes where compte_id = :id").param("id", compteId)
                .query(BigDecimal.class).single();
    }

    private long version(long compteId) {
        return jdbc.sql("select version_agregat from comptes where compte_id = :id").param("id", compteId)
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

    private static String message(UUID idEvenement, long compteId, String solde, long version) {
        return """
                {"idEvenement":"%s","type":"compte-modifie","version":1,"horodatage":"2026-10-09T09:00:00Z",\
                "producteur":"coeur-bancaire","donnees":{"compteId":%d,"clientId":1,"numero":"FR76-0001-0001",\
                "type":"COURANT","iban":"FR76-0001-0001","bic":"DEMOFR21XXX","solde":"%s",\
                "versionAgregat":%d}}""".formatted(idEvenement, compteId, solde, version);
    }
}
