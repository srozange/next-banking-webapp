package fr.nextbanking.credits.adapter;

import static fr.nextbanking.credits.adapter.OutilsHttp.avecJeton;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sql.DataSource;
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
 * Portefeuille du conseiller côté crédits, de bout en bout : projection de référence des clients
 * ({@code reference-data/projection-clients.sql}) et prêts de référence du client 1, plus des prêts des clients 1 et 3 ;
 * déduit du sujet du jeton, paramètres ignorés, codes et montants tels que stockés, ADMIN sans client à liste vide.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-015-01")
@Tag("FR-015-02")
@Tag("FR-015-09")
@Tag("FR-030-07")
@Tag("BUG-093")
class PortefeuilleHttpIT {

    private static final String CHEMIN = "/api/credits/conseiller/portefeuille";
    private static final String PORTEFEUILLE_SOPHIE = "[{\"clientId\":1,\"prets\":["
            + "{\"pretId\":2,\"type\":\"AUTO\",\"montant\":\"9000.00\",\"mensualite\":\"1517.11\",\"statut\":\"EN_COURS\"},"
            + "{\"pretId\":50,\"type\":null,\"montant\":\"3000.00\",\"mensualite\":\"260.00\",\"statut\":\"EN_ATTENTE\"},"
            + "{\"pretId\":1,\"type\":\"TRAVAUX\",\"montant\":\"5000.00\",\"mensualite\":\"427.81\",\"statut\":\"EN_COURS\"},"
            + "{\"pretId\":51,\"type\":\"ETUDIANT\",\"montant\":\"8000.00\",\"mensualite\":\"700.50\",\"statut\":\"DEFAUT\"}]},"
            + "{\"clientId\":2,\"prets\":[]}]";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // La projection vient du fichier de référence : ni scoring ni consommation des clients pendant ces tests.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PretRepository depot;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void donnees() throws Exception {
        OutilsHttp.chargerDonnees(base);
        OutilsHttp.executer(base, Files.readString(Path.of("reference-data/projection-clients.sql"),
                StandardCharsets.UTF_8));
        OutilsHttp.executer(base, """
                insert into loans (id, customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (50, 1, null, 3000, 4.0, 12, 260.00, 3120.00, 120.00, 'Sans type', 'EN_ATTENTE',
                        date '2026-07-01', date '2027-07-01'),
                       (51, 1, 'ETUDIANT', 8000, 2.5, 12, 700.50, 8406.00, 406.00, 'Etudes', 'DEFAUT',
                        date '2026-05-01', date '2027-05-01'),
                       (52, 3, 'IMMOBILIER', 200000, 3.2, 240, 1129.84, 271161.60, 71161.60, 'Maison', 'EN_COURS',
                        date '2026-09-01', date '2046-09-01');""");
    }

    @Test
    void sophie_recoit_ses_deux_clients_avec_leurs_prets_et_un_client_sans_pret_a_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, OutilsHttp.jetonValide("11", "CONSEILLER"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo(PORTEFEUILLE_SOPHIE);
    }

    @Test
    void un_parametre_force_est_ignore() throws Exception {
        String jeton = OutilsHttp.jetonValide("11", "CONSEILLER");

        for (String parametre : new String[] {"?conseillerId=12", "?clientId=3", "?conseillerId=12&clientId=3"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN + parametre, jeton);
            assertThat(reponse.statusCode()).as(parametre).isEqualTo(200);
            assertThat(reponse.body()).as(parametre).isEqualTo(PORTEFEUILLE_SOPHIE);
        }
    }

    @Test
    void marc_ne_voit_que_ses_clients() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, OutilsHttp.jetonValide("12", "CONSEILLER"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(json.readTree(reponse.body())).isEqualTo(json.readTree("""
                [{"clientId":3,"prets":[{"pretId":52,"type":"IMMOBILIER","montant":"200000.00",
                  "mensualite":"1129.84","statut":"EN_COURS"}]},
                 {"clientId":4,"prets":[]}]"""));
    }

    @Test
    void un_admin_sans_client_affecte_recoit_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", CHEMIN, OutilsHttp.jetonValide("10", "ADMIN"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    void un_client_supprime_ou_reaffecte_sort_du_portefeuille() throws Exception {
        jdbc.sql("update clients set supprime = true where client_id = 2").update();
        jdbc.sql("update clients set conseiller_id = 12 where client_id = 1").update();

        HttpResponse<String> sophie = avecJeton(port, "GET", CHEMIN, OutilsHttp.jetonValide("11", "CONSEILLER"));
        HttpResponse<String> marc = avecJeton(port, "GET", CHEMIN, OutilsHttp.jetonValide("12", "CONSEILLER"));

        assertThat(sophie.body()).isEqualTo("[]");
        assertThat(json.readTree(marc.body())).extracting(client -> client.get("clientId").asLong())
                .containsExactly(1L, 3L, 4L);
    }

    @Test
    void la_lecture_par_lot_des_prets_d_aucun_client_est_vide() {
        assertThat(depot.desClients(List.of())).isEmpty();
        assertThat(depot.desClients(List.of(3L, 2L))).extracting(pret -> pret.id()).containsExactly(52L);
    }
}
