package fr.nextbanking.credits.adapter;

import static fr.nextbanking.credits.adapter.OutilsHttp.avecJeton;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
 * Routes d'administration des prêts sur une vraie base et un vrai Kafka : pipeline, historique, approbation, refus,
 * rôle ADMIN exigé, prêt déjà décidé ignoré, erreur sans détail avec audit conservé, IP d'origine, aucun événement
 * {@code credits.pret-decide.v1}.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class AdministrationPretsHttpIT {

    private static final String PIPELINE = "/api/credits/administration/prets/en-attente";
    private static final String HISTORIQUE = "/api/credits/administration/prets";
    private static final String INDICATEURS = "/api/credits/administration/indicateurs";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Les prêts restent EN_ATTENTE : ni scoring ni consommation des clients pendant ces tests.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
        // Le client local est un mandataire de confiance : X-Forwarded-For est alors retenu.
        registre.add("nb.reseau.mandataires-de-confiance", () -> "127.0.0.0/8,::1/128");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    private final JsonMapper json = JsonMapper.builder().build();
    private final String admin = OutilsHttp.jetonValide("10", "ADMIN");

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
        jdbc.sql("""
                insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime,
                                     version_agregat)
                values (1, 'user1', 'Jean Dupont', 'CLIENT', true, 11, date '2026-10-06', false, 1)""").update();
    }

    @AfterEach
    void nettoyer() {
        jdbc.sql("drop function if exists echec_voulu() cascade").update();
    }

    private long pretEnAttente(long clientId, String debut) {
        return jdbc.sql("""
                insert into loans (customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (:client, 'CONSOMMATION', 10000, 3.5, 36, 293.02, 10548.72, 548.72, 'Voiture', 'EN_ATTENTE',
                        cast(:debut as date), cast(:debut as date) + interval '36 months') returning id""")
                .param("client", clientId).param("debut", debut).query(Long.class).single();
    }

    /** Prêt à identifiant unique dans l'exécution : les topics Kafka de test gardent les messages des autres tests. */
    private long pretEnAttenteUnique() {
        long pretId = 800_000L + (System.nanoTime() % 100_000L);
        jdbc.sql("""
                insert into loans (id, customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (:id, 1, 'CONSOMMATION', 10000, 3.5, 36, 293.02, 10548.72, 548.72, 'Voiture', 'EN_ATTENTE',
                        date '2026-10-01', date '2029-10-01')""").param("id", pretId).update();
        return pretId;
    }

    private HttpResponse<String> poster(String chemin, String jeton, Map<String, String> entetes) throws Exception {
        Map<String, String> tous = new java.util.HashMap<>(entetes);
        tous.put("Authorization", "Bearer " + jeton);
        return OutilsHttp.envoyer(port, "POST", chemin, null, tous);
    }

    private String statut(long pretId) {
        return jdbc.sql("select status from loans where id = :id").param("id", pretId).query(String.class).single();
    }

    private long compter(String requete) {
        return jdbc.sql(requete).query(Long.class).single();
    }

    @Test
    @Tag("FR-016-03")
    void le_pipeline_liste_les_prets_en_attente_avec_le_nom_complet_ou_l_identifiant_en_repli() throws Exception {
        long recent = pretEnAttente(1L, "2026-10-01");
        long ancien = pretEnAttente(2L, "2026-09-01");

        HttpResponse<String> reponse = avecJeton(port, "GET", PIPELINE, admin);

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode lignes = json.readTree(reponse.body());
        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).get("id").asLong()).isEqualTo(recent);
        assertThat(lignes.get(0).get("clientId").asLong()).isEqualTo(1L);
        assertThat(lignes.get(0).get("nomClient").asString()).isEqualTo("Jean Dupont");
        assertThat(lignes.get(0).get("type").asString()).isEqualTo("CONSOMMATION");
        assertThat(lignes.get(0).get("montant").asString()).isEqualTo("10000.00");
        assertThat(lignes.get(0).get("statut").asString()).isEqualTo("EN_ATTENTE");
        assertThat(lignes.get(0).get("dureeMois").asInt()).isEqualTo(36);
        assertThat(lignes.get(0).get("tauxAnnuel").asString()).isEqualTo("3.5000");
        assertThat(lignes.get(0).get("mensualite").asString()).isEqualTo("293.02");
        assertThat(lignes.get(0).get("objet").asString()).isEqualTo("Voiture");
        assertThat(lignes.get(0).get("dateDebut").asString()).isEqualTo("2026-10-01");
        assertThat(lignes.get(1).get("id").asLong()).isEqualTo(ancien);
        assertThat(lignes.get(1).get("nomClient").asString()).isEqualTo("2");
    }

    @Test
    @Tag("FR-016-03")
    void un_pret_sans_type_est_rendu_avec_un_type_nul() throws Exception {
        long pretId = pretEnAttente(1L, "2026-10-01");
        jdbc.sql("update loans set type = null where id = :id").param("id", pretId).update();

        JsonNode lignes = json.readTree(avecJeton(port, "GET", PIPELINE, admin).body());

        assertThat(lignes.get(0).get("type").isNull()).isTrue();
    }

    @Test
    @Tag("FR-016-03")
    void sans_pret_en_attente_le_pipeline_est_vide() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", PIPELINE, admin);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("[]");
    }

    @Test
    @Tag("FR-016-02")
    void l_indicateur_compte_et_cumule_les_seuls_prets_en_attente_et_ignore_les_parametres() throws Exception {
        long petit = pretEnAttente(1L, "2026-10-01");
        jdbc.sql("update loans set montant = 5000 where id = :id").param("id", petit).update();
        pretEnAttente(2L, "2026-09-01");
        long refuse = pretEnAttente(1L, "2026-08-01");
        jdbc.sql("update loans set status = 'REFUSE' where id = :id").param("id", refuse).update();

        HttpResponse<String> reponse = avecJeton(port, "GET", INDICATEURS + "?statut=EN_COURS", admin);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("{\"pretsEnAttente\":2,\"montantEnAttente\":\"15000.00\"}");
    }

    @Test
    @Tag("FR-016-02")
    void sans_pret_en_attente_l_indicateur_vaut_zero() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", INDICATEURS, admin);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("{\"pretsEnAttente\":0,\"montantEnAttente\":\"0.00\"}");
    }

    @Test
    @Tag("FR-016-12")
    void l_historique_rend_tous_les_prets_tous_statuts_du_plus_recent_au_plus_ancien() throws Exception {
        long enAttente = pretEnAttente(1L, "2026-10-01");

        JsonNode lignes = json.readTree(avecJeton(port, "GET", HISTORIQUE, admin).body());

        assertThat(lignes).hasSize(3);
        assertThat(lignes.get(0).get("id").asLong()).isEqualTo(enAttente);
        assertThat(lignes.get(1).get("statut").asString()).isEqualTo("EN_COURS");
        assertThat(lignes.get(1).get("dateDebut").asString()).isEqualTo("2026-08-01");
        assertThat(lignes.get(2).get("dateDebut").asString()).isEqualTo("2026-06-01");
        assertThat(lignes.get(1).get("nomClient").asString()).isEqualTo("Jean Dupont");
    }

    @Test
    @Tag("FR-016-12")
    void sans_aucun_pret_l_historique_est_vide() throws Exception {
        jdbc.sql("truncate table loan_repayments, loans restart identity").update();

        assertThat(avecJeton(port, "GET", HISTORIQUE, admin).body()).isEqualTo("[]");
    }

    @Test
    @Tag("FR-016-04")
    @Tag("FR-016-06")
    @Tag("FR-026-02")
    @Tag("FR-026-05")
    void l_approbation_passe_en_cours_cree_36_echeances_ecrit_l_audit_avec_l_ip_et_ne_publie_aucun_pret_decide()
            throws Exception {
        long pretId = pretEnAttenteUnique();

        HttpResponse<String> reponse = poster(HISTORIQUE + "/" + pretId + "/approbation", admin,
                Map.of("X-Forwarded-For", "203.0.113.7, 10.0.0.1"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(statut(pretId)).isEqualTo("EN_COURS");
        assertThat(compter("select count(*) from loan_repayments where loan_id = " + pretId + " and not paye"))
                .isEqualTo(36L);
        JsonNode audit = json.readTree(jdbc.sql("select charge from outbox_audit").query(String.class).single());
        assertThat(audit.get("type").asString()).isEqualTo("audit");
        assertThat(audit.get("producteur").asString()).isEqualTo("credits");
        assertThat(audit.get("donnees").get("utilisateur").asString()).isEqualTo("10");
        assertThat(audit.get("donnees").get("action").asString()).isEqualTo("ADMIN_LOAN_APPROVE");
        assertThat(audit.get("donnees").get("detail").asString()).isEqualTo("Approbation prêt id=" + pretId);
        assertThat(audit.get("donnees").get("adresseIp").asString()).isEqualTo("203.0.113.7");
        assertThat(compter("select count(*) from outbox")).isZero();
        assertThat(LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(), "credits.audit.v1",
                Long.toString(pretId), Duration.ofSeconds(20))).isPresent();
        assertThat(LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(), "credits.pret-decide.v1",
                Long.toString(pretId), Duration.ofSeconds(2))).isEmpty();
    }

    @Test
    @Tag("FR-016-05")
    @Tag("FR-016-06")
    @Tag("FR-026-02")
    void le_refus_passe_refuse_sans_echeancier_et_ecrit_l_audit_avec_l_adresse_distante() throws Exception {
        long pretId = pretEnAttente(1L, "2026-10-01");

        HttpResponse<String> reponse = poster(HISTORIQUE + "/" + pretId + "/refus", admin, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(statut(pretId)).isEqualTo("REFUSE");
        assertThat(compter("select count(*) from loan_repayments where loan_id = " + pretId)).isZero();
        JsonNode audit = json.readTree(jdbc.sql("select charge from outbox_audit").query(String.class).single());
        assertThat(audit.get("donnees").get("action").asString()).isEqualTo("ADMIN_LOAN_REFUSE");
        assertThat(audit.get("donnees").get("detail").asString()).isEqualTo("Refus prêt id=" + pretId);
        assertThat(audit.get("donnees").get("adresseIp").asString()).isIn("127.0.0.1", "0:0:0:0:0:0:0:1");
        assertThat(compter("select count(*) from outbox")).isZero();
    }

    @Test
    @Tag("FR-026-05")
    void un_en_tete_x_forwarded_for_illisible_retombe_sur_l_adresse_distante() throws Exception {
        long pretId = pretEnAttente(1L, "2026-10-01");

        poster(HISTORIQUE + "/" + pretId + "/refus", admin, Map.of("X-Forwarded-For", "pas-une-ip"));

        JsonNode audit = json.readTree(jdbc.sql("select charge from outbox_audit").query(String.class).single());
        assertThat(audit.get("donnees").get("adresseIp").asString()).isIn("127.0.0.1", "0:0:0:0:0:0:0:1");
    }

    @Test
    @Tag("FR-016-04")
    @Tag("FR-016-05")
    void un_pret_deja_decide_ou_inconnu_est_ignore_avec_200_sans_echeancier_ni_audit() throws Exception {
        // Prêts 1 et 2 des données de référence : EN_COURS, avec leurs échéances.
        long echeancesAvant = compter("select count(*) from loan_repayments");

        assertThat(poster(HISTORIQUE + "/1/approbation", admin, Map.of()).statusCode()).isEqualTo(200);
        assertThat(poster(HISTORIQUE + "/1/refus", admin, Map.of()).statusCode()).isEqualTo(200);
        assertThat(poster(HISTORIQUE + "/9999/approbation", admin, Map.of()).statusCode()).isEqualTo(200);

        assertThat(statut(1L)).isEqualTo("EN_COURS");
        assertThat(compter("select count(*) from loan_repayments")).isEqualTo(echeancesAvant);
        assertThat(compter("select count(*) from outbox_audit")).isZero();
    }

    @Test
    @Tag("FR-030-07")
    void client_et_conseiller_sont_refuses_en_403_sans_rien_modifier_et_l_admin_reussit() throws Exception {
        long pretId = pretEnAttente(1L, "2026-10-01");
        String route = HISTORIQUE + "/" + pretId;

        for (String jeton : List.of(OutilsHttp.jetonValide("1", "CLIENT"), OutilsHttp.jetonValide("11", "CONSEILLER"))) {
            for (String chemin : List.of(PIPELINE, HISTORIQUE)) {
                HttpResponse<String> reponse = avecJeton(port, "GET", chemin, jeton);
                assertThat(reponse.statusCode()).isEqualTo(403);
                assertThat(reponse.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
            }
            for (String action : List.of("/approbation", "/refus")) {
                HttpResponse<String> reponse = poster(route + action, jeton, Map.of());
                assertThat(reponse.statusCode()).isEqualTo(403);
                assertThat(reponse.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
            }
        }

        assertThat(statut(pretId)).isEqualTo("EN_ATTENTE");
        assertThat(compter("select count(*) from outbox_audit")).isZero();
        assertThat(avecJeton(port, "GET", PIPELINE, admin).statusCode()).isEqualTo(200);
        assertThat(poster(route + "/approbation", admin, Map.of()).statusCode()).isEqualTo(200);
        assertThat(statut(pretId)).isEqualTo("EN_COURS");
    }

    @Test
    @Tag("FR-030-07")
    void sans_jeton_les_routes_d_administration_rendent_401() throws Exception {
        HttpResponse<String> reponse = OutilsHttp.envoyer(port, "GET", PIPELINE, null, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(401);
    }

    @Test
    @Tag("FR-030-07")
    void un_jeton_dont_le_sujet_n_est_pas_numerique_rend_401_a_la_decision() throws Exception {
        long pretId = pretEnAttente(1L, "2026-10-01");

        HttpResponse<String> reponse = poster(HISTORIQUE + "/" + pretId + "/refus",
                OutilsHttp.jetonValide("admin", "ADMIN"), Map.of());

        assertThat(reponse.statusCode()).isEqualTo(401);
        assertThat(statut(pretId)).isEqualTo("EN_ATTENTE");
    }

    @Test
    @Tag("FR-016-07")
    @Tag("BUG-054")
    @Tag("FR-026-04")
    void une_erreur_pendant_l_approbation_laisse_le_pret_en_attente_garde_l_audit_et_masque_le_detail()
            throws Exception {
        long pretId = pretEnAttente(1L, "2026-10-01");
        jdbc.sql("create function echec_voulu() returns trigger language plpgsql as "
                + "$$ begin raise exception 'echec voulu du test'; end $$").update();
        jdbc.sql("create trigger echec_echeancier before insert on loan_repayments for each row "
                + "execute function echec_voulu()").update();

        HttpResponse<String> reponse = poster(HISTORIQUE + "/" + pretId + "/approbation", admin, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(500);
        assertThat(reponse.body()).isEqualTo(
                "{\"message\":\"Erreur lors du traitement du prêt. Veuillez réessayer.\"}");
        assertThat(reponse.body()).doesNotContain("echec voulu", "Exception", "postgres");
        assertThat(statut(pretId)).isEqualTo("EN_ATTENTE");
        assertThat(compter("select count(*) from loan_repayments where loan_id = " + pretId)).isZero();
        assertThat(jdbc.sql("select charge from outbox_audit").query(String.class).single())
                .contains("ADMIN_LOAN_APPROVE");
    }

    @Test
    @Tag("FR-016-07")
    @Tag("BUG-054")
    @Tag("FR-026-04")
    void une_erreur_a_la_validation_du_refus_laisse_le_pret_en_attente_et_garde_l_audit() throws Exception {
        long pretId = pretEnAttente(1L, "2026-10-01");
        jdbc.sql("create function echec_voulu() returns trigger language plpgsql as "
                + "$$ begin raise exception 'echec voulu du test'; end $$").update();
        jdbc.sql("create constraint trigger echec_validation after update on loans deferrable initially deferred "
                + "for each row execute function echec_voulu()").update();

        HttpResponse<String> reponse = poster(HISTORIQUE + "/" + pretId + "/refus", admin, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(500);
        assertThat(reponse.body()).isEqualTo(
                "{\"message\":\"Erreur lors du traitement du prêt. Veuillez réessayer.\"}");
        assertThat(statut(pretId)).isEqualTo("EN_ATTENTE");
        assertThat(jdbc.sql("select charge from outbox_audit").query(String.class).single())
                .contains("ADMIN_LOAN_REFUSE");
    }
}
