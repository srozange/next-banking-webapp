package fr.nextbanking.notifications.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.notifications.infrastructure.ApplicationNotifications;
import fr.nextbanking.notifications.infrastructure.Conteneurs;
import java.time.Duration;
import java.util.ArrayList;
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
 * Réception de {@code coeur-bancaire.virement-decide.v1} sur une vraie base et un vrai Kafka : message brut (chaîne
 * JSON sans en-tête de type) livré deux fois, plafond par utilisateur, conservation après redémarrage.
 */
@SpringBootTest(classes = ApplicationNotifications.class)
@DirtiesContext
@Tag("FR-002-09")
@Tag("FR-023-05")
@Tag("FR-023-06")
@Tag("FR-025-04")
@Tag("BUG-006")
@Tag("BUG-007")
class ReceptionVirementDecideTest {

    private static final String TOPIC = "coeur-bancaire.virement-decide.v1";
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
    void un_evenement_livre_deux_fois_ne_cree_qu_une_notification_stockee_telle_que_rendue_non_lue() throws Exception {
        long clientId = 1_000L + (System.nanoTime() % 1_000L);
        UUID id = UUID.randomUUID();
        UUID idFin = UUID.randomUUID();

        // Même clé, donc même partition : quand le dernier message est traité, les précédents l'ont été.
        envoyer("12", enveloppe(id, 12, clientId, "ALERTE", "Texte rendu par le producteur"),
                enveloppe(id, 12, clientId, "ALERTE", "Texte rendu par le producteur"),
                enveloppe(idFin, 13, clientId, "CONFIRMATION", CONFIRMATION));

        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        List<Map<String, Object>> lignes = jdbc.sql("""
                select virement_id, type, titre, message, lue from notifications where client_id = :c order by id desc""")
                .param("c", clientId).query().listOfRows();
        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0)).containsEntry("virement_id", 13L).containsEntry("type", "CONFIRMATION")
                .containsEntry("message", CONFIRMATION).containsEntry("lue", false);
        assertThat(lignes.get(1)).containsEntry("virement_id", 12L).containsEntry("type", "ALERTE")
                .containsEntry("titre", null).containsEntry("message", "Texte rendu par le producteur")
                .containsEntry("lue", false);
    }

    @Test
    void la_21e_notification_supprime_la_plus_ancienne_et_se_place_en_tete() throws Exception {
        long clientId = 2_000L + (System.nanoTime() % 1_000L);
        List<String> messages = new ArrayList<>();
        UUID dernier = null;
        for (int i = 1; i <= 21; i++) {
            dernier = UUID.randomUUID();
            messages.add(enveloppe(dernier, 100 + i, clientId, "CONFIRMATION", "message " + i));
        }

        envoyer("plafond", messages.toArray(String[]::new));

        UUID idFin = dernier;
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        assertThat(messagesDe(clientId)).hasSize(20).first().isEqualTo("message 21");
        assertThat(messagesDe(clientId).getLast()).isEqualTo("message 2");
        assertThat(messagesDe(clientId)).doesNotContain("message 1");
    }

    @Test
    void apres_un_redemarrage_sur_la_meme_base_les_notifications_leur_ordre_et_leur_etat_de_lecture_sont_identiques()
            throws Exception {
        long clientId = 3_000L + (System.nanoTime() % 1_000L);
        UUID idFin = UUID.randomUUID();
        envoyer("redemarrage", enveloppe(UUID.randomUUID(), 301, clientId, "CONFIRMATION", "premiere"),
                enveloppe(idFin, 302, clientId, "ALERTE", "seconde"));
        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        jdbc.sql("update notifications set lue = true where client_id = :c and message = 'premiere'")
                .param("c", clientId).update();
        List<Map<String, Object>> avant = lignesDe(clientId);

        List<String> arguments = new ArrayList<>(Conteneurs.arguments());
        arguments.add("--spring.kafka.listener.auto-startup=false");
        arguments.add("--server.port=0");
        try (ConfigurableApplicationContext redemarre = new SpringApplicationBuilder(ApplicationNotifications.class)
                .web(WebApplicationType.SERVLET).run(arguments.toArray(String[]::new))) {
            List<Map<String, Object>> apres = redemarre.getBean(JdbcClient.class).sql(LECTURE)
                    .param("c", clientId).query().listOfRows();

            assertThat(apres).isEqualTo(avant);
        }
        assertThat(avant).extracting(l -> l.get("message"), l -> l.get("lue"))
                .containsExactly(org.assertj.core.groups.Tuple.tuple("seconde", false),
                        org.assertj.core.groups.Tuple.tuple("premiere", true));
    }

    private static final String LECTURE = """
            select message, type, titre, lue, virement_id from notifications where client_id = :c order by id desc""";

    private List<Map<String, Object>> lignesDe(long clientId) {
        return jdbc.sql(LECTURE).param("c", clientId).query().listOfRows();
    }

    private List<String> messagesDe(long clientId) {
        return jdbc.sql("select message from notifications where client_id = :c order by id desc")
                .param("c", clientId).query(String.class).list();
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

    private static String enveloppe(UUID idEvenement, long virementId, long clientId, String type, String message) {
        return """
                {"idEvenement":"%s","type":"virement-decide","version":1,"horodatage":"2026-11-17T09:00:00Z",\
                "producteur":"coeur-bancaire","donnees":{"virementId":%d,"clientId":%d,"etat":"EXECUTE",\
                "notification":{"clientId":%d,"type":"%s","titre":null,"message":"%s"}}}"""
                .formatted(idEvenement, virementId, clientId, clientId, type, message);
    }
}
