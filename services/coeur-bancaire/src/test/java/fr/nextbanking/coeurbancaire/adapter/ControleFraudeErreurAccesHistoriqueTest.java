package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
 * Accès à l'historique du contrôle de fraude qui dépasse {@code nb.fraude.acces-historique.delai} (1 ms ici, verrou du
 * client tenu par une autre transaction) : le virement de 50 € puis de 50 000 € est persisté REFUSÉ_ERREUR_CONTRÔLE,
 * aucun solde ne change, ni opération, remise ou notification ; la réponse est {@code {"message", "reference"}}.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"nb.reseau.mandataires-de-confiance=127.0.0.0/8", "nb.fraude.acces-historique.delai=1ms"})
@DirtiesContext
@Tag("FR-003-27")
@Tag("FR-027-02")
@Tag("BUG-084")
class ControleFraudeErreurAccesHistoriqueTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource source;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
        jdbc.sql("update comptes set solde = 100000.00 where id = 1").update();
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    private HttpResponse<String> virement(String montant) throws Exception {
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                + "/api/coeur-bancaire/virements"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + Jetons.pour(1, "CLIENT"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"compteSourceId\":1,\"compteDestinataireId\":2,"
                        + "\"montant\":\"" + montant + "\"}"))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(requete, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void un_virement_de_50_puis_de_50000_euros_est_refuse_sans_effet_avec_message_et_reference() throws Exception {
        try (Connection verrou = source.getConnection()) {
            verrou.setAutoCommit(false);
            verrou.createStatement().execute("select pg_advisory_xact_lock(1)");

            HttpResponse<String> petit = virement("50.00");
            HttpResponse<String> gros = virement("50000.00");
            verrou.rollback();

            for (HttpResponse<String> reponse : java.util.List.of(petit, gros)) {
                assertThat(reponse.statusCode()).isEqualTo(500);
                JsonNode corps = json.readTree(reponse.body());
                assertThat(corps.propertyNames()).containsExactlyInAnyOrder("message", "reference");
                String reference = corps.get("reference").asString();
                assertThat(reference).matches("[0-9a-f]{32}");
                assertThat(corps.get("message").asString()).endsWith("avec la référence " + reference + ".")
                        .startsWith("Votre virement n'a pas pu être vérifié. Rien n'a été débité.")
                        .doesNotContainIgnoringCase("fraude");
            }
        }

        assertThat(jdbc.sql("select etat from virements order by id").query(String.class).list())
                .containsExactly("REFUSE_ERREUR_CONTROLE", "REFUSE_ERREUR_CONTROLE");
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(String.class).single())
                .isEqualTo("100000.00");
        assertThat(jdbc.sql("select solde from comptes where id = 2").query(String.class).single())
                .isEqualTo("15000.00");
        assertThat(count("operations")).isZero();
        assertThat(count("outbox")).isZero();
        assertThat(count("alertes_fraude")).isZero();
        assertThat(jdbc.sql("select charge from outbox_audit order by id").query(String.class).list())
                .hasSize(2).allSatisfy(charge -> assertThat(charge).contains("Issue: REFUSE_ERREUR_CONTROLE"));
    }

    private long count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }
}
