package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.AjouterBeneficiaireCasUsage;
import fr.nextbanking.coeurbancaire.application.SupprimerBeneficiaireCasUsage;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Bénéficiaires externes de bout en bout sur une vraie base : liste du client du jeton (actifs, triés, IBAN masqué,
 * zone SEPA), suppression logique, bénéficiaire d'autrui (BUG-047), identifiants inconnus, authentification.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class BeneficiairesIT {

    private static final String CHEMIN = "/api/coeur-bancaire/beneficiaires";
    private static final String IBAN = "FR3530004000000123456789010";
    private static final String ERREUR = "{\"message\":\"Erreur lors de la suppression.\"}";

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

    @Test
    @Tag("FR-013-01")
    void sans_jeton_la_liste_et_la_suppression_rendent_401() throws Exception {
        HttpResponse<String> liste = Jetons.envoyer(port, "GET", CHEMIN, null, null);
        HttpResponse<String> suppression = Jetons.envoyer(port, "DELETE", CHEMIN + "/1", null, null);

        assertThat(liste.statusCode()).isEqualTo(401);
        assertThat(liste.body()).isEqualTo("{\"message\":\"Authentification requise\"}");
        assertThat(suppression.statusCode()).isEqualTo(401);
        assertThat(actif(1)).isTrue();
    }

    @Test
    @Tag("FR-013-02")
    void la_liste_ne_rend_que_les_beneficiaires_actifs_du_client_tries_par_libelle() throws Exception {
        long zoe = ajouter(1, "Zoe", IBAN, null, null, true);
        ajouter(1, "Ancien", IBAN, null, null, false);
        ajouter(8, "Autre client", IBAN, null, null, true);
        ajouter(1, "Alice", "US12345678901234", "ABCDUS33", "Bank of Test", true);

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(1, "CLIENT"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode liste = json.readTree(reponse.body());
        // Les 3 bénéficiaires de user1 dans l'import, plus Alice et Zoe.
        assertThat(liste).hasSize(5);
        assertThat(liste.get(0).get("libelle").asString()).isEqualTo("Alice");
        assertThat(liste.get(1).get("libelle").asString()).isEqualTo("Electricite EDF");
        assertThat(liste.get(2).get("libelle").asString()).isEqualTo("Loyer Paris 14e");
        assertThat(liste.get(3).get("libelle").asString()).isEqualTo("Mon compte Boursorama");
        assertThat(liste.get(4).get("libelle").asString()).isEqualTo("Zoe");
        assertThat(liste.get(4).get("id").asLong()).isEqualTo(zoe);
        assertThat(reponse.body()).doesNotContain("Autre client").doesNotContain("Ancien");
    }

    @Test
    @Tag("FR-013-03")
    void l_iban_est_masque_et_la_zone_sepa_calculee_sans_bic_ni_banque() throws Exception {
        ajouter(1, "Zoe", IBAN, null, null, true);
        ajouter(1, "Alice", "US12345678901234", "ABCDUS33", "Bank of Test", true);

        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(1, "CLIENT"), null);

        JsonNode liste = json.readTree(reponse.body());
        JsonNode alice = liste.get(0);
        assertThat(alice.get("iban").asString()).isEqualTo("US12***1234");
        assertThat(alice.get("zoneSepa").asBoolean()).isFalse();
        assertThat(alice.get("bic").asString()).isEqualTo("ABCDUS33");
        assertThat(alice.get("banque").asString()).isEqualTo("Bank of Test");
        JsonNode zoe = liste.get(4);
        assertThat(zoe.get("iban").asString()).isEqualTo("FR35***9010");
        assertThat(zoe.get("zoneSepa").asBoolean()).isTrue();
        assertThat(zoe.get("bic").isNull()).isTrue();
        assertThat(zoe.get("banque").isNull()).isTrue();
        assertThat(reponse.body()).doesNotContain(IBAN);
    }

    @Test
    @Tag("FR-013-02")
    void un_client_sans_beneficiaire_a_une_liste_vide() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(10, "CLIENT"), null);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    @Tag("FR-013-09")
    void le_proprietaire_desactive_son_beneficiaire_sans_perdre_la_ligne() throws Exception {
        long id = ajouter(1, "Zoe", IBAN, null, null, true);
        String jeton = Jetons.pour(1, "CLIENT");

        HttpResponse<String> suppression = Jetons.envoyer(port, "DELETE", CHEMIN + "/" + id, jeton, null);
        HttpResponse<String> liste = Jetons.envoyer(port, "GET", CHEMIN, jeton, null);

        assertThat(suppression.statusCode()).isEqualTo(204);
        assertThat(suppression.body()).isEmpty();
        assertThat(actif(id)).isFalse();
        assertThat(jdbc.sql("select count(*) from beneficiaires_externes where id = :id").param("id", id)
                .query(Long.class).single()).isEqualTo(1L);
        assertThat(json.readTree(liste.body())).hasSize(3);
    }

    @Test
    @Tag("FR-013-10")
    @Tag("BUG-047")
    void le_beneficiaire_d_un_autre_client_rend_403_et_reste_actif() throws Exception {
        long id = ajouter(8, "Autre client", IBAN, null, null, true);

        HttpResponse<String> reponse = Jetons.envoyer(port, "DELETE", CHEMIN + "/" + id,
                Jetons.pour(1, "CLIENT"), null);

        assertThat(reponse.statusCode()).isEqualTo(403);
        assertThat(reponse.body()).isEqualTo(ERREUR);
        assertThat(actif(id)).isTrue();
        HttpResponse<String> chezLeProprietaire = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(8, "CLIENT"), null);
        assertThat(json.readTree(chezLeProprietaire.body())).hasSize(1);
    }

    @Test
    @Tag("FR-013-10")
    void un_identifiant_inexistant_ou_deja_desactive_rend_404() throws Exception {
        long inactif = ajouter(1, "Ancien", IBAN, null, null, false);
        String jeton = Jetons.pour(1, "CLIENT");

        HttpResponse<String> inexistant = Jetons.envoyer(port, "DELETE", CHEMIN + "/999", jeton, null);
        HttpResponse<String> desactive = Jetons.envoyer(port, "DELETE", CHEMIN + "/" + inactif, jeton, null);
        HttpResponse<String> illisible = Jetons.envoyer(port, "DELETE", CHEMIN + "/abc", jeton, null);

        assertThat(inexistant.statusCode()).isEqualTo(404);
        assertThat(inexistant.body()).isEqualTo(ERREUR);
        assertThat(desactive.statusCode()).isEqualTo(404);
        assertThat(desactive.body()).isEqualTo(ERREUR);
        assertThat(illisible.statusCode()).isEqualTo(400);
    }

    @Test
    @Tag("FR-013-09")
    @Tag("BUG-086")
    void la_suppression_ne_laisse_aucune_ligne_de_journal_avec_l_iban_en_clair() throws Exception {
        long id = ajouter(1, "Zoe", IBAN, null, null, true);
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
        Logger journal = Logger.getLogger(SupprimerBeneficiaireCasUsage.class.getName());
        journal.addHandler(capteur);
        try {
            Jetons.envoyer(port, "DELETE", CHEMIN + "/" + id, Jetons.pour(1, "CLIENT"), null);
        } finally {
            journal.removeHandler(capteur);
        }

        assertThat(String.join("\n", lignes)).contains("FR35****9010").doesNotContain(IBAN);
    }

    private static final String VERIFICATION = CHEMIN + "/verification-iban";
    private static final String IBAN_FAUX = "FR3530004000000123456789011";

    private String corpsAjout(String libelle, String titulaire, String iban, String bic, String banque) {
        return json.createObjectNode().put("libelle", libelle).put("titulaire", titulaire).put("iban", iban)
                .put("bic", bic).put("banque", banque).toString();
    }

    private long compter() {
        return jdbc.sql("select count(*) from beneficiaires_externes").query(Long.class).single();
    }

    @Test
    @Tag("FR-013-04")
    void sans_jeton_la_verification_et_l_ajout_rendent_401() throws Exception {
        HttpResponse<String> verification = Jetons.envoyer(port, "POST", VERIFICATION, null, "{\"iban\":\"\"}");
        HttpResponse<String> ajout = Jetons.envoyer(port, "POST", CHEMIN, null,
                corpsAjout("a", "b", IBAN, null, null));

        assertThat(verification.statusCode()).isEqualTo(401);
        assertThat(ajout.statusCode()).isEqualTo(401);
        assertThat(compter()).isEqualTo(5L);
    }

    @Test
    @Tag("FR-013-04")
    void la_verification_rend_un_statut_structure_sans_texte() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");

        assertThat(verifier(jeton, "{}")).isEqualTo("{\"statut\":\"vide\",\"codePays\":null,\"zoneSepa\":false}");
        assertThat(verifier(jeton, "{\"iban\":\"  \"}")).contains("\"statut\":\"vide\"");
        assertThat(verifier(jeton, "{\"iban\":\"" + IBAN_FAUX + "\"}")).contains("\"statut\":\"invalide\"");
        assertThat(verifier(jeton, "{\"iban\":\"" + IBAN + "\"}"))
                .isEqualTo("{\"statut\":\"valide\",\"codePays\":\"FR\",\"zoneSepa\":true}");
        assertThat(verifier(jeton, "{\"iban\":\"TR330006100519786457841326\"}"))
                .isEqualTo("{\"statut\":\"valide\",\"codePays\":\"TR\",\"zoneSepa\":false}");
        assertThat(compter()).isEqualTo(5L);
    }

    private String verifier(String jeton, String corps) throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", VERIFICATION, jeton, corps);
        assertThat(reponse.statusCode()).isEqualTo(200);
        return reponse.body();
    }

    @Test
    @Tag("FR-013-07")
    @Tag("FR-013-08")
    void l_ajout_nominal_normalise_et_enregistre_un_beneficiaire_actif() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");

        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, jeton, corpsAjout(
                "[retro] test beneficiaire", "Retro Test SA", " fr35 3000 4000 0001 2345 6789 010 ", " hsbcfrppxxx ",
                "   "));

        assertThat(reponse.statusCode()).isEqualTo(201);
        JsonNode cree = json.readTree(reponse.body());
        assertThat(cree.get("iban").asString()).isEqualTo("FR35***9010");
        assertThat(cree.get("bic").asString()).isEqualTo("HSBCFRPPXXX");
        assertThat(cree.get("banque").isNull()).isTrue();
        assertThat(cree.get("zoneSepa").asBoolean()).isTrue();
        assertThat(reponse.body()).doesNotContain(IBAN);
        var ligne = jdbc.sql("select client_id, iban, bic, banque, zone_sepa, actif, "
                + "(cree_le at time zone 'Europe/Paris')::date as jour from beneficiaires_externes where id = :id")
                .param("id", cree.get("id").asLong()).query().singleRow();
        assertThat(ligne.get("client_id")).isEqualTo(1L);
        assertThat(ligne.get("iban")).isEqualTo(IBAN);
        assertThat(ligne.get("bic")).isEqualTo("HSBCFRPPXXX");
        assertThat(ligne.get("banque")).isNull();
        assertThat(ligne.get("zone_sepa")).isEqualTo(true);
        assertThat(ligne.get("actif")).isEqualTo(true);
        assertThat(ligne.get("jour").toString()).isEqualTo(LocalDate.now(ZoneId.of("Europe/Paris")).toString());
        HttpResponse<String> liste = Jetons.envoyer(port, "GET", CHEMIN, jeton, null);
        assertThat(json.readTree(liste.body())).hasSize(4);
    }

    @Test
    @Tag("FR-013-07")
    void un_iban_hors_sepa_est_enregistre_hors_zone() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, Jetons.pour(1, "CLIENT"),
                corpsAjout("Istanbul", "Titulaire", "TR330006100519786457841326", "ABCDTR2A", "Banque"));

        assertThat(reponse.statusCode()).isEqualTo(201);
        assertThat(json.readTree(reponse.body()).get("zoneSepa").asBoolean()).isFalse();
    }

    @Test
    @Tag("FR-013-05")
    void les_saisies_refusees_rendent_400_avec_le_texte_et_le_champ_sans_rien_enregistrer() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");

        refuse(jeton, corpsAjout("", "b", IBAN, null, null), "Le libellé est obligatoire.", "libelle");
        refuse(jeton, corpsAjout("a", "b", "", null, null), "L'IBAN est obligatoire.", "iban");
        refuse(jeton, corpsAjout("a", "b", IBAN_FAUX, null, null),
                "IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97).", "iban");
        refuse(jeton, corpsAjout("a", "", IBAN, null, null), "Le nom du titulaire est obligatoire.", "titulaire");
        refuse(jeton, corpsAjout("a", "b", IBAN, "HSBCFR", null),
                "BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques.", "bic");
        refuse(jeton, "{}", "Le libellé est obligatoire.", "libelle");
        assertThat(compter()).isEqualTo(5L);
    }

    private void refuse(String jeton, String corps, String message, String champ) throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, jeton, corps);
        assertThat(reponse.statusCode()).isEqualTo(400);
        JsonNode erreur = json.readTree(reponse.body());
        assertThat(erreur.get("message").asString()).isEqualTo(message);
        assertThat(erreur.get("champ").asString()).isEqualTo(champ);
    }

    @Test
    @Tag("FR-013-06")
    void cent_un_caracteres_rendent_400_sans_enregistrement() throws Exception {
        String jeton = Jetons.pour(1, "CLIENT");
        String long101 = "x".repeat(101);

        for (String corps : new String[] {corpsAjout(long101, "b", IBAN, null, null),
                corpsAjout("a", long101, IBAN, null, null), corpsAjout("a", "b", IBAN, null, long101)}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, jeton, corps);
            assertThat(reponse.statusCode()).isEqualTo(400);
            assertThat(json.readTree(reponse.body()).get("message").asString()).isEqualTo("Requête invalide");
        }
        HttpResponse<String> limite = Jetons.envoyer(port, "POST", CHEMIN, jeton,
                corpsAjout("x".repeat(100), "b", IBAN, null, "y".repeat(100)));
        assertThat(limite.statusCode()).isEqualTo(201);
        assertThat(compter()).isEqualTo(6L);
    }

    @Test
    @Tag("FR-013-05")
    void un_corps_illisible_rend_400() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", CHEMIN, Jetons.pour(1, "CLIENT"), "pas du json");

        assertThat(reponse.statusCode()).isEqualTo(400);
        assertThat(compter()).isEqualTo(5L);
    }

    @Test
    @Tag("FR-013-07")
    @Tag("BUG-086")
    void l_ajout_ne_laisse_aucune_ligne_de_journal_avec_l_iban_en_clair() throws Exception {
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
        Logger journal = Logger.getLogger(AjouterBeneficiaireCasUsage.class.getName());
        journal.addHandler(capteur);
        try {
            Jetons.envoyer(port, "POST", CHEMIN, Jetons.pour(1, "CLIENT"), corpsAjout("a", "b", IBAN, null, null));
        } finally {
            journal.removeHandler(capteur);
        }

        assertThat(lignes).hasSize(1);
        assertThat(String.join("\n", lignes)).contains("FR35****9010").doesNotContain(IBAN);
    }

    private long ajouter(long clientId, String libelle, String iban, String bic, String banque, boolean actif) {
        return jdbc.sql("""
                insert into beneficiaires_externes (client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif,
                    cree_le)
                values (:client, 'Titulaire', :iban, :libelle, :banque, :bic, true, :actif, '2026-10-01T08:00:00Z')
                returning id""")
                .param("client", clientId).param("iban", iban).param("libelle", libelle).param("banque", banque)
                .param("bic", bic).param("actif", actif).query(Long.class).single();
    }

    private boolean actif(long id) {
        return jdbc.sql("select actif from beneficiaires_externes where id = :id").param("id", id)
                .query(Boolean.class).single();
    }
}
