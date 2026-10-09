package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * API du catalogue, sur une base chargée du {@code reference-data.sql} avant chaque test.
 */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class ProduitsIT {

    private static final String CHEMIN = "/api/open-banking-conseiller/produits";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void chargerLesDonneesDeReference() throws Exception {
        jdbc.execute("truncate savings_products, credit_products, investment_products, bank_products"
                + " restart identity");
        jdbc.execute(Files.readString(Path.of("reference-data/reference-data.sql"), StandardCharsets.UTF_8));
    }

    @Test
    @Tag("FR-010-01")
    void les_trois_roles_recoivent_le_meme_catalogue() throws Exception {
        String client = appeler(CHEMIN, "CLIENT").body();

        assertThat(appeler(CHEMIN, "CONSEILLER").body()).isEqualTo(client);
        assertThat(appeler(CHEMIN, "ADMIN").body()).isEqualTo(client);
        assertThat(appeler(CHEMIN, "CLIENT").statusCode()).isEqualTo(200);
    }

    @Test
    @Tag("FR-011-02")
    void le_catalogue_a_trois_groupes_tries_par_nom() throws Exception {
        JsonNode corps = json.readTree(appeler(CHEMIN, "CLIENT").body());

        assertThat(noms(corps.get("epargne"))).containsExactly("CEL - Compte Epargne Logement", "Livret A",
                "Livret Demo Bank", "PEL - Plan Epargne Logement");
        assertThat(noms(corps.get("credit"))).containsExactly("Credit Auto Classique",
                "Credit Immobilier Classique", "Credit Personnel", "Rachat de Credits");
        assertThat(noms(corps.get("investissement"))).containsExactly("ETF Monde Diversifie", "Fonds Euro Garanti",
                "PEA Actions Europe", "Private Equity Croissance", "SCPI Rendement Plus");
    }

    @Test
    @Tag("FR-011-02")
    void les_valeurs_sont_transmises_en_decimal_exact_sans_mise_en_forme() throws Exception {
        JsonNode corps = json.readTree(appeler(CHEMIN, "CLIENT").body());

        JsonNode livretA = corps.get("epargne").get(1);
        assertThat(livretA.get("taux").asString()).isEqualTo("0.0300");
        assertThat(livretA.get("depotMin").asString()).isEqualTo("10.00");
        assertThat(livretA.get("capitalGaranti").asBoolean()).isTrue();
        assertThat(livretA.get("dureeMaxMois").isNull()).isTrue();
        assertThat(corps.get("epargne").get(0).get("depotMin").asString()).isEqualTo("300.00");
        assertThat(corps.get("epargne").get(3).get("dureeMaxMois").asInt()).isEqualTo(120);

        JsonNode immobilier = corps.get("credit").get(1);
        assertThat(immobilier.get("taux").asString()).isEqualTo("0.0365");
        assertThat(immobilier.get("ltvMax").asString()).isEqualTo("0.8000");
        assertThat(immobilier.get("assuranceRequise").asBoolean()).isTrue();
        assertThat(immobilier.get("dureeMaxMois").asInt()).isEqualTo(300);
        JsonNode auto = corps.get("credit").get(0);
        assertThat(auto.get("ltvMax").isNull()).isTrue();
        assertThat(auto.get("assuranceRequise").asBoolean()).isFalse();

        JsonNode etf = corps.get("investissement").get(0);
        assertThat(etf.get("secteur").asString()).isEqualTo("Actions mondiales");
        assertThat(etf.get("niveauRisque").asInt()).isEqualTo(3);
        assertThat(etf.get("rendementAttendu").asString()).isEqualTo("0.0720");
        assertThat(etf.get("id").asLong()).isEqualTo(10L);
        assertThat(etf.get("description").asString()).startsWith("Fonds indiciel mondial");
    }

    @Test
    @Tag("FR-011-03")
    void le_filtre_par_type_ne_remplit_que_son_groupe() throws Exception {
        JsonNode epargne = json.readTree(appeler(CHEMIN + "?type=EPARGNE", "CLIENT").body());
        assertThat(epargne.get("epargne")).hasSize(4);
        assertThat(epargne.get("credit")).isEmpty();
        assertThat(epargne.get("investissement")).isEmpty();

        JsonNode credit = json.readTree(appeler(CHEMIN + "?type=CREDIT", "CONSEILLER").body());
        assertThat(credit.get("epargne")).isEmpty();
        assertThat(credit.get("credit")).hasSize(4);
        assertThat(credit.get("investissement")).isEmpty();

        JsonNode investissement = json.readTree(appeler(CHEMIN + "?type=INVESTISSEMENT", "ADMIN").body());
        assertThat(investissement.get("epargne")).isEmpty();
        assertThat(investissement.get("credit")).isEmpty();
        assertThat(investissement.get("investissement")).hasSize(5);
    }

    @Test
    @Tag("FR-011-03")
    void un_type_inconnu_rend_400_requete_invalide() throws Exception {
        for (String type : List.of("XXX", "epargne", "")) {
            HttpResponse<String> reponse = appeler(CHEMIN + "?type=" + type, "CLIENT");
            if (type.isEmpty()) {
                assertThat(reponse.statusCode()).as("type vide = absent").isEqualTo(200);
            } else {
                assertThat(reponse.statusCode()).as(type).isEqualTo(400);
                assertThat(reponse.body()).isEqualTo("{\"message\":\"Requête invalide\"}");
            }
        }
    }

    @Test
    @Tag("FR-011-02")
    void un_produit_desactive_n_apparait_dans_aucun_groupe() throws Exception {
        jdbc.update("update bank_products set active = false where id in (1, 5, 10)");

        JsonNode corps = json.readTree(appeler(CHEMIN, "CLIENT").body());

        assertThat(noms(corps.get("epargne"))).doesNotContain("Livret A").hasSize(3);
        assertThat(noms(corps.get("credit"))).doesNotContain("Credit Immobilier Classique").hasSize(3);
        assertThat(noms(corps.get("investissement"))).doesNotContain("ETF Monde Diversifie").hasSize(4);
    }

    @Test
    @Tag("FR-011-07")
    void les_valeurs_par_defaut_du_domaine_s_appliquent_aux_colonnes_absentes() throws Exception {
        jdbc.update("insert into bank_products (id, product_type, name) values (20, 'EPARGNE', 'Z Epargne')");
        jdbc.update("insert into savings_products (id) values (20)");
        jdbc.update("insert into bank_products (id, product_type, name) values (21, 'CREDIT', 'Z Credit')");
        jdbc.update("insert into credit_products (id) values (21)");

        JsonNode corps = json.readTree(appeler(CHEMIN, "CLIENT").body());

        JsonNode epargne = corps.get("epargne").get(4);
        assertThat(epargne.get("nom").asString()).isEqualTo("Z Epargne");
        assertThat(epargne.get("capitalGaranti").asBoolean()).isTrue();
        assertThat(epargne.get("taux").isNull()).isTrue();
        JsonNode credit = corps.get("credit").get(4);
        assertThat(credit.get("nom").asString()).isEqualTo("Z Credit");
        assertThat(credit.get("assuranceRequise").asBoolean()).isFalse();
    }

    @Test
    void un_catalogue_vide_rend_trois_groupes_vides() throws Exception {
        jdbc.update("update bank_products set active = false");

        JsonNode corps = json.readTree(appeler(CHEMIN, "CLIENT").body());

        assertThat(corps.get("epargne")).isEmpty();
        assertThat(corps.get("credit")).isEmpty();
        assertThat(corps.get("investissement")).isEmpty();
    }

    private HttpResponse<String> appeler(String chemin, String role) throws Exception {
        Instant maintenant = Instant.now();
        return Jetons.envoyer(port, "GET", chemin,
                Jetons.jeton(Conteneurs.CLES, role, maintenant, maintenant.plusSeconds(1800)), null);
    }

    private static List<String> noms(JsonNode groupe) {
        return groupe.valueStream().map(produit -> produit.get("nom").asString()).toList();
    }
}
