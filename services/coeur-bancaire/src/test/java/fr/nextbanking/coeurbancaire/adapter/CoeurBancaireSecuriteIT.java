package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Contrôle d'accès de coeur-bancaire : tout est authentifié sauf {@code POST /api/coeur-bancaire/temoin} et
 * {@code GET /actuator/health} ; sans jeton valide (absent, mal signé, expiré, illisible), 401 « Authentification
 * requise » en JSON, sans pile, redirection ni cookie ; avec un jeton valide, une route inconnue rend 404
 * « Ressource introuvable ».
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-011-09")
@Tag("FR-030-01")
class CoeurBancaireSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";
    private static final String RESSOURCE_INTROUVABLE = "{\"message\":\"Ressource introuvable\"}";
    private static final List<String> CHEMINS = List.of("/api/coeur-bancaire/traitements",
            "/api/coeur-bancaire/traitements?limite=50", "/api/coeur-bancaire/tableau-de-bord",
            "/api/coeur-bancaire/beneficiaires", "/api/coeur-bancaire/virements-programmes",
            "/api/coeur-bancaire/temoin", "/api/coeur-bancaire/temoin/", "/api/coeur-bancaire/inconnu",
            "/actuator/info", "/api/coeur-bancaire/conseiller/portefeuille",
            "/api/coeur-bancaire/administration/alertes-fraude");

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

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @Test
    void sans_jeton_ou_avec_un_jeton_invalide_les_points_proteges_rendent_401() throws Exception {
        Instant maintenant = Instant.now();
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = Jetons.jeton(generateur.generateKeyPair(), "ADMIN", maintenant, maintenant.plusSeconds(1800));
        String expire = Jetons.jeton(Conteneurs.CLES, "ADMIN", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));

        for (String chemin : CHEMINS) {
            for (String jeton : new String[] {null, malSigne, expire, "illisible"}) {
                HttpResponse<String> reponse = Jetons.envoyer(port, "GET", chemin, jeton, null);
                assertThat(reponse.statusCode()).as(chemin).isEqualTo(401);
                assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
                assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                        assertThat(type).startsWith("application/json"));
                assertThat(reponse.headers().firstValue("Location")).isEmpty();
                assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
            }
        }
    }

    @Test
    @Tag("FR-013-04")
    void sans_jeton_ou_avec_un_jeton_invalide_les_deux_posts_des_beneficiaires_rendent_401() throws Exception {
        Instant maintenant = Instant.now();
        String expire = Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));

        for (String chemin : new String[] {"/api/coeur-bancaire/beneficiaires",
                "/api/coeur-bancaire/beneficiaires/verification-iban"}) {
            for (String jeton : new String[] {null, expire, "illisible"}) {
                HttpResponse<String> reponse = Jetons.envoyer(port, "POST", chemin, jeton, "{}");
                assertThat(reponse.statusCode()).as(chemin).isEqualTo(401);
                assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
            }
        }
    }

    @Test
    @Tag("FR-004-01")
    void sans_jeton_ou_avec_un_jeton_invalide_la_creation_d_un_virement_programme_rend_401() throws Exception {
        Instant maintenant = Instant.now();
        String expire = Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));

        for (String jeton : new String[] {null, expire, "illisible"}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "POST", "/api/coeur-bancaire/virements-programmes",
                    jeton, "{}");
            assertThat(reponse.statusCode()).isEqualTo(401);
            assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        }
    }

    @Test
    @Tag("FR-004-09")
    @Tag("FR-004-10")
    void sans_jeton_ou_avec_un_jeton_invalide_les_actions_sur_un_virement_programme_rendent_401() throws Exception {
        Instant maintenant = Instant.now();
        String expire = Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));
        String base = "/api/coeur-bancaire/virements-programmes/1";

        for (String jeton : new String[] {null, expire, "illisible"}) {
            for (String[] appel : new String[][] {{"POST", base + "/suspension"}, {"POST", base + "/reactivation"},
                    {"DELETE", base}}) {
                HttpResponse<String> reponse = Jetons.envoyer(port, appel[0], appel[1], jeton, null);
                assertThat(reponse.statusCode()).as(appel[1]).isEqualTo(401);
                assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
            }
        }
    }

    @Test
    void avec_un_jeton_valide_une_route_inconnue_rend_404() throws Exception {
        Instant maintenant = Instant.now();
        String valide = Jetons.jeton(Conteneurs.CLES, "ADMIN", maintenant, maintenant.plusSeconds(1800));

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", "/api/coeur-bancaire/inconnu", valide,
                null);

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(reponse.body()).isEqualTo(RESSOURCE_INTROUVABLE);
    }

    @Test
    @Tag("FR-003-05")
    void le_virement_sans_jeton_rend_401_puis_avec_un_compte_source_d_un_autre_client_403() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
        String corps = "{\"compteSourceId\":3,\"compteDestinataireId\":2,\"montant\":\"100.00\"}";

        HttpResponse<String> sansJeton = Jetons.envoyer(port, "POST", "/api/coeur-bancaire/virements", null, corps);
        HttpResponse<String> autreClient = Jetons.envoyer(port, "POST", "/api/coeur-bancaire/virements",
                Jetons.pour(1, "CLIENT"), corps);

        assertThat(sansJeton.statusCode()).isEqualTo(401);
        assertThat(sansJeton.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        assertThat(autreClient.statusCode()).isEqualTo(403);
        assertThat(autreClient.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
        assertThat(jdbc.sql("select solde from comptes where id = 3").query(BigDecimal.class).single())
                .isEqualByComparingTo("500.00");
        assertThat(jdbc.sql("select count(*) from virements").query(Long.class).single()).isZero();
    }

    @Test
    @Tag("FR-030-07")
    @Tag("FR-015-01")
    void le_portefeuille_rend_401_a_un_sujet_illisible_et_403_a_un_client_meme_avec_un_parametre_force()
            throws Exception {
        Instant maintenant = Instant.now();
        String sujetIllisible = Jetons.signer(Conteneurs.CLES, "abc", "CONSEILLER", maintenant,
                maintenant.plusSeconds(1800));

        HttpResponse<String> illisible = Jetons.envoyer(port, "GET", "/api/coeur-bancaire/conseiller/portefeuille",
                sujetIllisible, null);
        HttpResponse<String> client = Jetons.envoyer(port, "GET",
                "/api/coeur-bancaire/conseiller/portefeuille?conseillerId=11", Jetons.pour(1, "CLIENT"), null);

        assertThat(illisible.statusCode()).isEqualTo(401);
        assertThat(illisible.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        assertThat(client.statusCode()).isEqualTo(403);
        assertThat(client.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
        assertThat(client.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    @Tag("FR-015-02")
    @Tag("FR-030-07")
    void le_conseiller_ne_voit_que_son_portefeuille_meme_avec_un_parametre_force() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
        try (java.sql.Connection connexion = source.getConnection()) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connexion,
                    new org.springframework.core.io.FileSystemResource("reference-data/projection-clients.sql"));
        }
        String sophie = Jetons.pour(11, "CONSEILLER");

        for (String parametre : new String[] {"?conseillerId=12", "?clientId=3"}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET",
                    "/api/coeur-bancaire/conseiller/portefeuille" + parametre, sophie, null);
            assertThat(reponse.statusCode()).as(parametre).isEqualTo(200);
            assertThat(reponse.body()).as(parametre).startsWith("[{\"clientId\":1,")
                    .endsWith("{\"clientId\":2,\"comptes\":[]}]").doesNotContain("\"clientId\":3", "\"clientId\":4");
        }
    }

    @Test
    @Tag("FR-016-14")
    @Tag("FR-016-15")
    @Tag("FR-016-16")
    void les_alertes_de_fraude_rendent_401_sans_jeton_et_403_a_un_conseiller_ou_un_client_sans_rien_changer()
            throws Exception {
        DonneesDeReference.recharger(source, jdbc);
        long id = jdbc.sql("""
                insert into alertes_fraude (client_id, regle, score, description, statut, cree_le)
                values (1, 'LARGE_AMOUNT', 45, 'a', 'OPEN', timestamptz '2026-11-17 09:00:00+00')
                returning id""").query(Long.class).single();
        String base = "/api/coeur-bancaire/administration/alertes-fraude";
        String[][] appels = {{"GET", base}, {"POST", base + "/" + id + "/confirmation"},
                {"POST", base + "/" + id + "/faux-positif"}};

        for (String[] appel : appels) {
            HttpResponse<String> sansJeton = Jetons.envoyer(port, appel[0], appel[1], null, null);
            assertThat(sansJeton.statusCode()).as(appel[1]).isEqualTo(401);
            assertThat(sansJeton.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
            for (String role : new String[] {"CONSEILLER", "CLIENT"}) {
                HttpResponse<String> refuse = Jetons.envoyer(port, appel[0], appel[1], Jetons.pour(11, role), null);
                assertThat(refuse.statusCode()).as(role + " " + appel[1]).isEqualTo(403);
                assertThat(refuse.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
            }
        }
        assertThat(jdbc.sql("select statut || '/' || coalesce(revu_par, '-') || '/' || coalesce(revu_le::text, '-') "
                + "from alertes_fraude where id = :id").param("id", id).query(String.class).single())
                .isEqualTo("OPEN/-/-");
    }

    @Test
    void le_temoin_reste_public() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", "/api/coeur-bancaire/temoin", null,
                "{\"libelle\":\"sans jeton\"}");

        assertThat(reponse.statusCode()).isEqualTo(201);
    }

    @Test
    void la_sante_est_publique() throws Exception {
        assertThat(Jetons.envoyer(port, "GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
    }
}
