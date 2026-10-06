package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
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
 * Consommateur interne de {@code coeur-bancaire.virement-a-traiter.v1} : un message brut (chaîne JSON de l'enveloppe,
 * sans en-tête de type) livré deux fois n'a qu'un effet (table {@code evenements_traites}), ne débite pas un virement
 * déjà exécuté et n'émet rien (ni outbox, ni audit).
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-023-01")
@Tag("FR-003-13")
class ReceptionVirementATraiterIT {

    private static final String TOPIC = "coeur-bancaire.virement-a-traiter.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource source;

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    @Test
    void un_message_livre_deux_fois_n_a_qu_un_effet_et_ne_debite_pas_un_virement_deja_execute() throws Exception {
        jdbc.sql("""
                insert into virements (id, client_id, compte_source_id, compte_destinataire_id, montant, motif, etat,
                                       statut, soumis_le)
                values (12, 1, 1, 2, 100.00, 'loyer', 'EXECUTE', 'SUCCESS', timestamptz '2026-11-17 09:00:00+00')""")
                .update();
        jdbc.sql("update comptes set solde = 2030.00 where id = 1").update();
        UUID idEvenement = UUID.randomUUID();
        UUID idFin = UUID.randomUUID();

        // Même clé, donc même partition : quand le dernier message est traité, les deux autres l'ont été.
        envoyer("12", enveloppe(idEvenement, 12), enveloppe(idEvenement, 12), enveloppe(idFin, 12));

        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        assertThat(traite(idEvenement)).isTrue();
        assertThat(jdbc.sql("select count(*) from evenements_traites").query(Long.class).single()).isEqualTo(2L);
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(BigDecimal.class).single())
                .isEqualByComparingTo("2030.00");
        assertThat(jdbc.sql("select solde from comptes where id = 2").query(BigDecimal.class).single())
                .isEqualByComparingTo("15200.00");
        assertThat(jdbc.sql("select count(*) from outbox").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from outbox_audit").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isZero();
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

    private static String enveloppe(UUID idEvenement, long virementId) {
        return """
                {"idEvenement":"%s","type":"virement-a-traiter","version":1,"horodatage":"2026-11-17T09:00:00Z",\
                "producteur":"coeur-bancaire","donnees":{"virementId":%d,"montant":"100.00","emetteurId":1,\
                "compteDestinataireId":2,"motif":"loyer"}}""".formatted(idEvenement, virementId);
    }
}
