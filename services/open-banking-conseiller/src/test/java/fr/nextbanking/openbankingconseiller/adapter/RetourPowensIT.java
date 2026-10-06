package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.PowensSimule;
import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.PowensSimule.ModeErreur;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Retour public de Powens (SC-014-01) : 302 sans corps, state signé à usage unique, effets en base. */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-014-01")
@Tag("FR-014-02")
@Tag("FR-014-03")
@Tag("FR-014-04")
@Tag("FR-014-05")
@Tag("FR-014-06")
@Tag("FR-014-07")
@Tag("FR-014-08")
@Tag("FR-012-02")
@Tag("FR-012-06")
@Tag("FR-012-07")
@Tag("FR-012-08")
@Tag("FR-012-09")
@Tag("FR-028-03")
@Tag("FR-028-04")
@Tag("FR-028-05")
@Tag("FR-028-06")
@Tag("BUG-040")
@Tag("BUG-044")
@Tag("BUG-089")
class RetourPowensIT {

    private static final String RETOUR = "/api/open-banking-conseiller/powens/retour";
    private static final String CLIENT = "10";
    private static final String INDISPONIBLE =
            "Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants.";

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

    @Autowired
    private DepotEtatPowens depot;

    private PowensSimule simule;

    @BeforeEach
    void remettreAZero() {
        jdbc.execute("truncate etat_powens, states_powens, comptes_externes, transactions_externes");
        simule = (PowensSimule) powens;
        simule.mode(ModeErreur.AUCUNE);
        jdbc.update("insert into etat_powens (sub, jeton_permanent) values (?, 'jeton-secret')", CLIENT);
    }

    private String stateValide() {
        return state(Instant.now().plusSeconds(600), true);
    }

    private String state(Instant expiration, boolean enregistre) {
        StatePowens state = new StatePowens(CLIENT, UUID.randomUUID().toString().replace("-", ""), expiration);
        if (enregistre) {
            depot.enregistrerState(state);
        }
        return emetteur.emettre(state);
    }

    private HttpResponse<String> retour(String requete) throws Exception {
        return Jetons.envoyer(port, "GET", RETOUR + requete, null, null);
    }

