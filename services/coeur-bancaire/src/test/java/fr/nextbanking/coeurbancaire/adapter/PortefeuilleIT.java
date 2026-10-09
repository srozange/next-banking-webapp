package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Portefeuille du conseiller côté cœur bancaire, de bout en bout : projection de référence des clients
 * ({@code reference-data/projection-clients.sql}) et comptes de référence ; déduit du sujet du jeton, paramètres
 * ignorés, codes de type et soldes en chaîne tels quels, ADMIN sans client à liste vide.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-015-01")
@Tag("FR-015-02")
@Tag("FR-015-09")
@Tag("FR-030-07")
@Tag("BUG-093")
class PortefeuilleIT {

    private static final String CHEMIN = "/api/coeur-bancaire/conseiller/portefeuille";
    private static final String PORTEFEUILLE_SOPHIE = """
            [{"clientId":1,"comptes":[\
            {"compteId":1,"numero":"FR76-0001-0001","type":"COURANT","solde":"2130.00"},\
            {"compteId":2,"numero":"FR76-0001-0002","type":"EPARGNE","solde":"15200.00"}]},\
            {"clientId":2,"comptes":[]}]""";

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
    private Comptes comptes;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
        try (Connection connexion = source.getConnection()) {
            ScriptUtils.executeSqlScript(connexion, new FileSystemResource("reference-data/projection-clients.sql"));
        }
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @Test
    void sophie_recoit_ses_deux_clients_avec_leurs_comptes_et_un_client_sans_compte_a_une_liste_vide()
            throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(11, "CONSEILLER"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo(PORTEFEUILLE_SOPHIE);
    }

    @Test
    void un_parametre_force_est_ignore() throws Exception {
        String jeton = Jetons.pour(11, "CONSEILLER");

        for (String parametre : new String[] {"?conseillerId=12", "?clientId=3", "?conseillerId=12&clientId=8"}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN + parametre, jeton, null);
            assertThat(reponse.statusCode()).as(parametre).isEqualTo(200);
            assertThat(reponse.body()).as(parametre).isEqualTo(PORTEFEUILLE_SOPHIE);
        }
    }

    @Test
    void marc_ne_voit_que_ses_clients() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(12, "CONSEILLER"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(json.readTree(reponse.body())).isEqualTo(json.readTree(
                "[{\"clientId\":3,\"comptes\":[]},{\"clientId\":4,\"comptes\":[]}]"));
    }

    @Test
    void un_admin_sans_client_affecte_recoit_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(10, "ADMIN"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    void un_client_supprime_de_la_projection_sort_du_portefeuille() throws Exception {
        jdbc.sql("update clients set supprime = true where client_id = 2").update();

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(11, "CONSEILLER"), null);

        assertThat(json.readTree(reponse.body())).hasSize(1);
        assertThat(reponse.body()).startsWith("[{\"clientId\":1,");
    }

    @Test
    void la_lecture_par_lot_des_comptes_d_aucun_client_est_vide() {
        assertThat(comptes.desClients(List.of())).isEmpty();
        assertThat(comptes.desClients(List.of(8L, 1L))).extracting(compte -> compte.id())
                .containsExactly(1L, 2L, 3L);
    }
}
