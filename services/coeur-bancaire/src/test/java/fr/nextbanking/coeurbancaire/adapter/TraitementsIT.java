package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Journal des traitements nocturnes sur une vraie base : lecture ouverte à tout utilisateur authentifié.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-011-09")
class TraitementsIT {

    private static final String CHEMIN = "/api/coeur-bancaire/traitements";
    private static final String REQUETE_INVALIDE = "{\"message\":\"Requête invalide\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void viderLeJournal() {
        jdbc.execute("truncate batch_job_logs restart identity");
    }

    @Test
    void rend_au_plus_50_executions_de_la_plus_recente_a_la_plus_ancienne() throws Exception {
        jdbc.update("insert into batch_job_logs (job_name, start_time, end_time, records_processed, status)"
                + " select 'job-' || n, timestamptz '2026-10-01 01:00:00+02' + n * interval '1 day',"
                + " timestamptz '2026-10-01 01:00:02+02' + n * interval '1 day', n, 'SUCCESS'"
                + " from generate_series(1, 60) n");

        for (String requete : new String[] {CHEMIN + "?limite=50", CHEMIN, CHEMIN + "?limite=500"}) {
            JsonNode corps = json.readTree(appeler(requete, "CLIENT").body());
            assertThat(corps).as(requete).hasSize(50);
            assertThat(corps.get(0).get("job").asString()).isEqualTo("job-60");
            assertThat(corps.get(49).get("job").asString()).isEqualTo("job-11");
        }
        assertThat(json.readTree(appeler(CHEMIN + "?limite=3", "ADMIN").body())).hasSize(3);
    }

    @Test
    void rend_les_champs_du_journal_avec_le_decalage_de_paris() throws Exception {
        jdbc.update("insert into batch_job_logs (job_name, start_time, end_time, records_processed, status, details)"
                + " values ('calcul-interets', timestamptz '2026-10-05 23:00:00+00',"
                + " timestamptz '2026-10-05 23:00:01.500+00', 42, 'PARTIAL', 'deux comptes ignores')");

        JsonNode ligne = json.readTree(appeler(CHEMIN, "CONSEILLER").body()).get(0);

        assertThat(ligne.get("job").asString()).isEqualTo("calcul-interets");
        assertThat(ligne.get("debut").asString()).isEqualTo("2026-10-06T01:00+02:00");
        assertThat(ligne.get("fin").asString()).isEqualTo("2026-10-06T01:00:01.500+02:00");
        assertThat(ligne.get("dureeMs").asLong()).isEqualTo(1500);
        assertThat(ligne.get("enregistrements").asInt()).isEqualTo(42);
        assertThat(ligne.get("statut").asString()).isEqualTo("PARTIAL");
        assertThat(ligne.get("details").asString()).isEqualTo("deux comptes ignores");
    }

    @Test
    void une_execution_sans_debut_passe_en_dernier_avec_une_duree_nulle() throws Exception {
        jdbc.update("insert into batch_job_logs (job_name, start_time, status) values"
                + " ('ancien', timestamptz '2026-01-01 01:00:00+01', 'FAILED')");
        jdbc.update("insert into batch_job_logs (job_name, end_time) values ('sans-debut', now())");

        JsonNode corps = json.readTree(appeler(CHEMIN, "CLIENT").body());

        assertThat(corps.get(0).get("job").asString()).isEqualTo("ancien");
        assertThat(corps.get(0).get("fin").isNull()).isTrue();
        assertThat(corps.get(0).get("dureeMs").asLong()).isZero();
        assertThat(corps.get(1).get("job").asString()).isEqualTo("sans-debut");
        assertThat(corps.get(1).get("debut").isNull()).isTrue();
        assertThat(corps.get(1).get("dureeMs").asLong()).isZero();
        assertThat(corps.get(1).get("statut").isNull()).isTrue();
        assertThat(corps.get(1).get("enregistrements").isNull()).isTrue();
    }

    @Test
    void un_journal_vide_rend_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = appeler(CHEMIN, "CLIENT");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    void une_limite_nulle_negative_ou_non_numerique_rend_400() throws Exception {
        for (String limite : new String[] {"0", "-1", "abc", "1.5", "99999999999999999999"}) {
            HttpResponse<String> reponse = appeler(CHEMIN + "?limite=" + limite, "CLIENT");
            assertThat(reponse.statusCode()).as(limite).isEqualTo(400);
            assertThat(reponse.body()).as(limite).isEqualTo(REQUETE_INVALIDE);
        }
    }

    @Test
    void les_trois_roles_voient_le_meme_journal() throws Exception {
        jdbc.update("insert into batch_job_logs (job_name, start_time, status) values"
                + " ('job', timestamptz '2026-10-05 01:00:00+02', 'SUCCESS')");

        String client = appeler(CHEMIN, "CLIENT").body();

        assertThat(appeler(CHEMIN, "CONSEILLER").body()).isEqualTo(client);
        assertThat(appeler(CHEMIN, "ADMIN").body()).isEqualTo(client);
    }

    @Test
    void la_base_refuse_un_statut_hors_des_trois_valeurs_du_legacy() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                "insert into batch_job_logs (job_name, status) values ('x', 'RUNNING')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private HttpResponse<String> appeler(String chemin, String role) throws Exception {
        Instant maintenant = Instant.now();
        return Jetons.envoyer(port, "GET", chemin,
                Jetons.jeton(Conteneurs.CLES, role, maintenant, maintenant.plusSeconds(1800)), null);
    }
}
