package fr.nextbanking.documents.adapter;

import static fr.nextbanking.documents.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.documents.adapter.OutilsHttp.envoyer;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.infrastructure.ApplicationDocuments;
import fr.nextbanking.documents.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrôle d'accès du service documents : tout est authentifié (tout rôle) sauf {@code GET /actuator/health} ; refus en
 * JSON {@code {"message"}} : 401 sans jeton valide, 403 pour le compte d'un autre client, 404 pour un compte inexistant.
 * Le titulaire seul télécharge ses documents, CLIENT, CONSEILLER et ADMIN compris.
 */
@SpringBootTest(classes = {ApplicationDocuments.class, ConfigurationHorlogeFixe.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class DocumentsSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";
    private static final Path SC_017_01 = Path.of("../../docs/retro/golden-master/derived/FEAT-017/SC-017-01");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
        // Le compte 3 (COURANT de user2, client 2) vient des données de référence (import.sql du legacy).
        OutilsHttp.insererCompte(base, 10, 10, "FR76-0010-0001", "COURANT", "100.00");
        OutilsHttp.insererCompte(base, 11, 11, "FR76-0011-0001", "COURANT", "110.00");
    }

    private long totalDesCompteurs() {
        return jdbc.sql("select coalesce(sum(download_count), 0) from documents").query(Long.class).single();
    }

    @Test
    @Tag("FR-017-01")
    @Tag("FR-008-02")
    void la_demande_de_sc_017_01_sans_jeton_rend_401_sans_document_genere_ni_compte() throws Exception {
        JsonNode demande = json.readTree(SC_017_01.resolve("input-request.json").toFile());
        int statutAttendu = json.readTree(SC_017_01.resolve("expected-response.json").toFile()).get("status_code").asInt();
        long avant = totalDesCompteurs();
        long lignes = jdbc.sql("select count(*) from documents").query(Long.class).single();

        HttpResponse<String> reponse = envoyer(port, demande.get("method").asString(),
                demande.get("path").asString(), null, headers(demande.get("headers")));

        assertThat(demande.get("path").asString()).isEqualTo("/api/documents/telechargement?type=RIB&compteId=1");
        assertThat(reponse.statusCode()).isEqualTo(statutAttendu).isEqualTo(401);
        assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(reponse.headers().firstValue("Location")).isEmpty();
        assertThat(totalDesCompteurs()).isEqualTo(avant);
        assertThat(jdbc.sql("select count(*) from documents").query(Long.class).single()).isEqualTo(lignes);
    }

    private static Map<String, String> headers(JsonNode noeud) {
        Map<String, String> entetes = new java.util.HashMap<>();
        noeud.propertyNames().forEach(nom -> entetes.put(nom, noeud.get(nom).asString()));
        return entetes;
    }

    @Test
    @Tag("FR-017-01")
    @Tag("FR-008-02")
    void sans_jeton_les_points_et_les_chemins_voisins_rendent_401() throws Exception {
        for (String chemin : new String[] {"/api/documents/telechargement", "/api/documents/telechargement?type=RIB&compteId=1",
                "/api/documents/inconnu", "/actuator/info"}) {
            HttpResponse<String> reponse = envoyer(port, "GET", chemin, null, Map.of());

            assertThat(reponse.statusCode()).as(chemin).isEqualTo(401);
            assertThat(reponse.body()).as(chemin).isEqualTo(AUTHENTIFICATION_REQUISE);
            assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        }
    }

    @Test
    @Tag("FR-017-01")
    void un_jeton_expire_mal_signe_ou_illisible_rend_401() throws Exception {
        Instant maintenant = Instant.now();
        String expire = OutilsHttp.jeton(Conteneurs.CLES, "1", "CLIENT", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = OutilsHttp.jeton(generateur.generateKeyPair(), "1", "CLIENT", maintenant,
                maintenant.plusSeconds(1800));
        long avant = totalDesCompteurs();

        for (String jeton : new String[] {expire, malSigne, "illisible", OutilsHttp.jetonValide("1") + "x"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET",
                    "/api/documents/telechargement?type=RIB&compteId=1", jeton);

            assertThat(reponse.statusCode()).isEqualTo(401);
            assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        }
        assertThat(totalDesCompteurs()).isEqualTo(avant);
    }

    @Test
    @Tag("FR-017-01")
    void un_sujet_qui_n_est_pas_un_identifiant_numerique_rend_401() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/documents/telechargement?type=RIB&compteId=1",
                OutilsHttp.jetonValide("user1"));

        assertThat(reponse.statusCode()).isEqualTo(401);
        assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
    }

    @Test
    void la_sante_est_publique() throws Exception {
        HttpResponse<String> reponse = envoyer(port, "GET", "/actuator/health", null, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    @Tag("FR-017-08")
    @Tag("FR-008-04")
    void le_compte_d_un_autre_client_rend_403_et_un_compte_inexistant_404_quel_que_soit_le_role() throws Exception {
        for (String role : new String[] {"CLIENT", "CONSEILLER", "ADMIN"}) {
            String jeton = OutilsHttp.jetonValide("1", role);

            HttpResponse<String> autrui = avecJeton(port, "GET", "/api/documents/telechargement?type=RIB&compteId=3", jeton);
            assertThat(autrui.statusCode()).as(role).isEqualTo(403);
            assertThat(autrui.body()).as(role).isEqualTo("{\"message\":\"Accès refusé\"}");
            assertThat(autrui.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                    assertThat(type).startsWith("application/json"));

            HttpResponse<String> inconnu = avecJeton(port, "GET",
                    "/api/documents/telechargement?type=RIB&compteId=999999", jeton);
            assertThat(inconnu.statusCode()).as(role).isEqualTo(404);
            assertThat(inconnu.body()).as(role).isEqualTo("{\"message\":\"Compte introuvable\"}");
        }
    }

    @Test
    @Tag("FR-030-06")
    void client_conseiller_et_administrateur_telechargent_chacun_les_documents_de_leurs_propres_comptes()
            throws Exception {
        Object[][] titulaires = {{"1", "CLIENT", 1}, {"11", "CONSEILLER", 11}, {"10", "ADMIN", 10}};

        for (Object[] titulaire : titulaires) {
            String jeton = OutilsHttp.jetonValide((String) titulaire[0], (String) titulaire[1]);
            for (String type : new String[] {"RIB", "ATTESTATION"}) {
                var reponse = OutilsHttp.octetsAvecJeton(port,
                        "/api/documents/telechargement?type=" + type + "&compteId=" + titulaire[2], jeton);

                assertThat(reponse.statusCode()).as(titulaire[1] + " " + type).isEqualTo(200);
                assertThat(reponse.headers().firstValue("Content-Type")).contains("application/pdf");
                assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
            }
        }
    }
}
