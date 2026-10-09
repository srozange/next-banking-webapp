package fr.nextbanking.credits.adapter;

import static fr.nextbanking.credits.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.credits.adapter.OutilsHttp.publier;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
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
 * Échéancier, paiement d'une échéance et suppression d'un prêt par HTTP, sur une vraie base : propriétaire seul,
 * textes propres des refus, transition REMBOURSE, cascade, aucun événement dans l'outbox.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class EcheancesHttpIT {

    private static final String PAIEMENT_REFUSE =
            "{\"message\":\"Erreur : l'échéance n'a pas été marquée comme payée. Veuillez réessayer.\"}";
    private static final String SUPPRESSION_REFUSEE =
            "{\"message\":\"Erreur suppression : le prêt n'a pas été supprimé. Veuillez réessayer.\"}";

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
    private final String user1 = OutilsHttp.jetonValide("1");
    private final String user2 = OutilsHttp.jetonValide("2");

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
        jdbc.sql("""
                insert into loans (id, customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (30, 2, 'CONSOMMATION', 1200, 0, 12, 100, 1200, 0, 'x', 'EN_ATTENTE', date '2026-10-01',
                        date '2027-10-01')""").update();
    }

    private HttpResponse<String> payer(String jeton, long echeanceId) throws Exception {
        return publier(port, "/api/credits/echeances/" + echeanceId + "/paiement", "", jeton);
    }

    private long compter(String requete) {
        return jdbc.sql(requete).query(Long.class).single();
    }

    @Test
    @Tag("FR-007-11")
    void le_proprietaire_lit_les_echeances_triees_au_format_du_back() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/credits/prets/1/echeances", user1);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        JsonNode echeances = json.readTree(reponse.body());
        assertThat(echeances).hasSize(12);
        assertThat(echeances.get(0).toString()).isEqualTo("{\"id\":1,\"numero\":1,\"dateEcheance\":\"2025-02-01\","
                + "\"capital\":\"233.69\",\"interets\":\"35.00\",\"total\":\"268.69\",\"capitalRestant\":\"11766.31\","
                + "\"paye\":true,\"datePaiement\":\"2025-02-01T09:00:00Z\"}");
        assertThat(echeances.get(10).get("paye").asBoolean()).isFalse();
        assertThat(echeances.get(10).get("datePaiement").isNull()).isTrue();
        for (int i = 0; i < 12; i++) {
            assertThat(echeances.get(i).get("numero").asInt()).isEqualTo(i + 1);
        }
    }

    @Test
    @Tag("FR-007-11")
    void un_pret_en_attente_a_un_echeancier_vide_un_pret_d_autrui_est_refuse_un_inconnu_introuvable() throws Exception {
        HttpResponse<String> enAttente = avecJeton(port, "GET", "/api/credits/prets/30/echeances", user2);
        assertThat(enAttente.statusCode()).isEqualTo(200);
        assertThat(enAttente.body()).isEqualTo("[]");

        HttpResponse<String> autrui = avecJeton(port, "GET", "/api/credits/prets/1/echeances", user2);
        assertThat(autrui.statusCode()).isEqualTo(403);
        assertThat(autrui.body()).isEqualTo("{\"message\":\"Accès refusé\"}");

        HttpResponse<String> inconnu = avecJeton(port, "GET", "/api/credits/prets/999/echeances", user1);
        assertThat(inconnu.statusCode()).isEqualTo(404);
        assertThat(inconnu.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");

        assertThat(avecJeton(port, "GET", "/api/credits/prets/1/echeances", "faux").statusCode()).isEqualTo(401);
    }

    @Test
    @Tag("FR-007-12")
    void le_paiement_marque_l_echeance_a_la_date_courante_sans_evenement_ni_changement_de_statut() throws Exception {
        Instant avant = Instant.now();
        HttpResponse<String> reponse = payer(user1, 11);

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode echeance = json.readTree(reponse.body());
        assertThat(echeance.get("id").asLong()).isEqualTo(11);
        assertThat(echeance.get("paye").asBoolean()).isTrue();
        Instant paiement = Instant.parse(echeance.get("datePaiement").asString());
        assertThat(paiement).isBetween(avant.minusSeconds(5), Instant.now().plusSeconds(5));
        assertThat(compter("select count(*) from loan_repayments where loan_id = 1 and not paye")).isEqualTo(1);
        assertThat(jdbc.sql("select status from loans where id = 1").query(String.class).single())
                .isEqualTo("EN_COURS");
        assertThat(compter("select count(*) from outbox")).isZero();
    }

    @Test
    @Tag("FR-007-12")
    void payer_deux_fois_ne_change_pas_la_date_et_ne_donne_aucune_erreur() throws Exception {
        String premiere = json.readTree(payer(user1, 11).body()).get("datePaiement").asString();
        Thread.sleep(Duration.ofMillis(50));

        HttpResponse<String> seconde = payer(user1, 11);

        assertThat(seconde.statusCode()).isEqualTo(200);
        assertThat(json.readTree(seconde.body()).get("datePaiement").asString()).isEqualTo(premiere);
        assertThat(payer(user1, 1).statusCode()).isEqualTo(200);
        assertThat(jdbc.sql("select date_paiement from loan_repayments where id = 1")
                .query(java.time.OffsetDateTime.class).single().toInstant())
                .isEqualTo(Instant.parse("2025-02-01T09:00:00Z"));
    }

    @Test
    @Tag("FR-007-12")
    void la_derniere_echeance_payee_passe_le_pret_rembourse() throws Exception {
        for (int id = 15; id <= 17; id++) {
            assertThat(payer(user1, id).statusCode()).isEqualTo(200);
            assertThat(jdbc.sql("select status from loans where id = 2").query(String.class).single())
                    .isEqualTo("EN_COURS");
        }

        assertThat(payer(user1, 18).statusCode()).isEqualTo(200);

        assertThat(jdbc.sql("select status from loans where id = 2").query(String.class).single())
                .isEqualTo("REMBOURSE");
        assertThat(compter("select count(*) from outbox")).isZero();
    }

    @Test
    @Tag("FR-007-12")
    void le_paiement_d_un_autre_client_est_refuse_avec_son_texte_et_ne_change_rien() throws Exception {
        HttpResponse<String> reponse = payer(user2, 11);

        assertThat(reponse.statusCode()).isEqualTo(403);
        assertThat(reponse.body()).isEqualTo(PAIEMENT_REFUSE);
        assertThat(compter("select count(*) from loan_repayments where id = 11 and not paye")).isEqualTo(1);

        HttpResponse<String> inconnue = payer(user1, 999);
        assertThat(inconnue.statusCode()).isEqualTo(404);
        assertThat(inconnue.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
    }

    @Test
    @Tag("FR-007-13")
    void la_suppression_retire_le_pret_et_ses_echeances_sans_evenement() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "DELETE", "/api/credits/prets/1", user1);

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(reponse.body()).isEmpty();
        assertThat(compter("select count(*) from loans where id = 1")).isZero();
        assertThat(compter("select count(*) from loan_repayments where loan_id = 1")).isZero();
        assertThat(compter("select count(*) from loans where id = 2")).isEqualTo(1);
        assertThat(compter("select count(*) from outbox")).isZero();
    }

    @Test
    @Tag("FR-007-13")
    void un_pret_en_attente_se_supprime_aussi() throws Exception {
        assertThat(avecJeton(port, "DELETE", "/api/credits/prets/30", user2).statusCode()).isEqualTo(204);
        assertThat(compter("select count(*) from loans where id = 30")).isZero();
    }

    @Test
    @Tag("FR-007-13")
    void la_suppression_d_un_pret_d_autrui_est_refusee_avec_son_texte_un_pret_inconnu_est_introuvable()
            throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "DELETE", "/api/credits/prets/1", user2);

        assertThat(reponse.statusCode()).isEqualTo(403);
        assertThat(reponse.body()).isEqualTo(SUPPRESSION_REFUSEE);
        assertThat(compter("select count(*) from loans where id = 1")).isEqualTo(1);

        HttpResponse<String> inconnu = avecJeton(port, "DELETE", "/api/credits/prets/999", user1);
        assertThat(inconnu.statusCode()).isEqualTo(404);
        assertThat(inconnu.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
    }
}
