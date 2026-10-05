package fr.nextbanking.notifications.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.notifications.application.port.out.EvenementsTraites;
import fr.nextbanking.notifications.application.port.out.RegistreTemoinsRecus;
import fr.nextbanking.notifications.application.port.out.Transaction;
import fr.nextbanking.notifications.domain.TemoinRecu;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Réception sur une vraie base et un vrai Kafka : message brut (chaîne JSON sans en-tête de type) livré deux fois,
 * un seul effet.
 */
@SpringBootTest(classes = ApplicationNotifications.class)
@DirtiesContext
class ReceptionTemoinTest {

    private static final String TOPIC = "coeur-bancaire.temoin-ecrit.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private Transaction transaction;

    @Autowired
    private RegistreTemoinsRecus registre;

    @Autowired
    private EvenementsTraites evenementsTraites;

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
    void un_message_livre_deux_fois_n_a_qu_un_effet() throws Exception {
        long temoinId = 900_000L + (System.nanoTime() % 100_000L);
        UUID idEvenement = UUID.randomUUID();
        UUID idTemoinDeFin = UUID.randomUUID();
        String cle = Long.toString(temoinId);

        try (KafkaProducer<String, String> producteur = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Conteneurs.KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            producteur.send(new ProducerRecord<>(TOPIC, cle, enveloppe(idEvenement, temoinId))).get();
            producteur.send(new ProducerRecord<>(TOPIC, cle, enveloppe(idEvenement, temoinId))).get();
            // Même clé, donc même partition : quand ce dernier message est traité, les deux autres l'ont été.
            producteur.send(new ProducerRecord<>(TOPIC, cle, enveloppe(idTemoinDeFin, temoinId))).get();
        }

        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idTemoinDeFin));
        assertThat(traite(idEvenement)).isTrue();
        // Un effet pour l'événement doublé, un pour le message de fin.
        assertThat(jdbc.sql("select count(*) from temoins_recus where temoin_id = :id").param("id", temoinId)
                .query(Long.class).single()).isEqualTo(2L);
    }

    @Test
    void une_transaction_annulee_ne_marque_pas_l_evenement() {
        long temoinId = 700_000L + (System.nanoTime() % 100_000L);
        UUID idEvenement = UUID.randomUUID();

        assertThatThrownBy(() -> transaction.executer(() -> {
            evenementsTraites.marquerTraite(idEvenement);
            registre.enregistrer(new TemoinRecu(temoinId));
            throw new IllegalStateException("annulation voulue");
        })).hasMessage("annulation voulue");

        assertThat(traite(idEvenement)).isFalse();
        assertThat(jdbc.sql("select count(*) from temoins_recus where temoin_id = :id").param("id", temoinId)
                .query(Long.class).single()).isZero();
    }

    @Test
    void une_transaction_independante_est_validee_malgre_l_annulation_de_l_appelant() {
        long temoinId = 800_000L + (System.nanoTime() % 100_000L);

        assertThatThrownBy(() -> transaction.executer(() -> {
            transaction.executerIndependamment(() -> {
                registre.enregistrer(new TemoinRecu(temoinId));
                return null;
            });
            throw new IllegalStateException("annulation voulue");
        })).hasMessage("annulation voulue");

        assertThat(jdbc.sql("select count(*) from temoins_recus where temoin_id = :id").param("id", temoinId)
                .query(Long.class).single()).isEqualTo(1L);
    }

    private boolean traite(UUID idEvenement) {
        return jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", idEvenement)
                .query(Long.class).single() == 1L;
    }

    private static String enveloppe(UUID idEvenement, long temoinId) {
        return """
                {"idEvenement":"%s","type":"temoin-ecrit","version":1,"horodatage":"2026-10-05T08:30:00Z",\
                "producteur":"coeur-bancaire","donnees":{"temoinId":%d}}""".formatted(idEvenement, temoinId);
    }
}
