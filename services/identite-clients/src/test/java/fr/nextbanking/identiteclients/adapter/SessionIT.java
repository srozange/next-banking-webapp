package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.MDP_CLIENT;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.connexion;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.envoyer;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Déconnexion (audit LOGOUT, sans liste de révocation) et renouvellement de la session, de bout en bout.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-001-10")
@Tag("FR-030-05")
class SessionIT {

    private static final String USER1 = "{\"identifiant\":\"user1\",\"motDePasse\":\"" + MDP_CLIENT + "\"}";
    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("nb.reseau.mandataires-de-confiance", () -> "127.0.0.1/32,::1/128");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    private final JsonMapper json = JsonMapper.builder().build();
    private final NimbusJwtDecoder decodeur =
            NimbusJwtDecoder.withPublicKey((RSAPublicKey) Conteneurs.CLES.getPublic()).build();

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    private String jetonUser1() throws Exception {
        return json.readTree(connexion(port, USER1).body()).get("jeton").asString();
    }

    @Test
    @Tag("FR-001-12")
    @Tag("FR-026-01")
    @Tag("FR-026-05")
    void la_deconnexion_rend_204_et_ecrit_logout_avec_l_adresse_ip() throws Exception {
        String jeton = jetonUser1();
        long depuis = LignesAudit.dernierId(jdbc);

        HttpResponse<String> reponse = envoyer(port, "POST", "/api/identite-clients/deconnexion", null,
                Map.of("Authorization", "Bearer " + jeton, "X-Forwarded-For", "203.0.113.7"));

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        List<LignesAudit.Ligne> lignes = LignesAudit.depuis(jdbc, depuis);
        assertThat(lignes).singleElement().satisfies(ligne -> {
            assertThat(ligne.cle()).isEqualTo("1");
            assertThat(ligne.donnees().get("action").asString()).isEqualTo("LOGOUT");
            assertThat(ligne.donnees().get("detail").asString()).isEqualTo("Déconnexion");
            assertThat(ligne.donnees().get("utilisateur").asString()).isEqualTo("1");
            assertThat(ligne.donnees().get("adresseIp").asString()).isEqualTo("203.0.113.7");
        });
    }

    @Test
    void sans_jeton_la_deconnexion_et_le_renouvellement_rendent_401() throws Exception {
        for (String chemin : new String[] {"/api/identite-clients/deconnexion", "/api/identite-clients/renouvellement"}) {
            HttpResponse<String> reponse = envoyer(port, "POST", chemin, null, Map.of());
            assertThat(reponse.statusCode()).as(chemin).isEqualTo(401);
            assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        }
    }

    @Test
    void le_jeton_reste_accepte_apres_la_deconnexion_jusqu_a_son_exp() throws Exception {
        String jeton = jetonUser1();

        assertThat(avecJeton(port, "POST", "/api/identite-clients/deconnexion", jeton).statusCode()).isEqualTo(204);

        assertThat(avecJeton(port, "GET", "/api/identite-clients/moi", jeton).statusCode()).isEqualTo(200);
    }

    @Test
    void le_renouvellement_rend_un_jeton_neuf() throws Exception {
        String ancien = jetonUser1();

        HttpResponse<String> reponse = avecJeton(port, "POST", "/api/identite-clients/renouvellement", ancien);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        var corps = json.readTree(reponse.body());
        assertThat(corps.propertyNames()).containsExactly("jeton");
        Jwt neuf = decodeur.decode(corps.get("jeton").asString());
        Jwt precedent = decodeur.decode(ancien);
        assertThat(neuf.getSubject()).isEqualTo("1");
        assertThat(neuf.getClaimAsStringList("roles")).containsExactly("CLIENT");
        assertThat(neuf.getId()).isNotEqualTo(precedent.getId());
        assertThat(neuf.getExpiresAt()).isEqualTo(neuf.getIssuedAt().plusSeconds(1800));
    }

    @Test
    void un_client_desactive_ou_inconnu_ne_renouvelle_pas() throws Exception {
        jdbc.sql("update customers set active = false where id = 1").update();
        String desactive = OutilsHttp.jetonValide("1");
        String inconnu = OutilsHttp.jetonValide("9999");

        for (String jeton : new String[] {desactive, inconnu}) {
            HttpResponse<String> reponse = avecJeton(port, "POST", "/api/identite-clients/renouvellement", jeton);
            assertThat(reponse.statusCode()).isEqualTo(401);
            assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
            assertThat(reponse.body()).doesNotContain("jeton\"");
        }
    }

    @Test
    void un_jeton_expire_ou_mal_signe_ne_renouvelle_pas() throws Exception {
        Instant maintenant = Instant.now();
        String expire = OutilsHttp.jeton(Conteneurs.CLES, "1", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));

        assertThat(avecJeton(port, "POST", "/api/identite-clients/renouvellement", expire).statusCode())
                .isEqualTo(401);
        assertThat(avecJeton(port, "POST", "/api/identite-clients/renouvellement", "illisible").statusCode())
                .isEqualTo(401);
    }
}
