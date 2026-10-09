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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Consommation de {@code coeur-bancaire.operation-enregistree.v1} sur une vraie base et un vrai Kafka : une opération
 * est insérée une seule fois (clé d'idempotence {@code operationId}) et un message livré deux fois n'a qu'un effet.
 */
@SpringBootTest(classes = ApplicationDocuments.class)
@DirtiesContext
class OperationEnregistreeKafkaIT {

    private static final String TOPIC = "coeur-bancaire.operation-enregistree.v1";

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
    @Tag("FR-017-04")
    void une_operation_est_projetee_une_seule_fois_meme_livree_deux_fois_ou_sous_deux_evenements() throws Exception {
        long operationId = 9000L + (System.nanoTime() % 1000);
        UUID premier = UUID.randomUUID();
        UUID autreEvenement = UUID.randomUUID();
        UUID suivant = UUID.randomUUID();

        // Même clé : même partition, donc traités dans l'ordre d'envoi.
        envoyer("1",
                message(premier, operationId, "Virement", "DEBIT", "100.00", "1900.00", 2),
                message(premier, operationId, "Virement", "DEBIT", "100.00", "1900.00", 2),
                message(autreEvenement, operationId, "Autre libelle", "CREDIT", "1.00", "1.00", 3),
                message(suivant, operationId + 1, "Versement interet", "CREDIT", "12.35", "1912.35", 3));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(suivant));

        assertThat(jdbc.sql("select count(*) from operations where operation_id = :id").param("id", operationId)
                .query(Long.class).single()).isEqualTo(1L);
        assertThat(jdbc.sql("select compte_id || '|' || to_char(instant at time zone 'UTC', 'YYYY-MM-DD HH24:MI:SS') || '|' || libelle || '|' || type || '|' || montant"
                + " || '|' || solde_apres || '|' || version_agregat from operations where operation_id = :id")
                .param("id", operationId).query(String.class).single())
                .isEqualTo("1|2026-11-17 09:00:00|Virement|DEBIT|100.00|1900.00|2");
        assertThat(jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", premier)
                .query(Long.class).single()).isEqualTo(1L);
        assertThat(traite(autreEvenement)).isTrue();
        assertThat(jdbc.sql("select montant from operations where operation_id = :id").param("id", operationId + 1)
                .query(BigDecimal.class).single()).isEqualByComparingTo("12.35");
    }

    private boolean traite(UUID idEvenement) {
        return jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", idEvenement)
                .query(Long.class).single() == 1L;
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

    private static String message(UUID idEvenement, long operationId, String libelle, String type, String montant,
            String soldeApres, long version) {
        return """
                {"idEvenement":"%s","type":"operation-enregistree","version":1,"horodatage":"2026-11-17T09:00:00Z",\
                "producteur":"coeur-bancaire","donnees":{"operationId":%d,"compteId":1,\
                "instant":"2026-11-17T09:00:00Z","libelle":"%s","type":"%s","montant":"%s","soldeApres":"%s",\
                "versionAgregat":%d}}""".formatted(idEvenement, operationId, libelle, type, montant, soldeApres, version);
    }
}
