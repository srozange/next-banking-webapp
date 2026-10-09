package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.jetonValide;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Portefeuille du conseiller de bout en bout : déduit du sujet du jeton ({@code customers.advisor_id}), paramètres de
 * requête ignorés, clients inactifs inclus, clients supprimés exclus, ADMIN sans client à liste vide.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-015-01")
@Tag("FR-015-02")
@Tag("FR-030-07")
@Tag("BUG-093")
class PortefeuilleIT {

    private static final String CHEMIN = "/api/identite-clients/conseiller/portefeuille";
    private static final String PORTEFEUILLE_SOPHIE = "["
            + "{\"clientId\":1,\"nomComplet\":\"Jean Dupont\",\"login\":\"user1\","
            + "\"email\":\"jean.dupont@example.com\",\"telephone\":\"+33601020304\",\"actif\":true},"
            + "{\"clientId\":2,\"nomComplet\":\"Marie Lebrun\",\"login\":\"user2\","
            + "\"email\":\"marie.lebrun@example.com\",\"telephone\":\"+33601020305\",\"actif\":true}]";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    @Test
    void sophie_recoit_exactement_ses_deux_clients() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, jetonValide("11", "CONSEILLER"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo(PORTEFEUILLE_SOPHIE);
    }

    @Test
    void un_parametre_force_est_ignore() throws Exception {
        String jeton = jetonValide("11", "CONSEILLER");

        for (String parametre : new String[] {"?conseillerId=12", "?clientId=3", "?conseillerId=12&clientId=3"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN + parametre, jeton);
            assertThat(reponse.statusCode()).as(parametre).isEqualTo(200);
            assertThat(reponse.body()).as(parametre).isEqualTo(PORTEFEUILLE_SOPHIE);
        }
    }

    @Test
    void marc_recoit_ses_propres_clients() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, jetonValide("12", "CONSEILLER"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).contains("\"clientId\":3", "\"clientId\":4")
                .doesNotContain("\"clientId\":1,", "\"clientId\":2,");
    }

    @Test
    void un_admin_sans_client_affecte_recoit_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, jetonValide("10", "ADMIN"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    void un_client_inactif_reste_dans_le_portefeuille_un_client_supprime_en_sort_le_telephone_peut_etre_nul()
            throws Exception {
        OutilsHttp.executer(base, "update customers set active = false, phone = null where id = 2;"
                + "update customers set supprime = true where id = 1;");

        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, jetonValide("11", "CONSEILLER"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[{\"clientId\":2,\"nomComplet\":\"Marie Lebrun\",\"login\":\"user2\","
                + "\"email\":\"marie.lebrun@example.com\",\"telephone\":null,\"actif\":false}]");
    }

    @Test
    void le_tri_suit_le_nom_complet() throws Exception {
        OutilsHttp.executer(base, "update customers set full_name = 'Albert Zola' where id = 2;");

        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, jetonValide("11", "CONSEILLER"));

        assertThat(reponse.body()).startsWith("[{\"clientId\":2,\"nomComplet\":\"Albert Zola\"");
    }
}
