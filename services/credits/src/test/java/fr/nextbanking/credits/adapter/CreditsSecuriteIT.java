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
        // Ces tests constatent le prêt EN_ATTENTE : le scoring (écouteur) ne doit pas le décider.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
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
    @Tag("FR-030-07")
    @Tag("FR-015-01")
    void le_portefeuille_rend_401_sans_jeton_ou_a_un_sujet_illisible_et_403_a_un_client() throws Exception {
        String chemin = "/api/credits/conseiller/portefeuille";
        assertRefus(envoyer(port, "GET", chemin, null, Map.of()));
        assertRefus(avecJeton(port, "GET", chemin, OutilsHttp.jetonValide("abc", "CONSEILLER")));

        HttpResponse<String> client = avecJeton(port, "GET", chemin + "?conseillerId=11",
                OutilsHttp.jetonValide("1", "CLIENT"));
        assertThat(client.statusCode()).isEqualTo(403);
        assertThat(client.body()).isEqualTo(ACCES_REFUSE);
        assertThat(client.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    @Tag("FR-015-02")
    @Tag("FR-030-07")
    void le_conseiller_ne_voit_que_son_portefeuille_meme_avec_un_parametre_force() throws Exception {
        OutilsHttp.executer(base, java.nio.file.Files.readString(
                java.nio.file.Path.of("reference-data/projection-clients.sql")));
        String sophie = OutilsHttp.jetonValide("11", "CONSEILLER");

        for (String parametre : new String[] {"?conseillerId=12", "?clientId=3"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET", "/api/credits/conseiller/portefeuille" + parametre,
                    sophie);
            assertThat(reponse.statusCode()).as(parametre).isEqualTo(200);
            assertThat(reponse.body()).as(parametre).startsWith("[{\"clientId\":1,")
                    .endsWith("{\"clientId\":2,\"prets\":[]}]").doesNotContain("\"clientId\":3", "\"clientId\":4");
        }
    }

    @Test
    @Tag("FR-016-01")
    @Tag("FR-016-02")
    @Tag("FR-030-07")
    void l_indicateur_d_administration_rend_401_sans_jeton_et_403_a_un_client_ou_un_conseiller() throws Exception {
        String chemin = "/api/credits/administration/indicateurs";
        assertRefus(envoyer(port, "GET", chemin, null, Map.of()));

        for (String role : new String[] {"CLIENT", "CONSEILLER"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET", chemin, OutilsHttp.jetonValide("11", role));
            assertThat(reponse.statusCode()).as(role).isEqualTo(403);
            assertThat(reponse.body()).isEqualTo(ACCES_REFUSE);
            assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        }
        assertThat(avecJeton(port, "GET", chemin, OutilsHttp.jetonValide("10", "ADMIN")).statusCode()).isEqualTo(200);
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