    private static String encoder(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8);
    }

    private Map<String, Object> etat() {
        return jdbc.queryForMap("select jeton_permanent, connecte, message, compte_selectionne from etat_powens"
                + " where sub = ?", CLIENT);
    }

    private int compter(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private void verifierRedirection(HttpResponse<String> reponse) {
        assertThat(reponse.statusCode()).isEqualTo(302);
        assertThat(reponse.headers().firstValue("Location")).contains("/comptes-tiers");
        assertThat(reponse.body()).isEmpty();
    }

    @Test
    void sans_parametre_et_sans_jeton_le_retour_redirige_sans_corps_ni_appel_powens() throws Exception {
        int avant = simule.nombreAppels();

        verifierRedirection(retour(""));

        assertThat(simule.nombreAppels()).isEqualTo(avant);
        assertThat(etat().get("message")).isNull();
    }

    @Test
    void un_state_absent_mal_signe_expire_ou_rejoue_n_a_aucun_effet_ni_appel_powens() throws Exception {
        int avant = simule.nombreAppels();
        String valide = stateValide();
        String malSigne = valide.substring(0, valide.length() - 2) + "AA";
        String expire = state(Instant.now().minusSeconds(5), true);
        String nonEnregistre = state(Instant.now().plusSeconds(600), false);

        for (String state : new String[] {null, "", malSigne, expire, nonEnregistre}) {
            verifierRedirection(retour("?connection_id=1" + (state == null ? "" : "&state=" + encoder(state))));
        }
        assertThat(etat().get("connecte")).isEqualTo(false);
        assertThat(etat().get("message")).isNull();
        assertThat(compter("comptes_externes")).isZero();

        verifierRedirection(retour("?error=boom&state=" + encoder(valide)));
        String messageApres = (String) etat().get("message");
        assertThat(messageApres).contains("boom");
        verifierRedirection(retour("?connection_id=1&state=" + encoder(valide)));
        assertThat(etat().get("message")).isEqualTo(messageApres);
        assertThat(simule.nombreAppels()).isEqualTo(avant);
    }

    @Test
    void error_remet_a_zero_puis_pose_le_message_d_echec() throws Exception {
        jdbc.update("update etat_powens set connecte = true, compte_selectionne = 101 where sub = ?", CLIENT);
        jdbc.update("insert into comptes_externes (sub, compte_id, nom) values (?, 101, 'x')", CLIENT);

        verifierRedirection(retour("?error=user_cancelled&connection_id=1&code=abc&state=" + encoder(stateValide())));

        Map<String, Object> etat = etat();
        assertThat(etat.get("jeton_permanent")).isNull();
        assertThat(etat.get("connecte")).isEqualTo(false);
        assertThat(etat.get("compte_selectionne")).isNull();
        assertThat(etat.get("message")).isEqualTo("La connexion a votre banque n a pas abouti (user_cancelled). "
                + "Veuillez recommencer la connexion.");
        assertThat(compter("comptes_externes")).isZero();
        Instant maintenant = Instant.now();
        HttpResponse<String> lecture = Jetons.envoyer(port, "GET", "/api/open-banking-conseiller/powens",
                Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant, maintenant.plusSeconds(1800)), null);
        assertThat(lecture.body()).contains("\"connecte\":false").contains("user_cancelled");
    }

    @Test
    void connection_id_nominal_lit_les_comptes_et_vingt_transactions_sur_vingt_cinq() throws Exception {
        simule.mode(ModeErreur.TRANSACTIONS_25);

        verifierRedirection(retour("?connection_id=1&state=" + encoder(stateValide())));

        Map<String, Object> etat = etat();
        assertThat(etat.get("connecte")).isEqualTo(true);
        assertThat(etat.get("compte_selectionne")).isEqualTo(101L);
        assertThat(etat.get("message")).isNull();
        assertThat(etat.get("jeton_permanent")).isEqualTo("jeton-secret");
        assertThat(compter("comptes_externes")).isEqualTo(2);
        assertThat(compter("transactions_externes")).isEqualTo(20);
    }

    @Test
    void aucun_compte_est_connecte_avec_son_message() throws Exception {
        simule.mode(ModeErreur.AUCUN_COMPTE);

        verifierRedirection(retour("?connection_id=1&state=" + encoder(stateValide())));

        assertThat(etat().get("connecte")).isEqualTo(true);
        assertThat(etat().get("message")).isEqualTo(
                "Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.");
        assertThat(compter("comptes_externes")).isZero();
    }

    @Test
    void une_panne_powens_503_puis_500_reste_non_connecte_avec_le_message_d_indisponibilite() throws Exception {
        for (ModeErreur mode : List.of(ModeErreur.COMPTES_503, ModeErreur.COMPTES_500)) {
            simule.mode(mode);

            verifierRedirection(retour("?connection_id=1&state=" + encoder(stateValide())));

            assertThat(etat().get("connecte")).as(mode.name()).isEqualTo(false);
            assertThat(etat().get("message")).as(mode.name()).isEqualTo(INDISPONIBLE);
            assertThat(compter("comptes_externes")).isZero();
        }
    }

    @Test
    void un_jeton_efface_donne_session_expiree_sans_appel_powens() throws Exception {
        jdbc.update("update etat_powens set jeton_permanent = null where sub = ?", CLIENT);
        int avant = simule.nombreAppels();

        verifierRedirection(retour("?connection_id=1&state=" + encoder(stateValide())));

        assertThat(etat().get("message")).isEqualTo("Session expiree. Veuillez recommencer la connexion.");
        assertThat(etat().get("connecte")).isEqualTo(false);
        assertThat(simule.nombreAppels()).isEqualTo(avant);
    }

    @Test
    void code_seul_et_parametres_vides_donnent_un_retour_incomplet() throws Exception {
        verifierRedirection(retour("?code=abc&state=" + encoder(stateValide())));
        assertThat(etat().get("message")).isEqualTo("Callback Powens incomplet. Parametres recus: [code]");

        verifierRedirection(retour("?state=" + encoder(stateValide())));
        assertThat(etat().get("message")).isEqualTo("Callback Powens incomplet. Parametres recus: []");

        verifierRedirection(retour("?error=&connection_id=&code=&state=" + encoder(stateValide())));
        assertThat(etat().get("message")).isEqualTo("Callback Powens incomplet. Parametres recus: []");
        assertThat(etat().get("connecte")).isEqualTo(false);
    }
}
