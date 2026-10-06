package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.envoyer;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import javax.sql.DataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Contrôle d'accès du service : tout est authentifié sauf {@code POST /api/identite-clients/connexion} et
 * {@code GET /actuator/health}, comparés sur le chemin exact ; refus en JSON, sans cookie.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-030-01")
class IdentiteClientsSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";
    private static final String RESSOURCE_INTROUVABLE = "{\"message\":\"Ressource introuvable\"}";

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
    void sans_jeton_les_points_proteges_et_les_chemins_voisins_de_la_connexion_rendent_401() throws Exception {
        assertRefus(envoyer(port, "GET", "/api/identite-clients/moi", null, Map.of()));
        assertRefus(envoyer(port, "GET", "/api/identite-clients/connexion", null, Map.of()));
        assertRefus(envoyer(port, "POST", "/api/identite-clients/connexion/x", "{}",
                Map.of("Content-Type", "application/json")));
        assertRefus(envoyer(port, "POST", "/api/identite-clients/connexion/", "{}",
                Map.of("Content-Type", "application/json")));
        assertRefus(envoyer(port, "GET", "/api/identite-clients/inconnu", null, Map.of()));
        assertRefus(envoyer(port, "GET", "/actuator/info", null, Map.of()));
    }

    @Test
    void un_jeton_expire_mal_signe_ou_illisible_rend_401() throws Exception {
        Instant maintenant = Instant.now();
        String expire = OutilsHttp.jeton(Conteneurs.CLES, "1", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = OutilsHttp.jeton(generateur.generateKeyPair(), "1", maintenant, maintenant.plusSeconds(1800));

        for (String jeton : new String[] {expire, malSigne, "illisible", OutilsHttp.jetonValide("1") + "x"}) {
            assertRefus(avecJeton(port, "GET", "/api/identite-clients/moi", jeton));
        }
    }

    @Test
    void la_sante_est_publique() throws Exception {
        HttpResponse<String> reponse = envoyer(port, "GET", "/actuator/health", null, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void avec_jeton_une_route_inconnue_rend_404_et_une_methode_non_prevue_405() throws Exception {
        String jeton = OutilsHttp.jetonValide("1");

        HttpResponse<String> inconnue = avecJeton(port, "GET", "/api/identite-clients/inconnu", jeton);
        assertThat(inconnue.statusCode()).isEqualTo(404);
        assertThat(inconnue.body()).isEqualTo(RESSOURCE_INTROUVABLE);

        HttpResponse<String> methode = avecJeton(port, "GET", "/api/identite-clients/connexion", jeton);
        assertThat(methode.statusCode()).isEqualTo(405);
        assertThat(methode.body()).isEqualTo(RESSOURCE_INTROUVABLE);
    }

    @Test
    void avec_jeton_valide_le_profil_est_accessible() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/identite-clients/moi", OutilsHttp.jetonValide("1"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    @Tag("FR-030-07")
    void un_jeton_client_ou_conseiller_est_refuse_en_403_sur_la_desactivation_et_l_activation() throws Exception {
        for (String role : new String[] {"CLIENT", "CONSEILLER"}) {
            String jeton = OutilsHttp.jetonValide("1", role);
            for (String action : new String[] {"desactivation", "activation"}) {
                HttpResponse<String> reponse = avecJeton(port, "POST", "/api/identite-clients/clients/1/" + action,
                        jeton);
                assertThat(reponse.statusCode()).as(role + " " + action).isEqualTo(403);
                assertThat(reponse.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
                assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
            }
        }
    }

    @Test
    @Tag("FR-030-07")
    void sans_jeton_la_desactivation_et_l_activation_rendent_401() throws Exception {
        assertRefus(envoyer(port, "POST", "/api/identite-clients/clients/1/desactivation", null, Map.of()));
        assertRefus(envoyer(port, "POST", "/api/identite-clients/clients/1/activation", null, Map.of()));
    }

    private static void assertRefus(HttpResponse<String> reponse) {
        assertThat(reponse.statusCode()).as(reponse.uri().toString()).isEqualTo(401);
        assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }
}
