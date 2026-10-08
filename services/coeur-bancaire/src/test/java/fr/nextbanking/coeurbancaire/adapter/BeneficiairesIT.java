package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.SupprimerBeneficiaireCasUsage;
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
        assertThat(liste).hasSize(3);
        assertThat(liste.get(0).get("libelle").asString()).isEqualTo("Alice");
        assertThat(liste.get(1).get("libelle").asString()).isEqualTo("Loyer");
        assertThat(liste.get(2).get("libelle").asString()).isEqualTo("Zoe");
        assertThat(liste.get(2).get("id").asLong()).isEqualTo(zoe);
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
        JsonNode zoe = liste.get(2);
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
        assertThat(json.readTree(liste.body())).hasSize(1);
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
