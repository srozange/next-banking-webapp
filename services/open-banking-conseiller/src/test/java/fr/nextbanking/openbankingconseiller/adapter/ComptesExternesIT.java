package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.PowensSimule;
import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.PowensSimule.ModeErreur;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
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

/** Lecture, sélection, rafraîchissement et déconnexion des comptes externes (matrice de la story 8.3). */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-028-07")
@Tag("FR-028-08")
@Tag("FR-028-09")
@Tag("FR-028-10")
@Tag("FR-028-11")
@Tag("FR-012-10")
@Tag("FR-014-07")
@Tag("FR-014-08")
@Tag("BUG-042")
@Tag("BUG-043")
@Tag("BUG-089")
class ComptesExternesIT {

    private static final String ETAT = "/api/open-banking-conseiller/powens";
    private static final String CONNEXION = ETAT + "/connexion";
    private static final String RAFRAICHISSEMENT = ETAT + "/rafraichissement";
    private static final String INDISPONIBLE =
            "{\"message\":\"Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants.\"}";
    private static final String INTROUVABLE = "{\"message\":\"Ressource introuvable\"}";

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

    private PowensSimule simule;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void preparer() {
        jdbc.execute("truncate etat_powens, states_powens, comptes_externes, transactions_externes");
        simule = (PowensSimule) powens;
        simule.mode(ModeErreur.AUCUNE);
        jdbc.update("insert into etat_powens (sub, jeton_permanent, connecte, compte_selectionne, message)"
                + " values ('10', 'jeton-secret', true, 101, 'message en cours')");
        compte("10", 101, "Compte courant BNP Paribas", "1250.50");
        compte("10", 102, "Livret Credit Agricole", "3000.00");
        jdbc.update("insert into transactions_externes (sub, compte_id, id, date, libelle, montant)"
                + " values ('10', 101, 1, date '2026-10-01', 'Ancienne', 1.00)");
    }

    private void compte(String sub, long id, String nom, String solde) {
        jdbc.update("insert into comptes_externes (sub, compte_id, nom, type, solde, iban)"
                + " values (?, ?, ?, 'checking', ?::numeric, '')", sub, id, nom, solde);
    }

    private HttpResponse<String> appeler(String methode, String chemin, String sub) throws Exception {
        Instant maintenant = Instant.now();
        String jeton = sub == null ? null
                : Jetons.jeton(Conteneurs.CLES, sub, "CLIENT", maintenant, maintenant.plusSeconds(1800));
        return Jetons.envoyer(port, methode, chemin, jeton, null);
    }

    private String instantane() {
        return jdbc.queryForObject("select (select count(*) from etat_powens) || '|' || coalesce((select string_agg("
                + "sub || compte_selectionne || connecte || coalesce(message, '') || coalesce(jeton_permanent, ''), ',')"
                + " from etat_powens), '') || '|' || (select count(*) from comptes_externes) || '|'"
                + " || (select count(*) from transactions_externes)", String.class);
    }

    @Test
    void la_lecture_rend_l_etat_en_base_sans_appeler_powens() throws Exception {
        int avant = simule.nombreAppels();

        HttpResponse<String> reponse = appeler("GET", ETAT, "10");

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode etat = json.readTree(reponse.body());
        assertThat(etat.get("connecte").asBoolean()).isTrue();
        assertThat(etat.get("compteSelectionne").asLong()).isEqualTo(101);
        assertThat(etat.get("message").asString()).isEqualTo("message en cours");
        assertThat(etat.get("patrimoineTotal").asString()).isEqualTo("4250.50");
        assertThat(etat.get("comptes")).hasSize(2);
        assertThat(etat.get("comptes").get(0).get("solde").asString()).isEqualTo("1250.50");
        assertThat(etat.get("comptes").get(0).get("soldeAbsent").asBoolean()).isFalse();
        assertThat(etat.get("transactions").get(0).get("date").asString()).isEqualTo("2026-10-01");
        assertThat(etat.get("transactions").get(0).get("libelle").asString()).isEqualTo("Ancienne");
        assertThat(etat.get("transactions").get(0).get("montant").asString()).isEqualTo("1.00");
        assertThat(reponse.body()).doesNotContain("jeton-secret").doesNotContain("jeton");
        assertThat(simule.nombreAppels()).isEqualTo(avant);
    }

