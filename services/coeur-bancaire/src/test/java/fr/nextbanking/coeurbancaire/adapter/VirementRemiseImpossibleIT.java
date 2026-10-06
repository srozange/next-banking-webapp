package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Remise au traitement asynchrone impossible (écriture de la ligne {@code outbox} refusée par la base) : la validation
 * échoue, rien n'est écrit, le client voit le texte d'échec, et une entrée VIREMENT reste dans {@code outbox_audit}
 * (BUG-073, FR-023-02).
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-023-02")
@Tag("BUG-073")
class VirementRemiseImpossibleIT {

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

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    @Test
    void un_virement_valide_de_100_euros_avec_remise_impossible_n_ecrit_rien_et_audite_la_tentative() throws Exception {
        // Le refus d'écriture de l'outbox : la table est indisponible le temps du test.
        jdbc.sql("alter table outbox rename to outbox_indisponible").update();
        HttpResponse<String> reponse;
        try {
            reponse = poster("{\"compteSourceId\":1,\"compteDestinataireId\":2,\"montant\":\"100.00\","
                    + "\"motif\":\"loyer\"}");
        } finally {
            jdbc.sql("alter table outbox_indisponible rename to outbox").update();
        }

        assertThat(reponse.statusCode()).isEqualTo(500);
        assertThat(reponse.body()).isEqualTo(
                "{\"message\":\"Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard\"}");
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("2130.00");
        assertThat(jdbc.sql("select solde from comptes where id = 2").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("15200.00");
        assertThat(jdbc.sql("select count(*) from virements").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isZero();
        List<Map<String, Object>> audit = jdbc.sql("select cle, charge from outbox_audit order by id")
                .query().listOfRows();
        assertThat(audit).singleElement().satisfies(ligne -> {
            var donnees = json.readTree((String) ligne.get("charge")).get("donnees");
            assertThat(donnees.get("action").asString()).isEqualTo("VIREMENT");
            assertThat(donnees.get("detail").asString()).isEqualTo("Montant: 100.00 - Motif: loyer - Issue: ECHEC_REMISE");
            assertThat(donnees.get("utilisateur").asString()).isEqualTo("1");
        });
    }

    private HttpResponse<String> poster(String corps) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                            + "/api/coeur-bancaire/virements"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Jetons.pour(1, "CLIENT"))
                    .POST(HttpRequest.BodyPublishers.ofString(corps)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
