package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Consommation de {@code identite-clients.client-modifie.v1} sur une vraie base et un vrai Kafka : une
 * {@code versionAgregat} strictement supérieure réaffecte le client (y compris vers aucun conseiller), une version
 * égale ou inférieure est ignorée, un message livré deux fois n'a qu'un effet.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-015-02")
@Tag("FR-016-19")
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
            admin.createTopics(List.of(new NewTopic(TOPIC, 3, (short) 1))).all().get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw e;
            }
        }
    }

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ProjectionClients projection;

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @Test
    void une_version_superieure_reaffecte_le_client_et_une_version_egale_ou_inferieure_est_ignoree()
            throws Exception {
        long clientId = 9000L + (System.nanoTime() % 1000);
        UUID premier = UUID.randomUUID();
        UUID egale = UUID.randomUUID();
        UUID ancienne = UUID.randomUUID();
        UUID reaffectation = UUID.randomUUID();
        UUID sansConseiller = UUID.randomUUID();

        // Même clé : même partition, donc traités dans l'ordre d'envoi.
        envoyer(Long.toString(clientId),
                message(premier, clientId, "11", 2),
                message(premier, clientId, "11", 2),
                message(egale, clientId, "12", 2),
                message(ancienne, clientId, "12", 1));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(ancienne));

        assertThat(conseiller(clientId)).isEqualTo(11L);
        assertThat(projection.clientsDuConseiller(11L)).contains(clientId);
        assertThat(projection.clientsDuConseiller(12L)).doesNotContain(clientId);

        envoyer(Long.toString(clientId), message(reaffectation, clientId, "12", 3));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(reaffectation));

        assertThat(projection.clientsDuConseiller(11L)).doesNotContain(clientId);
        assertThat(projection.clientsDuConseiller(12L)).contains(clientId);

        envoyer(Long.toString(clientId), message(sansConseiller, clientId, "null", 4));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(sansConseiller));

        assertThat(conseiller(clientId)).isNull();
        assertThat(projection.clientsDuConseiller(12L)).doesNotContain(clientId);
        assertThat(jdbc.sql("select version_agregat from clients where client_id = :id").param("id", clientId)
                .query(Long.class).single()).isEqualTo(4L);
    }

    private boolean traite(UUID idEvenement) {
        return jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", idEvenement)
                .query(Long.class).single() == 1L;
    }

    private Long conseiller(long clientId) {
        return jdbc.sql("select conseiller_id from clients where client_id = :id").param("id", clientId)
                .query((ligne, numero) -> ligne.getObject("conseiller_id", Long.class)).list().getFirst();
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

    private static String message(UUID idEvenement, long clientId, String conseillerId, long version) {
        return """
                {"idEvenement":"%s","type":"client-modifie","version":1,"horodatage":"2026-10-09T09:00:00Z",\
                "producteur":"identite-clients","donnees":{"clientId":%d,"login":"login%d","nomComplet":"Client %d",\
                "role":"CLIENT","actif":true,"conseillerId":%s,"dateCreation":"2026-10-06","supprime":false,\
                "versionAgregat":%d}}""".formatted(idEvenement, clientId, clientId, clientId, conseillerId, version);
    }
}