    @Test
    void un_solde_absent_est_distinct_de_zero_et_exclu_du_total() throws Exception {
        jdbc.execute("delete from comptes_externes");
        jdbc.update("insert into comptes_externes (sub, compte_id, nom, type, solde, iban) values"
                + " ('10', 1, 'A', 'checking', null, null), ('10', 2, 'B', 'checking', null, ''),"
                + " ('10', 3, 'C', 'checking', 0, null), ('10', 4, 'D', 'checking', 120.5, null)");

        JsonNode etat = json.readTree(appeler("GET", ETAT, "10").body());

        JsonNode comptes = etat.get("comptes");
        assertThat(comptes.get(0).get("soldeAbsent").asBoolean()).isTrue();
        assertThat(comptes.get(0).get("solde").isNull()).isTrue();
        assertThat(comptes.get(1).get("soldeAbsent").asBoolean()).isTrue();
        assertThat(comptes.get(1).get("solde").isNull()).isTrue();
        assertThat(comptes.get(2).get("soldeAbsent").asBoolean()).isFalse();
        assertThat(comptes.get(2).get("solde").asString()).isEqualTo("0.00");
        assertThat(comptes.get(3).get("solde").asString()).isEqualTo("120.50");
        assertThat(etat.get("patrimoineTotal").asString()).isEqualTo("120.50");
    }

    @Test
    void la_selection_charge_vingt_transactions_au_plus() throws Exception {
        simule.mode(ModeErreur.TRANSACTIONS_25);

        HttpResponse<String> reponse = appeler("POST", ETAT + "/comptes/102/selection", "10");

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode etat = json.readTree(reponse.body());
        assertThat(etat.get("compteSelectionne").asLong()).isEqualTo(102);
        assertThat(etat.get("transactions")).hasSize(20);
        assertThat(etat.get("message").asString()).isEqualTo("message en cours");
        assertThat(jdbc.queryForObject("select count(*) from transactions_externes where compte_id = 102",
                Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject("select count(*) from transactions_externes where compte_id = 101",
                Integer.class)).isZero();
    }

    @Test
    void la_selection_d_un_compte_inconnu_ou_d_un_autre_client_rend_404_sans_appel_et_sans_changement() throws Exception {
        compte("20", 555, "Autre client", "1.00");
        String avantEtat = instantane();
        int avant = simule.nombreAppels();

        for (String chemin : new String[] {"/comptes/999/selection", "/comptes/555/selection"}) {
            HttpResponse<String> reponse = appeler("POST", ETAT + chemin, "10");

            assertThat(reponse.statusCode()).as(chemin).isEqualTo(404);
            assertThat(reponse.body()).isEqualTo(INTROUVABLE);
        }
        assertThat(appeler("POST", ETAT + "/comptes/abc/selection", "10").statusCode()).isEqualTo(400);
        assertThat(simule.nombreAppels()).isEqualTo(avant);
        assertThat(instantane()).isEqualTo(avantEtat);
    }

    @Test
    void la_selection_en_panne_rend_503_et_laisse_l_etat_inchange() throws Exception {
        String avantEtat = instantane();

        for (ModeErreur mode : new ModeErreur[] {ModeErreur.TRANSACTIONS_404, ModeErreur.TRANSACTIONS_500}) {
            simule.mode(mode);

            HttpResponse<String> reponse = appeler("POST", ETAT + "/comptes/102/selection", "10");

            assertThat(reponse.statusCode()).as(mode.name()).isEqualTo(503);
            assertThat(reponse.body()).isEqualTo(INDISPONIBLE);
        }
        assertThat(instantane()).isEqualTo(avantEtat);
        assertThat(jdbc.queryForObject("select compte_selectionne from etat_powens", Long.class)).isEqualTo(101L);
    }

    @Test
    void le_rafraichissement_garde_la_selection_relit_les_comptes_et_efface_le_message() throws Exception {
        jdbc.update("update etat_powens set compte_selectionne = 102");

        HttpResponse<String> reponse = appeler("POST", RAFRAICHISSEMENT, "10");

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode etat = json.readTree(reponse.body());
        assertThat(etat.get("compteSelectionne").asLong()).isEqualTo(102);
        assertThat(etat.get("message").isNull()).isTrue();
        assertThat(etat.get("comptes")).hasSize(2);
        assertThat(etat.get("transactions")).hasSize(2);
        assertThat(etat.get("patrimoineTotal").asString()).isEqualTo("4250.50");
    }

    @Test
    void le_rafraichissement_prend_le_premier_compte_si_la_selection_a_disparu() throws Exception {
        jdbc.update("update etat_powens set compte_selectionne = 999");
        simule.mode(ModeErreur.COMPTE_SANS_SOLDE);

        JsonNode etat = json.readTree(appeler("POST", RAFRAICHISSEMENT, "10").body());

        assertThat(etat.get("compteSelectionne").asLong()).isEqualTo(101);
        assertThat(etat.get("comptes")).hasSize(1);
        assertThat(etat.get("comptes").get(0).get("soldeAbsent").asBoolean()).isTrue();
        assertThat(etat.get("patrimoineTotal").asString()).isEqualTo("0.00");
    }

    @Test
    void le_rafraichissement_en_panne_rend_503_et_laisse_l_etat_inchange() throws Exception {
        String avantEtat = instantane();

        for (ModeErreur mode : new ModeErreur[] {ModeErreur.COMPTES_500, ModeErreur.COMPTES_503,
                ModeErreur.TRANSACTIONS_500}) {
            simule.mode(mode);

            HttpResponse<String> reponse = appeler("POST", RAFRAICHISSEMENT, "10");

            assertThat(reponse.statusCode()).as(mode.name()).isEqualTo(503);
            assertThat(reponse.body()).isEqualTo(INDISPONIBLE);
        }
        assertThat(instantane()).isEqualTo(avantEtat);
    }

    @Test
    void le_rafraichissement_sans_connexion_rend_400_sans_appel_powens() throws Exception {
        jdbc.update("update etat_powens set jeton_permanent = null");
        int avant = simule.nombreAppels();

        HttpResponse<String> reponse = appeler("POST", RAFRAICHISSEMENT, "10");

        assertThat(reponse.statusCode()).isEqualTo(400);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Requête invalide\"}");
        assertThat(appeler("POST", RAFRAICHISSEMENT, "30").statusCode()).isEqualTo(400);
        assertThat(simule.nombreAppels()).isEqualTo(avant);
    }

