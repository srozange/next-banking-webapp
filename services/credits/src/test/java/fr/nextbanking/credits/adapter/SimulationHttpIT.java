package fr.nextbanking.credits.adapter;

import static fr.nextbanking.credits.adapter.OutilsHttp.publier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import fr.nextbanking.credits.application.port.out.CalculSimulation;
import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * {@code POST /api/credits/simulations} de bout en bout : calcul de référence, bornes, refus 400 aux textes du
 * legacy et panne du calcul en 5xx au message fixe.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class SimulationHttpIT {

    private static final String CHEMIN = "/api/credits/simulations";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Ces tests constatent le prêt EN_ATTENTE : le scoring (écouteur) ne doit pas le décider.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private CalculSimulation calcul;

    private HttpResponse<String> simuler(String corps) throws Exception {
        return publier(port, CHEMIN, corps, OutilsHttp.jetonValide("1"));
    }

    private static String corps(String montant, String taux, String duree) {
        return "{\"montant\":" + montant + ",\"tauxAnnuel\":" + taux + ",\"dureeMois\":" + duree + "}";
    }

    @Test
    @Tag("FR-007-03")
    void la_reference_rend_293_02_10548_72_548_72_et_36_lignes() throws Exception {
        HttpResponse<String> reponse = simuler(corps("\"10000\"", "\"3.5\"", "\"36\""));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        assertThat(reponse.body()).contains("\"mensualite\":\"293.02\"", "\"coutTotal\":\"10548.72\"",
                "\"totalInterets\":\"548.72\"", "\"numero\":36", "\"capitalRestant\":\"0.00\"");
        assertThat(reponse.body().split("\"numero\"", -1)).hasSize(37);
        assertThat(reponse.body()).contains(
                "{\"numero\":1,\"capital\":\"263.85\",\"interets\":\"29.17\",\"total\":\"293.02\","
                        + "\"capitalRestant\":\"9736.15\"}");
    }

    @Test
    @Tag("FR-007-03")
    void les_nombres_json_sont_acceptes_comme_des_chaines() throws Exception {
        HttpResponse<String> reponse = simuler(corps("12000", "0", "24"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).contains("\"mensualite\":\"500.00\"");
    }

    @Test
    @Tag("FR-007-02")
    void les_bornes_taux_30_et_duree_360_sont_acceptees() throws Exception {
        assertThat(simuler(corps("\"10000\"", "\"30\"", "\"36\"")).statusCode()).isEqualTo(200);
        HttpResponse<String> duree = simuler(corps("\"10000\"", "\"3.5\"", "\"360\""));
        assertThat(duree.statusCode()).isEqualTo(200);
        assertThat(duree.body().split("\"numero\"", -1)).hasSize(361);
    }

    @Test
    @Tag("FR-007-02")
    void un_taux_hors_plage_rend_400() throws Exception {
        for (String taux : new String[] {"\"-1\"", "\"30.01\""}) {
            assertRefus(simuler(corps("\"10000\"", taux, "\"36\"")), "Taux invalide (0-30%)");
        }
    }

    @Test
    @Tag("FR-007-02")
    void une_duree_hors_plage_rend_400() throws Exception {
        for (String duree : new String[] {"\"0\"", "\"361\""}) {
            assertRefus(simuler(corps("\"10000\"", "\"3.5\"", duree)), "Duree invalide (1-360 mois)");
        }
    }

    @Test
    @Tag("FR-004-05")
    void un_montant_vide_ou_nul_rend_400() throws Exception {
        for (String montant : new String[] {"\"\"", "\"0\"", "null"}) {
            assertRefus(simuler(corps(montant, "\"3.5\"", "\"36\"")), "Montant invalide");
        }
        assertRefus(simuler("{}"), "Montant invalide");
    }

    @Test
    void un_corps_illisible_rend_400() throws Exception {
        HttpResponse<String> reponse = simuler("pas du json");

        assertThat(reponse.statusCode()).isEqualTo(400);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Requête invalide\"}");
    }

    @Test
    @Tag("FR-007-03")
    void une_panne_du_calcul_rend_500_au_message_fixe_sans_detail() throws Exception {
        doThrow(new IllegalStateException("détail interne sensible")).when(calcul).calculer(any());

        HttpResponse<String> reponse = simuler(corps("\"10000\"", "\"3.5\"", "\"36\""));

        assertThat(reponse.statusCode()).isEqualTo(500);
        assertThat(reponse.body()).isEqualTo(
                "{\"message\":\"Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer.\"}");
        assertThat(reponse.body()).doesNotContain("détail interne", "Exception", "at fr.");
    }

    private static void assertRefus(HttpResponse<String> reponse, String message) {
        assertThat(reponse.statusCode()).isEqualTo(400);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"" + message + "\"}");
    }
}
