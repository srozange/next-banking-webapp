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
@Tag("FR-006-05")
@Tag("FR-006-06")
@Tag("FR-006-07")
@Tag("FR-006-08")
@Tag("FR-006-10")
@Tag("FR-006-11")
@Tag("BUG-023")
class BudgetIT {

    private static final String BUDGET = "/api/coeur-bancaire/budget";
    private static final String PLAFONDS = BUDGET + "/plafonds";

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
        plafond(2, "LOGEMENT", "2026-03");

        JsonNode budget = lire(1, "?periode=2026-03&clientId=2&compteId=3");

        assertThat(budget.get("totalDepenses").asString()).isEqualTo("10.00");
        assertThat(budget.get("totalRevenus").asString()).isEqualTo("0.00");
        assertThat(budget.get("plafondsDefinis").asInt()).isZero();
        assertThat(budget.get("repartition")).hasSize(1);
        // Le compte 3 appartient à user2 (client 2) dans l'import.
        assertThat(lire(2, "?periode=2026-03").get("totalDepenses").asString()).isEqualTo("999.00");
    }

    private HttpResponse<String> definir(Long clientId, String corps) throws Exception {
        return Jetons.envoyer(port, "PUT", PLAFONDS, clientId == null ? null : Jetons.pour(clientId, "CLIENT"), corps);
    }

    private static String corps(String categorie, String mois, String plafond) {
        return "{\"categorie\":" + (categorie == null ? "null" : "\"" + categorie + "\"") + ",\"mois\":\"" + mois
                + "\",\"plafond\":" + (plafond == null ? "null" : "\"" + plafond + "\"") + "}";
    }

    private long nombrePlafonds() {
        return jdbc.sql("select count(*) from plafonds_budget").query(Long.class).single();
    }

    @Test
    void sans_jeton_l_enregistrement_et_la_suppression_sont_refuses_en_401() throws Exception {
        assertThat(definir(null, corps("Logement", "2026-03", "500")).statusCode()).isEqualTo(401);
        assertThat(Jetons.envoyer(port, "DELETE", PLAFONDS + "/1", null, null).statusCode()).isEqualTo(401);
        assertThat(nombrePlafonds()).isZero();
    }

    @Test
    void un_plafond_se_cree_puis_se_remplace_et_reste_unique() throws Exception {
        operation(1, "DEBIT", "400.00", "Carrefour", "2026-03-01T08:00:00Z");

        HttpResponse<String> cree = definir(1L, corps("Alimentation", "2026-03", "500"));
        HttpResponse<String> remplace = definir(1L, corps("Alimentation", "2026-03", "300.456"));

        assertThat(cree.statusCode()).isEqualTo(200);
        assertThat(json.readTree(cree.body()).get("plafond").asString()).isEqualTo("500.00");
        assertThat(remplace.statusCode()).isEqualTo(200);
        JsonNode corps = json.readTree(remplace.body());
        assertThat(corps.get("id").asLong()).isEqualTo(json.readTree(cree.body()).get("id").asLong());
        assertThat(corps.get("categorie").asString()).isEqualTo("Alimentation");
        assertThat(corps.get("mois").asString()).isEqualTo("2026-03");
        assertThat(corps.get("plafond").asString()).isEqualTo("300.46");
        assertThat(nombrePlafonds()).isEqualTo(1);
        JsonNode budget = lire(1, "?periode=2026-03");
        assertThat(budget.get("plafondsDefinis").asInt()).isEqualTo(1);
        assertThat(budget.get("plafonds")).hasSize(1);
        JsonNode ligne = budget.get("plafonds").get(0);
        assertThat(ligne.get("id").asLong()).isEqualTo(corps.get("id").asLong());
        assertThat(ligne.get("categorie").asString()).isEqualTo("Alimentation");
        assertThat(ligne.get("plafond").asString()).isEqualTo("300.46");
        assertThat(ligne.get("depense").asString()).isEqualTo("400.00");
    }

    @Test
    void un_plafond_sans_depense_a_une_depense_a_zero_et_les_plafonds_suivent_l_ordre_des_categories()
            throws Exception {
        assertThat(definir(1L, corps("Autres", "2026-03", "10")).statusCode()).isEqualTo(200);
        assertThat(definir(1L, corps("Logement", "2026-03", "20")).statusCode()).isEqualTo(200);

        JsonNode plafonds = lire(1, "?periode=2026-03").get("plafonds");

        assertThat(plafonds).extracting(l -> l.get("categorie").asString() + "=" + l.get("depense").asString())
                .containsExactly("Logement=0.00", "Autres=0.00");
    }

    @Test
    void la_categorie_revenus_inconnue_ou_absente_est_refusee_en_400_sans_creer_de_plafond() throws Exception {
        for (String categorie : new String[] {"Revenus", "Voitures", null}) {
            HttpResponse<String> reponse = definir(1L, corps(categorie, "2026-03", "500"));
            assertThat(reponse.statusCode()).isEqualTo(400);
            assertThat(json.readTree(reponse.body()).get("message").asString())
                    .isEqualTo("Veuillez selectionner une categorie");
        }
        assertThat(nombrePlafonds()).isZero();
    }

    @Test
    void un_plafond_invalide_est_refuse_en_400() throws Exception {
        for (String plafond : new String[] {null, "", "0", "-10", "abc"}) {
            HttpResponse<String> reponse = definir(1L, corps("Logement", "2026-03", plafond));
            assertThat(reponse.statusCode()).isEqualTo(400);
            assertThat(json.readTree(reponse.body()).get("message").asString())
                    .isEqualTo("Veuillez saisir un plafond valide");
        }
        assertThat(nombrePlafonds()).isZero();
    }

    @Test
    void un_plafond_de_quatorze_chiffres_entiers_est_refuse_en_400_et_treize_chiffres_sont_acceptes() throws Exception {
        HttpResponse<String> trop = definir(1L, corps("Logement", "2026-03", "12345678901234"));

        assertThat(trop.statusCode()).isEqualTo(400);
        assertThat(json.readTree(trop.body()).get("message").asString())
                .isEqualTo("Le plafond ne doit pas dépasser 13 chiffres avant la virgule");
        assertThat(nombrePlafonds()).isZero();
        assertThat(definir(1L, corps("Logement", "2026-03", "9999999999999.99")).statusCode()).isEqualTo(200);
    }

    @Test
    void un_mois_illisible_ou_un_corps_illisible_est_refuse_en_400() throws Exception {
        assertThat(definir(1L, corps("Logement", "mars", "500")).statusCode()).isEqualTo(400);
        assertThat(definir(1L, "pas du json").statusCode()).isEqualTo(400);
        assertThat(definir(1L, null).statusCode()).isEqualTo(400);
        assertThat(nombrePlafonds()).isZero();
    }

    @Test
    void la_suppression_est_immediate_et_un_plafond_d_autrui_ou_inexistant_donne_le_meme_404() throws Exception {
        long id = json.readTree(definir(1L, corps("Logement", "2026-03", "500")).body()).get("id").asLong();

        HttpResponse<String> autrui = Jetons.envoyer(port, "DELETE", PLAFONDS + "/" + id, Jetons.pour(8, "CLIENT"),
                null);
        HttpResponse<String> inexistant = Jetons.envoyer(port, "DELETE", PLAFONDS + "/999999",
                Jetons.pour(1, "CLIENT"), null);

        assertThat(autrui.statusCode()).isEqualTo(404);
        assertThat(inexistant.statusCode()).isEqualTo(404);
        assertThat(autrui.body()).isEqualTo(inexistant.body());
        assertThat(json.readTree(autrui.body()).get("message").asString()).isNotBlank();
        assertThat(nombrePlafonds()).isEqualTo(1);
        assertThat(Jetons.envoyer(port, "DELETE", PLAFONDS + "/" + id, Jetons.pour(1, "CLIENT"), null).statusCode())
                .isEqualTo(204);
        assertThat(nombrePlafonds()).isZero();
        assertThat(Jetons.envoyer(port, "DELETE", PLAFONDS + "/abc", Jetons.pour(1, "CLIENT"), null).statusCode())
                .isEqualTo(400);
    }

    @Test
    void les_plafonds_de_deux_clients_sont_independants() throws Exception {
        definir(1L, corps("Logement", "2026-03", "500"));
        definir(8L, corps("Logement", "2026-03", "700"));

        assertThat(nombrePlafonds()).isEqualTo(2);
        assertThat(lire(1, "?periode=2026-03").get("plafonds").get(0).get("plafond").asString()).isEqualTo("500.00");
        assertThat(lire(8, "?periode=2026-03").get("plafonds").get(0).get("plafond").asString()).isEqualTo("700.00");
    }
}
