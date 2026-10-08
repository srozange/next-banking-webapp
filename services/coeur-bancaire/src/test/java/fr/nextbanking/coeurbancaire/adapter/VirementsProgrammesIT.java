package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.CreerVirementProgrammeCasUsage;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
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
 * Virements programmés de bout en bout sur une vraie base : création (SC-004-02), validations dans l'ordre du legacy,
 * compte source inexistant ou d'autrui, destinataire inexistant (BUG-015), liste filtrée et triée, authentification.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class VirementsProgrammesIT {

    private static final String CHEMIN = "/api/coeur-bancaire/virements-programmes";

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

    private final JsonMapper json = JsonMapper.builder().build();

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    private String corps(Object compte, String numero, String libelle, Object montant, String frequence,
            String motif, String date) {
        var noeud = json.createObjectNode();
        if (compte != null) {
            noeud.put("compteSourceId", Long.parseLong(compte.toString()));
        }
        return noeud.put("numeroBeneficiaire", numero).put("libelleBeneficiaire", libelle)
                .put("montant", montant == null ? null : montant.toString()).put("frequence", frequence)
                .put("motif", motif).put("datePremiereExecution", date).toString();
    }

    private String nominal() {
        return corps(1, "FR76-0001-0002", "[retro] test virprog", "50", "MENSUEL", "[retro] virement programm gm",
                "2027-01-01");
    }

    private long compter() {
        return jdbc.sql("select count(*) from virements_programmes").query(Long.class).single();
    }

    @Test
    @Tag("FR-004-01")
    void sans_jeton_la_liste_et_la_creation_rendent_401() throws Exception {
        HttpResponse<String> liste = Jetons.envoyer(port, "GET", CHEMIN, null, null);
        HttpResponse<String> creation = Jetons.envoyer(port, "POST", CHEMIN, null, nominal());

        assertThat(liste.statusCode()).isEqualTo(401);
        assertThat(creation.statusCode()).isEqualTo(401);
        assertThat(compter()).isZero();
    }

    @Test
    @Tag("FR-004-07")
    void la_creation_nominale_rend_201_et_enregistre_un_virement_actif() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, Jetons.pour(1, "CLIENT"), nominal());

        assertThat(reponse.statusCode()).isEqualTo(201);
        JsonNode cree = json.readTree(reponse.body());
        assertThat(cree.get("montant").asString()).isEqualTo("50.00");
        assertThat(cree.get("frequence").asString()).isEqualTo("MENSUEL");
        assertThat(cree.get("numeroCompteSource").asString()).isEqualTo("FR76-0001-0001");
        assertThat(cree.get("prochaineExecution").asString()).isEqualTo("2027-01-01");
        assertThat(cree.get("actif").asBoolean()).isTrue();
        assertThat(cree.get("nbExecutions").asInt()).isZero();
        var ligne = jdbc.sql("""
                select client_id, compte_source_id, numero_beneficiaire, libelle_beneficiaire, montant, frequence,
                    motif, actif, nb_executions, prochaine_execution = timestamptz '2027-01-01 00:00 Europe/Paris' as minuit,
                    cree_le > now() - interval '5 minutes' as recent
                from virements_programmes where id = :id""")
                .param("id", cree.get("id").asLong()).query().singleRow();
        assertThat(ligne.get("client_id")).isEqualTo(1L);
        assertThat(ligne.get("compte_source_id")).isEqualTo(1L);
        assertThat(ligne.get("numero_beneficiaire")).isEqualTo("FR76-0001-0002");
        assertThat(ligne.get("libelle_beneficiaire")).isEqualTo("[retro] test virprog");
        assertThat(ligne.get("motif")).isEqualTo("[retro] virement programm gm");
        assertThat(ligne.get("actif")).isEqualTo(true);
        assertThat(ligne.get("nb_executions")).isEqualTo(0);
        assertThat(ligne.get("minuit")).isEqualTo(true);
        assertThat(ligne.get("recent")).isEqualTo(true);
    }

    @Test
    @Tag("FR-004-07")
    void une_date_passee_est_acceptee() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, Jetons.pour(1, "CLIENT"),
                corps(1, "FR76-0001-0002", "Epargne", "5", "HEBDOMADAIRE", null, "2020-01-01"));

        assertThat(reponse.statusCode()).isEqualTo(201);
        assertThat(json.readTree(reponse.body()).get("prochaineExecution").asString()).isEqualTo("2020-01-01");
        assertThat(json.readTree(reponse.body()).get("motif").isNull()).isTrue();
    }

    @Test
    @Tag("FR-004-04")
    @Tag("FR-004-05")
    void les_saisies_invalides_rendent_400_avec_le_message_exact_sans_rien_creer() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");

        refus(jeton, "{}", 400, "Veuillez selectionner un compte source", "compteSourceId");
        refus(jeton, corps(1, "  ", "l", "5", "MENSUEL", null, "2027-01-01"), 400,
                "Veuillez saisir le numero de compte destinataire", "numeroBeneficiaire");
        refus(jeton, corps(1, "FR76-0001-0002", " ", "5", "MENSUEL", null, "2027-01-01"), 400,
                "Veuillez saisir le libelle du beneficiaire", "libelleBeneficiaire");
        for (String montant : new String[] {"", "0", "-5", "abc"}) {
            refus(jeton, corps(1, "FR76-0001-0002", "l", montant, "MENSUEL", null, "2027-01-01"), 400,
                    "Montant invalide", "montant");
        }
        refus(jeton, corps(1, "FR76-0001-0002", "l", "5", "MENSUEL", null, ""), 400,
                "Veuillez saisir la date de premiere execution", "datePremiereExecution");
        refus(jeton, corps(1, "FR76-0001-0002", "l", "5", "MENSUEL", null, "01/01/2027"), 400,
                "Format de date invalide (attendu : yyyy-MM-dd)", "datePremiereExecution");
        assertThat(compter()).isZero();
    }

    @Test
    @Tag("FR-004-06")
    void le_compte_source_inexistant_ou_d_autrui_rend_404_ou_403() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");

        refus(jeton, corps(99, "FR76-0001-0002", "l", "5", "MENSUEL", null, "2027-01-01"), 404,
                "Erreur : Client ou compte introuvable", "compteSourceId");
        refus(jeton, corps(3, "FR76-0001-0002", "l", "5", "MENSUEL", null, "2027-01-01"), 403,
                "Erreur : Compte non autorise", "compteSourceId");
        assertThat(compter()).isZero();
    }

    @Test
    @Tag("FR-004-13")
    @Tag("BUG-015")
    void un_destinataire_inexistant_rend_404_sans_creation() throws Exception {
        refus(Jetons.pour(1, "CLIENT"), corps(1, "FR76-9999-9999", "l", "5", "MENSUEL", null, "2027-01-01"), 404,
                "Compte destinataire introuvable", "numeroBeneficiaire");
        assertThat(compter()).isZero();
    }

    @Test
    @Tag("FR-004-06")
    void les_depassements_de_longueur_et_les_frequences_inconnues_rendent_400_sans_creation() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");

        for (String corps : new String[] {
                corps(1, "n".repeat(35), "l", "5", "MENSUEL", null, "2027-01-01"),
                corps(1, "FR76-0001-0002", "l".repeat(101), "5", "MENSUEL", null, "2027-01-01"),
                corps(1, "FR76-0001-0002", "l", "5", "MENSUEL", "m".repeat(256), "2027-01-01"),
                corps(1, "FR76-0001-0002", "l", "5", "QUOTIDIEN", null, "2027-01-01")}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, jeton, corps);
            assertThat(reponse.statusCode()).isEqualTo(400);
            assertThat(json.readTree(reponse.body()).get("message").asString()).isEqualTo("Requête invalide");
        }
        HttpResponse<String> illisible = Jetons.envoyer(port, "POST", CHEMIN, jeton, "{\"compteSourceId\":\"x\"}");
        assertThat(illisible.statusCode()).isEqualTo(400);
        assertThat(compter()).isZero();
    }

    @Test
    @Tag("FR-004-03")
    void la_liste_ne_rend_que_les_virements_du_client_tries_par_prochaine_execution() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");
        Jetons.envoyer(port, "POST", CHEMIN, jeton,
                corps(1, "FR76-0001-0002", "Tard", "5", "ANNUEL", null, "2027-06-01"));
        Jetons.envoyer(port, "POST", CHEMIN, jeton,
                corps(2, "FR76-0001-0001", "Tot", "7.5", "SEMESTRIEL", "motif", "2027-01-01"));
        Jetons.envoyer(port, "POST", CHEMIN, Jetons.pour(8, "CLIENT"),
                corps(3, "FR76-0001-0001", "Autre", "5", "MENSUEL", null, "2027-01-01"));

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, jeton, null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode liste = json.readTree(reponse.body());
        assertThat(liste).hasSize(2);
        assertThat(liste.get(0).get("libelleBeneficiaire").asString()).isEqualTo("Tot");
        assertThat(liste.get(0).get("numeroCompteSource").asString()).isEqualTo("FR76-0001-0002");
        assertThat(liste.get(0).get("montant").asString()).isEqualTo("7.50");
        assertThat(liste.get(0).get("motif").asString()).isEqualTo("motif");
        assertThat(liste.get(1).get("libelleBeneficiaire").asString()).isEqualTo("Tard");
        assertThat(reponse.body()).doesNotContain("Autre");
    }

    @Test
    @Tag("FR-004-03")
    void un_client_sans_virement_programme_a_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(10, "CLIENT"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    @Tag("FR-004-07")
    void le_journal_de_creation_ne_contient_pas_le_numero_du_beneficiaire() throws Exception {
        List<String> lignes = new ArrayList<>();
        Handler capteur = new Handler() {
            @Override
            public void publish(LogRecord ligne) {
                lignes.add(ligne.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger journal = Logger.getLogger(CreerVirementProgrammeCasUsage.class.getName());
        journal.addHandler(capteur);
        try {
            Jetons.envoyer(port, "POST", CHEMIN, Jetons.pour(1, "CLIENT"), nominal());
        } finally {
            journal.removeHandler(capteur);
        }

        assertThat(lignes).hasSize(1);
        assertThat(String.join("\n", lignes)).doesNotContain("FR76-0001-0002");
    }

    private void refus(String jeton, String corps, int statut, String message, String champ) throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, jeton, corps);
        assertThat(reponse.statusCode()).isEqualTo(statut);
        JsonNode erreur = json.readTree(reponse.body());
        assertThat(erreur.get("message").asString()).isEqualTo(message);
        assertThat(erreur.get("champ").asString()).isEqualTo(champ);
    }
}
