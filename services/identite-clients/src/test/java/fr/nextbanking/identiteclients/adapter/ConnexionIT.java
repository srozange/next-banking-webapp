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
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Connexion et profil de bout en bout, sur une vraie base chargée des données de référence (mots de passe de test).
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class ConnexionIT {

    private static final String SC_001_03 =
            "{\"identifiant\":\"retro-compte-inexistant\",\"motDePasse\":\"retro-mauvais-mot-de-passe\"}";
    private static final String USER1 = "{\"identifiant\":\"user1\",\"motDePasse\":\"" + MDP_CLIENT + "\"}";
    private static final String IDENTIFIANTS_INCORRECTS = "{\"message\":\"Identifiants incorrects\"}";
    private static final Pattern LIGNE_DE_JOURNAL = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    private static final Pattern TRACE = Pattern.compile("traceId=(\\S*) spanId=(\\S*)\\]");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
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

    @Test
    @Tag("FR-001-04")
    void user1_actif_recoit_un_jwt_rs256_aux_revendications_attendues() throws Exception {
        HttpResponse<String> reponse = connexion(port, USER1);

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode corps = json.readTree(reponse.body());
        assertThat(corps.propertyNames()).containsExactly("jeton");
        Jwt jeton = decodeur.decode(corps.get("jeton").asString());
        assertThat(jeton.getHeaders()).containsEntry("alg", "RS256");
        assertThat(jeton.getSubject()).isEqualTo("1");
        assertThat(jeton.getClaimAsStringList("roles")).containsExactly("CLIENT");
        assertThat(jeton.getId()).isNotBlank();
        assertThat(Duration.between(jeton.getIssuedAt(), jeton.getExpiresAt())).isEqualTo(Duration.ofSeconds(1800));
        assertThat(jeton.getIssuedAt()).isBetween(Instant.now().minusSeconds(60), Instant.now().plusSeconds(1));
    }

    @Test
    @Tag("FR-001-04")
    void chaque_role_du_legacy_se_connecte_avec_le_mot_de_passe_de_son_role() throws Exception {
        Map<String, String> attendus = Map.of("admin", "ADMIN", "sophie", "CONSEILLER", "marc", "CONSEILLER",
                "user4", "CLIENT");
        Map<String, String> motsDePasse = Map.of("ADMIN", OutilsHttp.MDP_ADMIN, "CONSEILLER",
                OutilsHttp.MDP_CONSEILLER, "CLIENT", MDP_CLIENT);
        for (Map.Entry<String, String> attendu : attendus.entrySet()) {
            HttpResponse<String> reponse = connexion(port, "{\"identifiant\":\"" + attendu.getKey()
                    + "\",\"motDePasse\":\"" + motsDePasse.get(attendu.getValue()) + "\"}");
            assertThat(reponse.statusCode()).as(attendu.getKey()).isEqualTo(200);
            Jwt jeton = decodeur.decode(json.readTree(reponse.body()).get("jeton").asString());
            assertThat(jeton.getClaimAsStringList("roles")).containsExactly(attendu.getValue());
        }
    }

    @Test
    @Tag("FR-001-09")
    @Tag("FR-030-08")
    @Tag("BUG-003")
    @Tag("BUG-092")
    void deux_connexions_donnent_deux_jti_et_aucune_reponse_publique_ne_pose_de_cookie() throws Exception {
        HttpResponse<String> premiere = connexion(port, USER1);
        HttpResponse<String> seconde = connexion(port, USER1);
        HttpResponse<String> refusee = connexion(port, SC_001_03);

        assertThat(jti(premiere)).isNotEqualTo(jti(seconde));
        for (HttpResponse<String> reponse : List.of(premiere, seconde, refusee)) {
            assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        }
        assertThat(refusee.body()).doesNotContain("jeton");
    }

    @Test
    @Tag("FR-001-06")
    void un_compte_inconnu_et_un_mauvais_mot_de_passe_ont_la_meme_reponse() throws Exception {
        HttpResponse<String> inconnu = connexion(port, SC_001_03);
        HttpResponse<String> mauvais = connexion(port, "{\"identifiant\":\"user1\",\"motDePasse\":\"mauvais\"}");

        for (HttpResponse<String> reponse : List.of(inconnu, mauvais)) {
            assertThat(reponse.statusCode()).isEqualTo(401);
            assertThat(reponse.body()).isEqualTo(IDENTIFIANTS_INCORRECTS);
        }
    }

    @Test
    @Tag("FR-001-07")
    @Tag("BUG-001")
    void un_compte_desactive_est_refuse_puis_accepte_une_fois_reactive() throws Exception {
        jdbc.sql("update customers set active = false where username = 'user1'").update();

        HttpResponse<String> desactive = connexion(port, USER1);
        assertThat(desactive.statusCode()).isEqualTo(401);
        assertThat(desactive.body()).isEqualTo(IDENTIFIANTS_INCORRECTS);

        jdbc.sql("update customers set active = true where username = 'user1'").update();

        HttpResponse<String> reactive = connexion(port, USER1);
        assertThat(reactive.statusCode()).isEqualTo(200);
        assertThat(json.readTree(reactive.body()).get("jeton").asString()).isNotBlank();
    }

    @Test
    @Tag("FR-001-03")
    void les_champs_vides_sont_refuses_identifiant_d_abord() throws Exception {
        assertMessage(connexion(port, "{\"identifiant\":\"\",\"motDePasse\":\"x\"}"), 400, "Identifiant requis");
        assertMessage(connexion(port, "{\"identifiant\":\"  \",\"motDePasse\":\"x\"}"), 400, "Identifiant requis");
        assertMessage(connexion(port, "{\"motDePasse\":\"x\"}"), 400, "Identifiant requis");
        assertMessage(connexion(port, "{\"identifiant\":\"user1\",\"motDePasse\":\"\"}"), 400, "Mot de passe requis");
        assertMessage(connexion(port, "{\"identifiant\":\"user1\"}"), 400, "Mot de passe requis");
        assertMessage(connexion(port, "{\"identifiant\":\"\",\"motDePasse\":\"\"}"), 400, "Identifiant requis");
    }

    @Test
    @Tag("FR-001-03")
    void un_corps_invalide_rend_400_requete_invalide_sans_autre_champ() throws Exception {
        List<HttpResponse<String>> reponses = List.of(
                connexion(port, "pas du json"),
                connexion(port, "null"),
                connexion(port, "{\"identifiant\":"),
                envoyer(port, "POST", "/api/identite-clients/connexion", USER1, Map.of()),
                envoyer(port, "POST", "/api/identite-clients/connexion", USER1, Map.of("Content-Type", "text/plain")));

        for (HttpResponse<String> reponse : reponses) {
            assertMessage(reponse, 400, "Requête invalide");
            assertThat(json.readTree(reponse.body()).propertyNames()).containsExactly("message");
        }
    }

    @Test
    @Tag("FR-030-01")
    void un_jeton_perime_presente_a_la_connexion_est_ignore() throws Exception {
        Instant passe = Instant.now().minusSeconds(7200);
        String perime = OutilsHttp.jeton(Conteneurs.CLES, "1", passe, passe.plusSeconds(1800));

        HttpResponse<String> reponse = envoyer(port, "POST", "/api/identite-clients/connexion", USER1,
                Map.of("Content-Type", "application/json", "Authorization", "Bearer " + perime));

        assertThat(reponse.statusCode()).isEqualTo(200);
    }

    @Test
    @Tag("FR-001-04")
    void le_profil_de_user1_se_lit_avec_son_jeton() throws Exception {
        String jeton = json.readTree(connexion(port, USER1).body()).get("jeton").asString();

        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/identite-clients/moi", jeton);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(json.readTree(reponse.body())).isEqualTo(json.readTree(
                "{\"identifiant\":\"user1\",\"nomComplet\":\"Jean Dupont\",\"roles\":[\"CLIENT\"]}"));
    }

    @Test
    void le_profil_d_un_client_absent_ou_d_un_sujet_illisible_rend_401() throws Exception {
        for (String sujet : new String[] {"999", "user1"}) {
            assertMessage(avecJeton(port, "GET", "/api/identite-clients/moi", OutilsHttp.jetonValide(sujet)),
                    401, "Authentification requise");
        }
    }

    @Test
    @Tag("FR-001-08")
    @Tag("BUG-002")
    void les_mots_de_passe_sont_stockes_haches() {
        List<String> empreintes = jdbc.sql("select password from customers").query(String.class).list();

        assertThat(empreintes).hasSize(7).allSatisfy(empreinte ->
                assertThat(empreinte).startsWith("{bcrypt}").doesNotContain(MDP_CLIENT));
    }

    @Test
    @Tag("FR-001-08")
    @Tag("BUG-002")
    void le_journal_ne_porte_ni_mot_de_passe_ni_jeton_et_chaque_ligne_porte_la_trace(CapturedOutput sortie)
            throws Exception {
        String jeton = json.readTree(connexion(port, USER1).body()).get("jeton").asString();
        connexion(port, "{\"identifiant\":\"user1\",\"motDePasse\":\"mot-de-passe-refuse\"}");

        List<String> lignes = Arrays.stream(sortie.getOut().split("\\R")).filter(l -> !l.isBlank()).toList();
        assertThat(lignes).noneMatch(ligne -> ligne.contains(MDP_CLIENT) || ligne.contains("mot-de-passe-refuse")
                || ligne.contains(jeton));
        assertThat(lignes).filteredOn(ligne -> LIGNE_DE_JOURNAL.matcher(ligne).matches())
                .isNotEmpty()
                .allSatisfy(ligne -> assertThat(ligne).contains("traceId=", "spanId="));
        List<String> connexions = lignes.stream()
                .filter(ligne -> ligne.contains("Connexion réussie") || ligne.contains("Connexion refusée")).toList();
        assertThat(connexions).hasSize(2).allSatisfy(ligne -> {
            Matcher trace = TRACE.matcher(ligne);
            assertThat(trace.find()).as(ligne).isTrue();
            assertThat(trace.group(1)).as(ligne).isNotEmpty();
            assertThat(trace.group(2)).as(ligne).isNotEmpty();
        });
    }

    private String jti(HttpResponse<String> reponse) {
        return decodeur.decode(json.readTree(reponse.body()).get("jeton").asString()).getId();
    }

    private void assertMessage(HttpResponse<String> reponse, int statut, String message) {
        assertThat(reponse.statusCode()).as(reponse.body()).isEqualTo(statut);
        assertThat(json.readTree(reponse.body()).get("message").asString()).isEqualTo(message);
    }
}
