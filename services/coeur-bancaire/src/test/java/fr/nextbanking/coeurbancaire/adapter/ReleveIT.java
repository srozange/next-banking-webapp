package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
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
 * Relevé d'un compte de bout en bout sur une vraie base : comptes avec solde, opérations triées par date décroissante
 * et sans limite, propriété du compte (403, 404, aucune opération renvoyée), authentification.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class ReleveIT {

    private static final String COMPTES = "/api/coeur-bancaire/comptes";
    private static final String TABLEAU = "/api/coeur-bancaire/tableau-de-bord";

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

    @Autowired
    private Horloge horloge;

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
    @Tag("FR-005-01")
    void sans_jeton_les_comptes_et_les_operations_rendent_401() throws Exception {
        assertThat(Jetons.envoyer(port, "GET", COMPTES, null, null).statusCode()).isEqualTo(401);
        assertThat(Jetons.envoyer(port, "GET", COMPTES + "/1/operations", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    @Tag("FR-005-02")
    @Tag("FR-005-04")
    void les_comptes_portent_leur_solde_et_un_utilisateur_sans_compte_n_en_a_aucun() throws Exception {
        HttpResponse<String> client = Jetons.envoyer(port, "GET", COMPTES, Jetons.pour(1, "CLIENT"), null);
        HttpResponse<String> admin = Jetons.envoyer(port, "GET", COMPTES, Jetons.pour(10, "ADMIN"), null);

        assertThat(client.statusCode()).isEqualTo(200);
        JsonNode comptes = json.readTree(client.body());
        assertThat(comptes).hasSize(2);
        assertThat(comptes.get(0).get("solde").asString()).isEqualTo("2450.00");
        assertThat(comptes.get(1).get("solde").asString()).isEqualTo("15000.00");
        assertThat(admin.body()).isEqualTo("[]");
    }

    @Test
    @Tag("FR-005-05")
    void toutes_les_operations_du_compte_sont_rendues_par_date_decroissante() throws Exception {
        operation(1, "CREDIT", "10.00", "2130.00", "ancienne", "2026-11-01 08:00:00+00");
        operation(2, "CREDIT", "5.00", "15205.00", "autre compte", "2026-11-09 08:00:00+00");
        operation(1, "DEBIT", "30.00", "2100.00", "[EN ATTENTE] recente", "2026-11-03 08:00:00+00");
        operation(1, "CREDIT", "20.00", "2120.00", "milieu", "2026-11-02 08:00:00+00");
        for (int i = 0; i < 120; i++) {
            operation(1, "CREDIT", "1.00", "2100.00", "ancienne " + i, "2026-10-01 08:00:00+00");
        }

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", COMPTES + "/1/operations",
                Jetons.pour(1, "CLIENT"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode operations = json.readTree(reponse.body());
        assertThat(operations).hasSize(123);
        assertThat(operations.get(0).get("description").asString()).isEqualTo("[EN ATTENTE] recente");
        assertThat(operations.get(0).get("date").asString()).isEqualTo("2026-11-03T08:00:00Z");
        assertThat(operations.get(0).get("type").asString()).isEqualTo("DEBIT");
        assertThat(operations.get(0).get("montant").asString()).isEqualTo("30.00");
        assertThat(operations.get(0).get("soldeApres").asString()).isEqualTo("2100.00");
        assertThat(operations.get(1).get("description").asString()).isEqualTo("milieu");
        assertThat(operations.get(2).get("description").asString()).isEqualTo("ancienne");
    }

    @Test
    @Tag("FR-005-05")
    void un_compte_sans_operation_rend_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", COMPTES + "/2/operations",
                Jetons.pour(1, "CLIENT"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    @Tag("FR-005-05")
    void le_compte_d_un_autre_client_rend_403_et_un_compte_inexistant_404_sans_operation() throws Exception {
        operation(3, "CREDIT", "10.00", "510.00", "secrete", "2026-11-01 08:00:00+00");
        String jeton = Jetons.pour(1, "CLIENT");

        HttpResponse<String> etranger = Jetons.envoyer(port, "GET", COMPTES + "/3/operations", jeton, null);
        HttpResponse<String> inexistant = Jetons.envoyer(port, "GET", COMPTES + "/999/operations", jeton, null);
        HttpResponse<String> illisible = Jetons.envoyer(port, "GET", COMPTES + "/abc/operations", jeton, null);

        assertThat(etranger.statusCode()).isEqualTo(403);
        assertThat(etranger.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
        assertThat(inexistant.statusCode()).isEqualTo(404);
        assertThat(inexistant.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
        assertThat(illisible.statusCode()).isEqualTo(400);
    }

    @Test
    @Tag("FR-002-03")
    void le_tableau_de_bord_sans_jeton_rend_401() throws Exception {
        assertThat(Jetons.envoyer(port, "GET", TABLEAU, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    @Tag("FR-002-03")
    @Tag("FR-002-04")
    void le_tableau_de_bord_rend_les_comptes_le_solde_total_et_ignore_un_compte_d_autrui_en_parametre()
            throws Exception {
        operation(3, "CREDIT", "10.00", "510.00", "secrete", "2026-11-01 08:00:00+00");

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", TABLEAU + "?compteId=3&clientId=8",
                Jetons.pour(1, "CLIENT"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode vue = json.readTree(reponse.body());
        assertThat(vue.get("comptes")).hasSize(2);
        assertThat(vue.get("comptes").get(0).get("type").asString()).isEqualTo("COURANT");
        assertThat(vue.get("soldeTotal").asString()).isEqualTo("17450.00");
        assertThat(vue.get("dernieresOperations")).isEmpty();
        assertThat(vue.get("aujourdhui").asString()).isEqualTo(horloge.aujourdhui().toString())
                .matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    @Tag("FR-002-05")
    void un_utilisateur_sans_compte_a_un_tableau_de_bord_vide() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", TABLEAU, Jetons.pour(10, "CONSEILLER"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("{\"comptes\":[],\"soldeTotal\":\"0.00\",\"dernieresOperations\":[],"
                + "\"aujourdhui\":\"" + horloge.aujourdhui() + "\"}");
    }

    @Test
    @Tag("NFR-PAR-04")
    @Tag("FR-002-06")
    void avec_les_donnees_de_reference_entieres_le_tableau_de_bord_de_user1_est_celui_de_l_import() throws Exception {
        DonneesDeReference.rechargerTout(source, jdbc);

        JsonNode vue = json.readTree(Jetons.envoyer(port, "GET", TABLEAU, Jetons.pour(1, "CLIENT"), null).body());

        assertThat(vue.get("soldeTotal").asString()).isEqualTo("17450.00");
        JsonNode operations = vue.get("dernieresOperations");
        assertThat(operations).hasSize(5);
        assertThat(operations.get(0).get("description").asString()).isEqualTo("Cinema UGC - sorties loisirs");
        assertThat(operations.get(0).get("date").asString()).isEqualTo("2025-11-27T18:00:00Z");
        assertThat(operations.get(4).get("description").asString()).isEqualTo("Pharmacie Monge - medicaments");
    }

    @Test
    @Tag("FR-002-05")
    @Tag("BUG-005")
    void le_tableau_de_bord_rend_les_cinq_dernieres_operations_du_courant_meme_si_l_epargne_est_creee_avant()
            throws Exception {
        jdbc.sql("update comptes set type = 'EPARGNE' where id = 1").update();
        jdbc.sql("update comptes set type = 'COURANT' where id = 2").update();
        operation(1, "CREDIT", "1.00", "1.00", "epargne", "2026-11-20 08:00:00+00");
        for (int jour = 1; jour <= 7; jour++) {
            operation(2, "DEBIT", "1.00", "2.00", "courant " + jour, "2026-11-0" + jour + " 08:00:00+00");
        }

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", TABLEAU, Jetons.pour(1, "CLIENT"), null);

        JsonNode operations = json.readTree(reponse.body()).get("dernieresOperations");
        assertThat(operations).hasSize(5);
        assertThat(operations.get(0).get("description").asString()).isEqualTo("courant 7");
        assertThat(operations.get(4).get("description").asString()).isEqualTo("courant 3");
    }

    private void operation(long compteId, String type, String montant, String soldeApres, String description,
            String date) {
        jdbc.sql("""
                insert into operations (compte_id, type, montant, solde_apres, description, date_operation)
                values (:compte, :type, :montant::numeric, :solde::numeric, :description, :date::timestamptz)""")
                .param("compte", compteId).param("type", type).param("montant", montant).param("solde", soldeApres)
                .param("description", description).param("date", date).update();
    }
}
