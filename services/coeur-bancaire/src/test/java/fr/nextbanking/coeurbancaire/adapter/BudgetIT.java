package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Budget de bout en bout sur une vraie base : authentification, périodes, ventilation, totaux, plafonds, isolation des
 * clients (le client vient du jeton, tout autre paramètre est ignoré).
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.kafka.listener.auto-startup=false")
@DirtiesContext
@Import(BudgetIT.ConfigurationHorloge.class)
@Tag("FR-006-01")
@Tag("FR-006-02")
@Tag("FR-006-03")
@Tag("FR-006-04")
class BudgetIT {

    private static final String BUDGET = "/api/coeur-bancaire/budget";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @TestConfiguration
    static class ConfigurationHorloge {
        @Bean
        @Primary
        HorlogeFixe horlogeFixe() {
            return new HorlogeFixe();
        }
    }

    @LocalServerPort
    private int port;
    @Autowired
    private HorlogeFixe horloge;
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
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS);
    }

    private void operation(long compteId, String type, String montant, String description, String date) {
        jdbc.sql("""
                insert into operations (compte_id, type, montant, solde_apres, description, date_operation)
                values (:c, :t, :m, 1.00, :d, :date)""")
                .param("c", compteId).param("t", type).param("m", new java.math.BigDecimal(montant))
                .param("d", description).param("date", java.sql.Timestamp.from(Instant.parse(date))).update();
    }

    private void plafond(long clientId, String categorie, String mois) {
        jdbc.sql("insert into plafonds_budget (client_id, categorie, mois, plafond) values (:c, :k, :m, 500.00)")
                .param("c", clientId).param("k", categorie).param("m", mois).update();
    }

    private JsonNode lire(long clientId, String requete) throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", BUDGET + requete, Jetons.pour(clientId, "CLIENT"),
                null);
        assertThat(reponse.statusCode()).isEqualTo(200);
        return json.readTree(reponse.body());
    }

    @Test
    void sans_jeton_la_reponse_est_401() throws Exception {
        assertThat(Jetons.envoyer(port, "GET", BUDGET, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void sans_operation_la_periode_est_le_mois_de_la_date_du_jour_du_service() throws Exception {
        horloge.regler(Instant.parse("2026-11-17T09:00:00Z"));

        JsonNode budget = lire(10, "");

        assertThat(budget.get("aujourdhui").asString()).isEqualTo("2026-11-17");
        assertThat(budget.get("periode").asString()).isEqualTo("2026-11");
        assertThat(budget.get("periodes")).isEmpty();
        assertThat(budget.get("totalDepenses").asString()).isEqualTo("0.00");
        assertThat(budget.get("totalRevenus").asString()).isEqualTo("0.00");
        assertThat(budget.get("plafondsDefinis").asInt()).isZero();
        assertThat(budget.get("repartition")).isEmpty();
    }

    @Test
    void ventilation_totaux_plafonds_et_periodes_du_client_du_jeton() throws Exception {
        operation(1, "DEBIT", "1000.00", "LOYER mars", "2026-03-01T08:00:00Z");
        operation(2, "DEBIT", "200.00", "Carrefour", "2026-03-02T08:00:00Z");
        operation(1, "DEBIT", "34.50", "xyz", "2026-03-03T08:00:00Z");
        operation(1, "CREDIT", "2000.00", "virement", "2026-03-04T08:00:00Z");
        operation(1, "DEBIT", "1.00", "loyer", "2026-02-04T08:00:00Z");
        plafond(1, "ALIMENTATION", "2026-03");
        plafond(1, "LOGEMENT", "2026-03");

        JsonNode budget = lire(1, "?periode=2026-03");

        assertThat(budget.get("periode").asString()).isEqualTo("2026-03");
        assertThat(budget.get("periodes")).extracting(JsonNode::asString).containsExactly("2026-03", "2026-02");
        assertThat(budget.get("totalDepenses").asString()).isEqualTo("1234.50");
        assertThat(budget.get("totalRevenus").asString()).isEqualTo("2000.00");
        assertThat(budget.get("plafondsDefinis").asInt()).isEqualTo(2);
        assertThat(budget.get("repartition")).extracting(l -> l.get("categorie").asString() + "=" + l.get("montant").asString())
                .containsExactly("Logement=1000.00", "Alimentation=200.00", "Autres=34.50");
        assertThat(lire(1, "").get("periode").asString()).isEqualTo("2026-03");
        assertThat(lire(1, "?periode=n-importe-quoi").get("periode").asString()).isEqualTo("2026-03");
    }

    @Test
    void rien_d_un_autre_client_n_est_compte_meme_avec_un_identifiant_en_parametre() throws Exception {
        operation(1, "DEBIT", "10.00", "loyer", "2026-03-01T08:00:00Z");
        operation(3, "DEBIT", "999.00", "loyer", "2026-03-01T08:00:00Z");
        operation(3, "CREDIT", "500.00", "salaire", "2026-03-01T08:00:00Z");
        plafond(8, "LOGEMENT", "2026-03");

        JsonNode budget = lire(1, "?periode=2026-03&clientId=8&compteId=3");

        assertThat(budget.get("totalDepenses").asString()).isEqualTo("10.00");
        assertThat(budget.get("totalRevenus").asString()).isEqualTo("0.00");
        assertThat(budget.get("plafondsDefinis").asInt()).isZero();
        assertThat(budget.get("repartition")).hasSize(1);
        assertThat(lire(8, "?periode=2026-03").get("totalDepenses").asString()).isEqualTo("999.00");
    }
}
