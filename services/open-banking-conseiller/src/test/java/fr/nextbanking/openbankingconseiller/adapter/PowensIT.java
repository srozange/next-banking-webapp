package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.PowensSimule;
import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.PowensSimule.ModeErreur;
import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** API Powens : connexion simulée (200 / 503), état initial, 401, état en base, journaux sans jeton ni secret. */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class PowensIT {

    private static final String CONNEXION = "/api/open-banking-conseiller/powens/connexion";
    private static final String ETAT = "/api/open-banking-conseiller/powens";
    private static final String MESSAGE_503 = "{\"message\":\"Impossible d initialiser la connexion Powens.\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Powens powens;

    @Autowired
    private EmetteurState emetteur;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void remettreAZero() {
        jdbc.execute("truncate etat_powens, states_powens");
        ((PowensSimule) powens).mode(ModeErreur.AUCUNE);
    }

    private HttpResponse<String> appeler(String methode, String chemin, String role) throws Exception {
        Instant maintenant = Instant.now();
        String jeton = role == null ? null
                : Jetons.jeton(Conteneurs.CLES, role, maintenant, maintenant.plusSeconds(1800));
        return Jetons.envoyer(port, methode, chemin, jeton, null);
    }

    @Test
    @Tag("FR-012-05")
    @Tag("FR-028-01")
    @Tag("FR-028-02")
    void la_connexion_simulee_rend_l_adresse_du_retour_et_enregistre_jeton_et_state() throws Exception {
        for (String role : List.of("CLIENT", "CONSEILLER", "ADMIN")) {
            HttpResponse<String> reponse = appeler("POST", CONNEXION, role);

            assertThat(reponse.statusCode()).as(role).isEqualTo(200);
            URI adresse = URI.create(json.readTree(reponse.body()).get("adresse").asString());
            assertThat(adresse.getPath()).isEqualTo("/api/open-banking-conseiller/powens/retour");
            Map<String, String> parametres = parametres(adresse);
            assertThat(parametres).containsEntry("connection_id", "1");
            StatePowens state = emetteur.verifier(parametres.get("state")).orElseThrow();
            assertThat(state.sub()).isEqualTo("10");
            assertThat(state.estExpire(Instant.now())).isFalse();
            assertThat(jdbc.queryForObject("select count(*) from states_powens where nonce = ? and sub = '10'"
                    + " and not utilise", Integer.class, state.nonce())).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("select jeton_permanent from etat_powens where sub = '10'", String.class))
                .isNotBlank();
        assertThat(jdbc.queryForObject("select connecte from etat_powens where sub = '10'", Boolean.class)).isFalse();
    }

    @Test
    @Tag("FR-028-01")
    void un_refus_de_creation_puis_une_reponse_sans_jeton_rendent_503_sans_adresse_et_sans_etat() throws Exception {
        for (ModeErreur mode : new ModeErreur[] {ModeErreur.REFUS_CREATION, ModeErreur.REPONSE_SANS_JETON}) {
            ((PowensSimule) powens).mode(mode);

            HttpResponse<String> reponse = appeler("POST", CONNEXION, "CLIENT");

            assertThat(reponse.statusCode()).as(mode.name()).isEqualTo(503);
            assertThat(reponse.body()).isEqualTo(MESSAGE_503);
        }
        assertThat(jdbc.queryForObject("select count(*) from etat_powens", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from states_powens", Integer.class)).isZero();
        JsonNode etat = json.readTree(appeler("GET", ETAT, "CLIENT").body());
        assertThat(etat.get("connecte").asBoolean()).isFalse();
    }

    @Test
    @Tag("FR-012-17")
    void l_etat_initial_est_non_connecte_sans_compte_ni_message() throws Exception {
        for (String role : List.of("CLIENT", "CONSEILLER", "ADMIN")) {
            HttpResponse<String> reponse = appeler("GET", ETAT, role);

            assertThat(reponse.statusCode()).as(role).isEqualTo(200);
            assertThat(reponse.body()).isEqualTo(
                    "{\"connecte\":false,\"comptes\":[],\"transactions\":[],\"message\":null}");
        }
    }

    @Test
    @Tag("FR-012-17")
    void apres_la_connexion_l_etat_reste_non_connecte_et_ne_laisse_voir_aucun_jeton() throws Exception {
        appeler("POST", CONNEXION, "CLIENT");
        String jeton = jdbc.queryForObject("select jeton_permanent from etat_powens", String.class);

        HttpResponse<String> reponse = appeler("GET", ETAT, "CLIENT");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(json.readTree(reponse.body()).get("connecte").asBoolean()).isFalse();
        assertThat(reponse.body()).doesNotContain(jeton).doesNotContain("jeton");
    }

    @Test
    @Tag("FR-012-05")
    @Tag("FR-028-01")
    void les_journaux_de_la_connexion_ne_contiennent_ni_jeton_ni_secret(CapturedOutput sortie) throws Exception {
        HttpResponse<String> reponse = appeler("POST", CONNEXION, "CLIENT");
        String jeton = jdbc.queryForObject("select jeton_permanent from etat_powens", String.class);
        ((PowensSimule) powens).mode(ModeErreur.REFUS_CREATION);
        appeler("POST", CONNEXION, "CLIENT");

        assertThat(reponse.body()).doesNotContain(jeton);
        assertThat(sortie.getAll()).doesNotContain(jeton).doesNotContain("POWENS_CLIENT_SECRET=");
    }

    private static Map<String, String> parametres(URI adresse) {
        Map<String, String> parametres = new java.util.HashMap<>();
        for (String paire : adresse.getRawQuery().split("&")) {
            String[] morceaux = paire.split("=", 2);
            parametres.put(morceaux[0], URLDecoder.decode(morceaux[1], StandardCharsets.UTF_8));
        }
        return parametres;
    }
}
