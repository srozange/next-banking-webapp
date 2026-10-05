package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Flux témoin de bout en bout sur une vraie base et un vrai Kafka : HTTP, table, outbox, publication, relais.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class FluxTemoinTest {

    private static final String TOPIC = "coeur-bancaire.temoin-ecrit.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private Transaction transaction;

    @Autowired
    private DepotTemoins depot;

    @Autowired
    private EvenementsSortants evenements;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void ecrit_le_temoin_et_publie_l_enveloppe_sur_kafka() throws Exception {
        HttpResponse<String> reponse = poster("{\"libelle\":\"x\"}");

        assertThat(reponse.statusCode()).isEqualTo(201);
        long temoinId = json.readTree(reponse.body()).get("temoinId").asLong();
        assertThat(jdbc.sql("select libelle from temoins where id = :id").param("id", temoinId)
                .query(String.class).single()).isEqualTo("x");
        assertThat(jdbc.sql("select publie from outbox where cle = :cle").param("cle", Long.toString(temoinId))
                .query(Boolean.class).single()).isTrue();

        ConsumerRecord<String, String> message = LecteurTopic.chercher(
                Conteneurs.KAFKA.getBootstrapServers(), TOPIC, Long.toString(temoinId), Duration.ofSeconds(20))
                .orElseThrow();
        assertThat(message.headers().lastHeader("__TypeId__")).isNull();
        JsonNode enveloppe = json.readTree(message.value());
        assertThat(enveloppe.get("type").asString()).isEqualTo("temoin-ecrit");
        assertThat(enveloppe.get("version").asInt()).isEqualTo(1);
        assertThat(enveloppe.get("producteur").asString()).isEqualTo("coeur-bancaire");
        assertThat(enveloppe.get("donnees").get("temoinId").asLong()).isEqualTo(temoinId);
        assertThat(UUID.fromString(enveloppe.get("idEvenement").asString())).isNotNull();
        assertThat(Instant.parse(enveloppe.get("horodatage").asString())).isNotNull();
        assertThat(enveloppe.get("horodatage").asString()).endsWith("Z");
    }

    @Test
    void refuse_un_libelle_vide_ou_absent_sans_corps_ni_ecriture() throws Exception {
        long temoins = compter("temoins");
        long lignes = compter("outbox");

        for (String corps : new String[] {"{\"libelle\":\" \"}", "{}", "pas du json"}) {
            HttpResponse<String> reponse = poster(corps);
            assertThat(reponse.statusCode()).as(corps).isEqualTo(400);
            assertThat(reponse.body()).as(corps).isEmpty();
        }
        assertThat(compter("temoins")).isEqualTo(temoins);
        assertThat(compter("outbox")).isEqualTo(lignes);
    }

    @Test
    void une_transaction_annulee_n_ecrit_ni_temoin_ni_outbox() {
        String libelle = "annulé " + UUID.randomUUID();
        UUID idEvenement = UUID.randomUUID();

        assertThatThrownBy(() -> transaction.executer(() -> {
            long temoinId = depot.enregistrer(new Temoin(libelle));
            evenements.consigner(new TemoinEcrit(idEvenement, temoinId, Instant.parse("2026-10-05T08:30:00Z")));
            throw new IllegalStateException("annulation voulue");
        })).hasMessage("annulation voulue");

        assertThat(jdbc.sql("select count(*) from temoins where libelle = :l").param("l", libelle)
                .query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from outbox where id_evenement = :id").param("id", idEvenement)
                .query(Long.class).single()).isZero();
    }

    @Test
    void une_transaction_independante_est_validee_malgre_l_annulation_de_l_appelant() {
        String libelle = "indépendant " + UUID.randomUUID();

        assertThatThrownBy(() -> transaction.executer(() -> {
            transaction.executerIndependamment(() -> depot.enregistrer(new Temoin(libelle)));
            throw new IllegalStateException("annulation voulue");
        })).hasMessage("annulation voulue");

        assertThat(jdbc.sql("select count(*) from temoins where libelle = :l").param("l", libelle)
                .query(Long.class).single()).isEqualTo(1L);
    }

    @Test
    void le_relais_publie_une_ligne_restee_non_publiee() {
        long temoinId = transaction.executer(() -> {
            long id = depot.enregistrer(new Temoin("relais"));
            evenements.consigner(new TemoinEcrit(UUID.randomUUID(), id, Instant.parse("2026-10-05T08:30:00Z")));
            return id;
        });

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jdbc.sql("select publie from outbox where cle = :cle")
                        .param("cle", Long.toString(temoinId)).query(Boolean.class).single()).isTrue());
        assertThat(LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(), TOPIC, Long.toString(temoinId),
                Duration.ofSeconds(20))).isPresent();
    }

    private long compter(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    private HttpResponse<String> poster(String corps) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/coeur-bancaire/temoin"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corps))
                    .build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
