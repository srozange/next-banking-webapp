package fr.nextbanking.notifications.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.notifications.infrastructure.ApplicationNotifications;
import fr.nextbanking.notifications.infrastructure.Conteneurs;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Réception de {@code coeur-bancaire.echeance-non-executee.v1} sur une vraie base et un vrai Kafka : message brut (chaîne
 * JSON sans en-tête de type) livré deux fois.
 */
@SpringBootTest(classes = ApplicationNotifications.class)
@DirtiesContext
@Tag("FR-004-12")
@Tag("FR-021-04")
class ReceptionEcheanceNonExecuteeTest {

    private static final String TOPIC = "coeur-bancaire.echeance-non-executee.v1";
    private static final String CONFIRMATION = "Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private JdbcClient jdbc;

    @BeforeAll
    static void creerLeTopic() throws Exception {
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                Conteneurs.KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 3, (short) 1))).all().get();
        } catch (java.util.concurrent.ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw e;
            }
        }
    }

    @Test
    void une_enveloppe_livree_deux_fois_ne_cree_qu_une_notification_stockee_avec_son_titre() throws Exception {
        long clientId = 1_000L + (System.nanoTime() % 1_000L);
        UUID id = UUID.randomUUID();
        UUID idFin = UUID.randomUUID();
        String solde = "Virement programmé vers Livret non exécuté : solde insuffisant, rien n'a été débité";

        // Même clé, donc même partition : quand le dernier message est traité, les précédents l'ont été.
        envoyer("3", enveloppe(id, 3, clientId, "Virement programmé non exécuté", solde),
                enveloppe(id, 3, clientId, "Virement programmé non exécuté", solde),
                enveloppe(idFin, 4, clientId, "Virement programmé refusé", "autre texte"));

        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        List<Map<String, Object>> lignes = jdbc.sql("""
                select virement_id, virement_programme_id, type, titre, message, lue from notifications
                where client_id = :c order by id desc""").param("c", clientId).query().listOfRows();
        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0)).containsEntry("virement_programme_id", 4L).containsEntry("virement_id", null)
                .containsEntry("titre", "Virement programmé refusé");
        assertThat(lignes.get(1)).containsEntry("virement_programme_id", 3L).containsEntry("virement_id", null)
                .containsEntry("type", "ALERTE").containsEntry("titre", "Virement programmé non exécuté")
                .containsEntry("message", solde).containsEntry("lue", false);
    }

    private boolean traite(UUID idEvenement) {
        return jdbc.sql("select count(*) from evenements_traites where id_evenement = :id")
                .param("id", idEvenement).query(Long.class).single() == 1L;
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

    private static String enveloppe(UUID idEvenement, long virementProgrammeId, long clientId, String titre,
            String message) {
        return """
                {"idEvenement":"%s","type":"echeance-non-executee","version":1,"horodatage":"2026-11-17T09:00:00Z",\
                "producteur":"coeur-bancaire","donnees":{"virementProgrammeId":%d,\
                "notification":{"clientId":%d,"type":"ALERTE","titre":"%s","message":"%s"}}}"""
                .formatted(idEvenement, virementProgrammeId, clientId, titre, message);
    }
}