    @Test
    void la_deconnexion_rend_204_et_remet_tout_a_zero_sans_toucher_un_autre_client() throws Exception {
        compte("20", 555, "Autre client", "1.00");
        jdbc.update("insert into etat_powens (sub, jeton_permanent, connecte) values ('20', 'autre', true)");

        HttpResponse<String> reponse = appeler("DELETE", CONNEXION, "10");

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(reponse.body()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from comptes_externes where sub = '10'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from transactions_externes where sub = '10'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("select jeton_permanent from etat_powens where sub = '10'", String.class))
                .isNull();
        JsonNode etat = json.readTree(appeler("GET", ETAT, "10").body());
        assertThat(etat.get("connecte").asBoolean()).isFalse();
        assertThat(etat.get("comptes")).isEmpty();
        assertThat(etat.get("message").isNull()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from comptes_externes where sub = '20'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void sans_jwt_tout_rend_401_et_les_etats_restent_inchanges() throws Exception {
        String avantEtat = instantane();
        int avant = simule.nombreAppels();

        for (String[] requete : new String[][] {{"POST", ETAT + "/comptes/102/selection"},
                {"POST", RAFRAICHISSEMENT}, {"DELETE", CONNEXION}, {"GET", ETAT}}) {
            HttpResponse<String> reponse = appeler(requete[0], requete[1], null);

            assertThat(reponse.statusCode()).as(requete[0] + " " + requete[1]).isEqualTo(401);
            assertThat(reponse.body()).isEqualTo("{\"message\":\"Authentification requise\"}");
        }
        assertThat(instantane()).isEqualTo(avantEtat);
        assertThat(simule.nombreAppels()).isEqualTo(avant);
    }
}
