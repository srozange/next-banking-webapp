package fr.nextbanking.credits.adapter;

import static fr.nextbanking.credits.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.credits.adapter.OutilsHttp.envoyer;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Map;
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
 * Contrôle d'accès du service credits : tout est authentifié (tout rôle) sauf {@code GET /actuator/health} ; refus
 * en JSON {@code {"message"}} : 401 sans jeton valide, 403 pour le prêt d'un autre client, 404 pour un prêt
 * inexistant.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-007-01")
class CreditsSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";
    private static final String ACCES_REFUSE = "{\"message\":\"Accès refusé\"}";
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
    void sans_jeton_les_points_et_les_chemins_voisins_rendent_401() throws Exception {
        assertRefus(envoyer(port, "GET", "/api/credits/prets", null, Map.of()));
        assertRefus(envoyer(port, "GET", "/api/credits/prets/1", null, Map.of()));
        assertRefus(envoyer(port, "POST", "/api/credits/simulations", "{}", Map.of("Content-Type", "application/json")));
        assertRefus(envoyer(port, "GET", "/api/credits/inconnu", null, Map.of()));
        assertRefus(envoyer(port, "GET", "/actuator/info", null, Map.of()));
    }

    @Test
    void un_jeton_expire_mal_signe_ou_illisible_rend_401() throws Exception {
        Instant maintenant = Instant.now();
        String expire = OutilsHttp.jeton(Conteneurs.CLES, "1", "CLIENT", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = OutilsHttp.jeton(generateur.generateKeyPair(), "1", "CLIENT", maintenant,
                maintenant.plusSeconds(1800));

        for (String jeton : new String[] {expire, malSigne, "illisible", OutilsHttp.jetonValide("1") + "x"}) {
            assertRefus(avecJeton(port, "GET", "/api/credits/prets", jeton));
        }
    }

    @Test
    void un_sujet_qui_n_est_pas_un_identifiant_numerique_rend_401() throws Exception {
        assertRefus(avecJeton(port, "GET", "/api/credits/prets", OutilsHttp.jetonValide("user1")));
        assertRefus(avecJeton(port, "GET", "/api/credits/prets/1", OutilsHttp.jetonValide("user1")));
    }

    @Test
    void la_sante_est_publique() throws Exception {
        HttpResponse<String> reponse = envoyer(port, "GET", "/actuator/health", null, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void tout_role_authentifie_accede_a_la_liste() throws Exception {
        for (String role : new String[] {"CLIENT", "CONSEILLER", "ADMIN"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET", "/api/credits/prets",
                    OutilsHttp.jetonValide("1", role));

            assertThat(reponse.statusCode()).as(role).isEqualTo(200);
            assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        }
    }

    @Test
    void le_pret_d_un_autre_client_rend_403_et_un_pret_inexistant_404() throws Exception {
        HttpResponse<String> autrui = avecJeton(port, "GET", "/api/credits/prets/1", OutilsHttp.jetonValide("2"));
        assertThat(autrui.statusCode()).isEqualTo(403);
        assertThat(autrui.body()).isEqualTo(ACCES_REFUSE);
        assertThat(autrui.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));

        HttpResponse<String> inconnu = avecJeton(port, "GET", "/api/credits/prets/999", OutilsHttp.jetonValide("1"));
        assertThat(inconnu.statusCode()).isEqualTo(404);
        assertThat(inconnu.body()).isEqualTo(RESSOURCE_INTROUVABLE);
    }

    @Test
    void le_proprietaire_consulte_son_pret() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/credits/prets/1", OutilsHttp.jetonValide("1"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).contains("\"id\":1", "\"statut\":\"EN_COURS\"");
    }

    @Test
    void avec_jeton_une_route_inconnue_rend_404_une_methode_non_prevue_405_et_un_identifiant_illisible_400()
            throws Exception {
        String jeton = OutilsHttp.jetonValide("1");

        HttpResponse<String> inconnue = avecJeton(port, "GET", "/api/credits/inconnu", jeton);
        assertThat(inconnue.statusCode()).isEqualTo(404);
        assertThat(inconnue.body()).isEqualTo(RESSOURCE_INTROUVABLE);

        HttpResponse<String> methode = avecJeton(port, "GET", "/api/credits/simulations", jeton);
        assertThat(methode.statusCode()).isEqualTo(405);
        assertThat(methode.body()).isEqualTo(RESSOURCE_INTROUVABLE);

        HttpResponse<String> illisible = avecJeton(port, "GET", "/api/credits/prets/abc", jeton);
        assertThat(illisible.statusCode()).isEqualTo(400);
        assertThat(illisible.body()).isEqualTo("{\"message\":\"Requête invalide\"}");
    }

    private static void assertRefus(HttpResponse<String> reponse) {
        assertThat(reponse.statusCode()).as(reponse.uri().toString()).isEqualTo(401);
        assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }
}
