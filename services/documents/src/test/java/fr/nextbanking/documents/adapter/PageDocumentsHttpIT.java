package fr.nextbanking.documents.adapter;

import static fr.nextbanking.documents.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.documents.adapter.OutilsHttp.envoyer;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.infrastructure.ApplicationDocuments;
import fr.nextbanking.documents.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** {@code GET /api/documents/comptes} : comptes du seul connecté (tout rôle) et {@code aujourdhui} de l'Horloge. */
@SpringBootTest(classes = {ApplicationDocuments.class, ConfigurationHorlogeFixe.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class PageDocumentsHttpIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
        OutilsHttp.insererCompte(base, 20, 1, "FR76-0001-0003", "EPARGNE", "10.00");
        OutilsHttp.insererCompte(base, 3, 2, "FR76-0002-0001", "COURANT", "500.00");
        OutilsHttp.insererCompte(base, 11, 11, "FR76-0011-0001", "COURANT", "110.00");
        OutilsHttp.insererCompte(base, 10, 10, "FR76-0010-0001", "COURANT", "100.00");
    }

    @Test
    @Tag("FR-008-03")
    @Tag("FR-008-05")
    void les_comptes_du_jeton_tries_par_identifiant_et_la_date_du_jour_de_l_horloge() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/documents/comptes?clientId=2",
                OutilsHttp.jetonValide("1"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode corps = json.readTree(reponse.body());
        assertThat(corps.get("aujourdhui").asString()).isEqualTo("2026-10-10");
        assertThat(corps.get("comptes")).hasSize(3);
        assertThat(corps.get("comptes").get(0).get("compteId").asLong()).isEqualTo(1L);
        assertThat(corps.get("comptes").get(0).get("type").asString()).isEqualTo("COURANT");
        assertThat(corps.get("comptes").get(1).get("compteId").asLong()).isEqualTo(2L);
        assertThat(corps.get("comptes").get(2).get("compteId").asLong()).isEqualTo(20L);
        assertThat(corps.get("comptes").get(2).get("numero").asString()).isEqualTo("FR76-0001-0003");
        assertThat(corps.get("comptes").get(2).get("type").asString()).isEqualTo("EPARGNE");
    }

    @Test
    @Tag("FR-008-06")
    void conseiller_et_administrateur_ne_voient_que_leurs_comptes() throws Exception {
        Object[][] titulaires = {{"11", "CONSEILLER", 11}, {"10", "ADMIN", 10}};

        for (Object[] titulaire : titulaires) {
            HttpResponse<String> reponse = avecJeton(port, "GET", "/api/documents/comptes",
                    OutilsHttp.jetonValide((String) titulaire[0], (String) titulaire[1]));

            assertThat(reponse.statusCode()).as(titulaire[1].toString()).isEqualTo(200);
            JsonNode comptes = json.readTree(reponse.body()).get("comptes");
            assertThat(comptes).hasSize(1);
            assertThat(comptes.get(0).get("compteId").asLong()).isEqualTo(((Integer) titulaire[2]).longValue());
        }
    }

    @Test
    @Tag("FR-008-06")
    void un_client_sans_compte_obtient_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/documents/comptes", OutilsHttp.jetonValide("777"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(json.readTree(reponse.body()).get("comptes")).isEmpty();
    }

    @Test
    @Tag("FR-008-06")
    void sans_jeton_ou_avec_un_sujet_non_numerique_la_reponse_est_401() throws Exception {
        HttpResponse<String> sansJeton = envoyer(port, "GET", "/api/documents/comptes", null, Map.of());
        HttpResponse<String> sujet = avecJeton(port, "GET", "/api/documents/comptes", OutilsHttp.jetonValide("user1"));

        assertThat(sansJeton.statusCode()).isEqualTo(401);
        assertThat(sansJeton.body()).isEqualTo("{\"message\":\"Authentification requise\"}");
        assertThat(sujet.statusCode()).isEqualTo(401);
    }
}
