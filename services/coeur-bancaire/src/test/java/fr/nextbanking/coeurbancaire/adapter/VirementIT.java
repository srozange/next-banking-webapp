package fr.nextbanking.coeurbancaire.adapter;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Virement immédiat de bout en bout sur une vraie base et un vrai Kafka : API, contrôles, atomicité, remise au
 * traitement asynchrone, audit indépendant par l'outbox d'audit, idempotence. Données de référence du service.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nb.reseau.mandataires-de-confiance=127.0.0.0/8")
@DirtiesContext
@Import(VirementIT.ConfigurationHorloge.class)
class VirementIT {

    private static final String JETON_USER1 = "user1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class ConfigurationHorloge {
        @Bean
        @Primary
        HorlogeFixe horlogeFixe() {
            return new HorlogeFixe();
        }
    }

    @Autowired
    private HorlogeFixe horloge;

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
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS);
        DonneesDeReference.recharger(source, jdbc);
    }

    @Test
    @Tag("FR-003-09")
    @Tag("FR-003-08")
    @Tag("FR-023-01")
    @Tag("FR-003-13")
    void un_virement_nominal_est_execute_dans_une_transaction_avec_ses_deux_operations_et_sa_remise() throws Exception {
        HttpResponse<String> reponse = virement(1, 2, "100.00", "loyer", "cle-nominal", user1());

        assertThat(reponse.statusCode()).isEqualTo(201);
        JsonNode corps = json.readTree(reponse.body());
        long virementId = corps.get("virementId").asLong();
        assertThat(corps.get("etat").asString()).isEqualTo("EXECUTE");
        assertThat(corps.get("statut").asString()).isEqualTo("SUCCESS");
        assertThat(corps.get("compteSourceId").asLong()).isEqualTo(1);
        assertThat(corps.get("compteDestinataireId").asLong()).isEqualTo(2);
        assertThat(corps.get("montant").asString()).isEqualTo("100.00");
        assertThat(corps.get("motif").asString()).isEqualTo("loyer");
        assertThat(solde(1)).isEqualByComparingTo("2030.00");
        assertThat(solde(2)).isEqualByComparingTo("15300.00");
        List<Map<String, Object>> operations = jdbc.sql("""
                select compte_id, type, montant, solde_apres, description from operations order by id""")
                .query().listOfRows();
        assertThat(operations).extracting(o -> o.get("compte_id"), o -> o.get("type"), o -> o.get("description"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "DEBIT", "Virement vers FR76-0001-0002 - loyer"),
                        org.assertj.core.groups.Tuple.tuple(2L, "CREDIT", "Virement depuis FR76-0001-0001 - loyer"));
        assertThat((BigDecimal) operations.get(0).get("solde_apres")).isEqualByComparingTo("2030.00");
        assertThat((BigDecimal) operations.get(1).get("solde_apres")).isEqualByComparingTo("15300.00");
        assertThat(jdbc.sql("select etat || '/' || statut from virements where id = :id").param("id", virementId)
                .query(String.class).single()).isEqualTo("EXECUTE/SUCCESS");
        Map<String, Object> remise = jdbc.sql("""
                select topic, cle, charge, id_evenement from outbox
                where cle = :cle and topic = 'coeur-bancaire.virement-a-traiter.v1'""")
                .param("cle", Long.toString(virementId)).query().singleRow();
        assertThat(remise.get("topic")).isEqualTo("coeur-bancaire.virement-a-traiter.v1");
        JsonNode donnees = json.readTree((String) remise.get("charge")).get("donnees");
        assertThat(donnees.get("virementId").asLong()).isEqualTo(virementId);
        assertThat(donnees.get("montant").asString()).isEqualTo("100.00");
        assertThat(donnees.get("emetteurId").asLong()).isEqualTo(1);
        assertThat(donnees.get("compteDestinataireId").asLong()).isEqualTo(2);
        assertThat(donnees.get("motif").asString()).isEqualTo("loyer");
        // Le consommateur interne marque l'événement traité sans débiter une deuxième fois ni rien émettre.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(jdbc.sql(
                "select count(*) from evenements_traites where id_evenement = :id").param("id", remise.get("id_evenement"))
                .query(Long.class).single()).isEqualTo(1L));
        assertThat(solde(1)).isEqualByComparingTo("2030.00");
        // La remise, la décision et un événement par compte modifié : une ligne chacun, la notification n'étant
        // écrite qu'à la décision.
        assertThat(count("outbox")).isEqualTo(4);
        assertThat(jdbc.sql("select count(*) from outbox where topic = 'coeur-bancaire.virement-decide.v1'")
                .query(Long.class).single()).isEqualTo(1L);
        List<String> comptesModifies = jdbc.sql(
                "select charge from outbox where topic = 'coeur-bancaire.compte-modifie.v1' order by id")
                .query(String.class).list();
        assertThat(comptesModifies).hasSize(2);
        JsonNode source = json.readTree(comptesModifies.get(0)).get("donnees");
        JsonNode destinataire = json.readTree(comptesModifies.get(1)).get("donnees");
        assertThat(source.get("compteId").asLong()).isEqualTo(1);
        assertThat(source.get("solde").asString()).isEqualTo("2030.00");
        assertThat(source.get("versionAgregat").asLong()).isEqualTo(2);
        assertThat(destinataire.get("compteId").asLong()).isEqualTo(2);
        assertThat(destinataire.get("solde").asString()).isEqualTo("15300.00");
        assertThat(destinataire.get("versionAgregat").asLong()).isEqualTo(2);
        assertThat(jdbc.sql("select cle from outbox where topic = 'coeur-bancaire.compte-modifie.v1' order by id")
                .query(String.class).list()).containsExactly("1", "2");
    }

    @Test
    @Tag("FR-003-15")
    @Tag("FR-003-25")
    @Tag("FR-027-01")
    @Tag("FR-027-03")
    void le_parcours_nominal_de_sc_003_04_est_execute_sans_alerte() throws Exception {
        HttpResponse<String> reponse = virement(1, 2, "100.00", null, null, user1());

        assertThat(reponse.statusCode()).isEqualTo(201);
        assertThat(json.readTree(reponse.body()).get("etat").asString()).isEqualTo("EXECUTE");
        assertThat(count("alertes_fraude")).isZero();
        assertThat(solde(1)).isEqualByComparingTo("2030.00");
    }

    @Test
    @Tag("FR-003-25")
    @Tag("FR-027-03")
    void a_75_aucune_alerte_et_au_dela_une_alerte_open_par_signal_leve_et_le_virement_est_suspendu()
            throws Exception {
        jdbc.sql("update comptes set solde = 20000.00 where id = 1").update();

        // 5000.50 : LARGE_AMOUNT 40 + NEW_BENEFICIARY 35 = 75, aucune alerte.
        HttpResponse<String> a75 = virement(1, 2, "5000.50", null, null, user1());
        assertThat(count("alertes_fraude")).isZero();

        // 17/11 à 23:30 Paris : LARGE_AMOUNT 40 + NEW_BENEFICIARY 35 + UNUSUAL_HOURS 45 + ROUND_AMOUNT 20 = 140.
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(13 * 3600 + 1800));
        HttpResponse<String> a80 = virement(1, 2, "5000.00", null, null, user1());

        assertThat(a75.statusCode()).isEqualTo(201);
        assertThat(a80.statusCode()).isEqualTo(201);
        long virementId = json.readTree(a80.body()).get("virementId").asLong();
        assertThat(json.readTree(a80.body()).get("etat").asString()).isEqualTo("SUSPENDU");
        List<Map<String, Object>> alertes = jdbc.sql("""
                select virement_id, client_id, regle, score, statut, description, cree_le, revu_le, revu_par
                from alertes_fraude order by id""").query().listOfRows();
        assertThat(alertes).extracting(a -> a.get("regle"), a -> a.get("score"), a -> a.get("statut"),
                a -> a.get("virement_id"), a -> a.get("client_id"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("LARGE_AMOUNT", 40, "OPEN", virementId, 1L),
                        org.assertj.core.groups.Tuple.tuple("NEW_BENEFICIARY", 35, "OPEN", virementId, 1L),
                        org.assertj.core.groups.Tuple.tuple("UNUSUAL_HOURS", 45, "OPEN", virementId, 1L),
                        org.assertj.core.groups.Tuple.tuple("ROUND_AMOUNT", 20, "OPEN", virementId, 1L));
        assertThat(alertes.getFirst().get("description"))
                .isEqualTo("Transaction amount 5000.00 exceeds the large threshold of 5000");
        assertThat(alertes.get(1).get("description"))
                .isEqualTo("Target IBAN FR76-0001-0002 is not registered as a trusted beneficiary");
        assertThat(alertes.getFirst().get("revu_le")).isNull();
        assertThat(alertes.getFirst().get("revu_par")).isNull();
        assertThat(alertes.getFirst().get("cree_le")).isNotNull();
        assertThat(solde(1)).isEqualByComparingTo("14999.50");
    }

    @Test
    @Tag("FR-003-19")
    @Tag("FR-027-07")
    void un_beneficiaire_actif_ne_leve_pas_new_beneficiary() throws Exception {
        jdbc.sql("update comptes set solde = 20000.00 where id = 1").update();
        jdbc.sql("""
                insert into beneficiaires_externes (client_id, titulaire, iban, libelle, actif)
                values (1, 'Titulaire', 'FR76-0001-0002', 'Mon épargne', true)""").update();

        // LARGE_AMOUNT 40 + ROUND_AMOUNT 20 = 60 (35 de plus sans le bénéficiaire = 95 : des alertes).
        virement(1, 2, "5000.00", null, null, user1());

        assertThat(count("alertes_fraude")).isZero();
    }

    @Test
    @Tag("FR-003-07")
    @Tag("FR-027-01")
    void un_virement_rejete_n_entre_pas_dans_la_succession_rapide_du_suivant() throws Exception {
        assertRefus(corps(1, 2, "9999.00", null), 400, "Solde insuffisant sur le compte source");
        jdbc.sql("update comptes set solde = 20000.00 where id = 1").update();

        virement(1, 2, "5000.50", null, null, user1());

        // NEW_BENEFICIARY + LARGE_AMOUNT = 75 : sans RAPID_SUCCESSION, aucune alerte.
        assertThat(count("alertes_fraude")).isZero();
    }

    @Test
    @Tag("FR-003-23")
    @Tag("FR-027-01")
    void deux_soumissions_simultanees_d_un_meme_client_sont_serialisees_et_la_seconde_voit_la_premiere()
            throws Exception {
        jdbc.sql("update comptes set solde = 20000.00 where id = 1").update();

        CompletableFuture<HttpResponse<String>> premiere = CompletableFuture.supplyAsync(
                () -> virementSansEchec("5000.50"));
        CompletableFuture<HttpResponse<String>> seconde = CompletableFuture.supplyAsync(
                () -> virementSansEchec("5000.50"));

        assertThat(premiere.get().statusCode()).isEqualTo(201);
        assertThat(seconde.get().statusCode()).isEqualTo(201);
        // Un seul des deux voit l'autre (RAPID_SUCCESSION 55 : 130 au total) : exactement trois alertes.
        assertThat(count("alertes_fraude")).isEqualTo(3);
        assertThat(jdbc.sql("select count(distinct virement_id) from alertes_fraude").query(Long.class).single())
                .isEqualTo(1L);
    }

    private HttpResponse<String> virementSansEchec(String montant) {
        try {
            return virement(1, 2, montant, null, null, user1());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @Tag("FR-003-10")
    void a_5000_euros_pending_avec_operations_en_attente_et_a_4999_99_success() throws Exception {
        jdbc.sql("update comptes set solde = 20000.00 where id = 1").update();
        jdbc.sql("""
                insert into beneficiaires_externes (client_id, titulaire, iban, libelle, actif)
                values (1, 'Titulaire', 'FR76-0001-0002', 'Mon épargne', true)""").update();

        HttpResponse<String> pending = virement(1, 2, "5000.00", "gros", null, user1());
        HttpResponse<String> succes = virement(1, 2, "4999.99", "petit", null, user1());

        assertThat(json.readTree(pending.body()).get("statut").asString()).isEqualTo("PENDING");
        assertThat(json.readTree(succes.body()).get("statut").asString()).isEqualTo("SUCCESS");
        assertThat(jdbc.sql("select description from operations order by id").query(String.class).list())
                .containsExactly("[EN ATTENTE] Virement vers FR76-0001-0002 - gros",
                        "[EN ATTENTE] Virement depuis FR76-0001-0001 - gros",
                        "Virement vers FR76-0001-0002 - petit", "Virement depuis FR76-0001-0001 - petit");
    }

    @Test
    @Tag("FR-003-02")
    @Tag("FR-003-07")
    void les_saisies_refusees_rendent_leur_message_sans_aucune_ecriture() throws Exception {
        assertRefus(corps(null, 2, "100", null), 400, "Veuillez sélectionner les deux comptes");
        assertRefus(corps(1, null, "100", null), 400, "Veuillez sélectionner les deux comptes");
        assertRefus("{}", 400, "Veuillez sélectionner les deux comptes");
        assertRefus(corps(1, 1, "100", "[retro] test comptes identiques"), 400,
                "Le compte source et destination doivent être différents");
        for (String montant : new String[] {"0", "-5", "abc", ""}) {
            assertRefus(corps(1, 2, montant, null), 400, "Erreur lors du virement : Le montant doit etre positif");
        }
        assertRefus("{\"compteSourceId\":1,\"compteDestinataireId\":2,\"motif\":\"x\"}", 400,
                "Erreur lors du virement : Le montant doit etre positif");
        assertRefus("{\"compteSourceId\":1,\"compteDestinataireId\":2,\"montant\":-5}", 400,
                "Erreur lors du virement : Le montant doit etre positif");
        assertRefus(corps(1, 2, "100", "x".repeat(101)), 400, "Le motif ne doit pas dépasser 100 caractères");
        assertRefus("pas du json", 400, "Requête invalide");
        assertRefus("{\"compteSourceId\":\"abc\"}", 400, "Requête invalide");
        aucuneEcriture();
    }

    @Test
    @Tag("FR-003-05")
    void un_compte_inexistant_rend_404_sans_debit_ni_credit() throws Exception {
        assertRefus(corps(999, 2, "100", null), 404, "Erreur lors du virement : Compte introuvable");
        assertRefus(corps(1, 999, "100", null), 404, "Erreur lors du virement : Compte introuvable");
        aucuneEcriture();
    }

    @Test
    @Tag("FR-003-05")
    void un_compte_d_un_autre_client_rend_403_sans_debit_ni_credit_et_sans_jeton_401() throws Exception {
        assertRefus(corps(3, 2, "100", null), 403, "Accès refusé");
        assertRefus(corps(1, 3, "100", null), 403, "Accès refusé");
        HttpResponse<String> sansJeton = envoyer("POST", "/api/coeur-bancaire/virements", null,
                corps(1, 2, "100", null), null, null);
        assertThat(sansJeton.statusCode()).isEqualTo(401);
        assertThat(sansJeton.body()).isEqualTo("{\"message\":\"Authentification requise\"}");
        aucuneEcriture();
    }

    @Test
    @Tag("FR-003-06")
    @Tag("BUG-008")
    @Tag("BUG-011")
    void un_solde_insuffisant_rend_un_message_clair_sans_aucune_ecriture() throws Exception {
        HttpResponse<String> reponse = virement(1, 2, "9999.00", null, null, user1());

        assertThat(reponse.statusCode()).isBetween(400, 499);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Solde insuffisant sur le compte source\"}")
                .doesNotContain("Exception", "java.lang");
        aucuneEcriture();
    }

    @Test
    @Tag("FR-003-08")
    void deux_soumissions_de_la_meme_cle_ne_font_qu_un_virement_et_une_reponse_identique() throws Exception {
        HttpResponse<String> premiere = virement(1, 2, "100.00", "loyer", "cle-double", user1());
        HttpResponse<String> seconde = virement(1, 2, "100.00", "loyer", "cle-double", user1());

        assertThat(premiere.statusCode()).isEqualTo(201);
        assertThat(seconde.statusCode()).isEqualTo(201);
        assertThat(seconde.body()).isEqualTo(premiere.body());
        assertThat(count("virements")).isEqualTo(1);
        assertThat(solde(1)).isEqualByComparingTo("2030.00");
        assertThat(count("operations")).isEqualTo(2);
        assertThat(count("outbox_audit")).isEqualTo(2);
    }

    @Test
    @Tag("FR-026-03")
    @Tag("FR-003-11")
    void l_audit_est_ecrit_par_l_outbox_d_audit_dans_l_ordre_avec_l_utilisateur_et_l_adresse_ip_d_origine()
            throws Exception {
        HttpResponse<String> reponse = envoyer("POST", "/api/coeur-bancaire/virements", user1(),
                corps(1, 2, "100.00", "loyer"), "cle-audit", "203.0.113.7");
        long virementId = json.readTree(reponse.body()).get("virementId").asLong();

        List<Map<String, Object>> lignes = jdbc.sql("select topic, cle, charge from outbox_audit order by id")
                .query().listOfRows();
        assertThat(lignes).hasSize(2).allSatisfy(l -> {
            assertThat(l.get("topic")).isEqualTo("coeur-bancaire.audit.v1");
            assertThat(l.get("cle")).isEqualTo(Long.toString(virementId));
        });
        JsonNode premiere = json.readTree((String) lignes.get(0).get("charge"));
        JsonNode seconde = json.readTree((String) lignes.get(1).get("charge"));
        assertThat(premiere.get("type").asString()).isEqualTo("audit");
        assertThat(premiere.get("producteur").asString()).isEqualTo("coeur-bancaire");
        assertThat(premiere.get("donnees").get("action").asString()).isEqualTo("VIREMENT");
        assertThat(premiere.get("donnees").get("detail").asString())
                .isEqualTo("Montant: 100.00 - Motif: loyer - Issue: EXECUTE");
        assertThat(premiere.get("donnees").get("utilisateur").asString()).isEqualTo("1");
        assertThat(premiere.get("donnees").get("adresseIp").asString()).isEqualTo("203.0.113.7");
        assertThat(seconde.get("donnees").get("action").asString()).isEqualTo("TRANSFER_EXECUTED");
        assertThat(seconde.get("donnees").get("detail").asString()).isEqualTo("Montant: 100.00 - Motif: loyer");
        assertThat(seconde.get("donnees").get("adresseIp").asString()).isEqualTo("203.0.113.7");
        // Publiées sur Kafka, clé = identifiant du virement.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(jdbc.sql(
                "select count(*) from outbox_audit where publie").query(Long.class).single()).isEqualTo(2L));
        ConsumerRecord<String, String> message = LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(),
                "coeur-bancaire.audit.v1", Long.toString(virementId), Duration.ofSeconds(20)).orElseThrow();
        assertThat(message.headers().lastHeader("__TypeId__")).isNull();
        assertThat(json.readTree(message.value()).get("donnees").get("action").asString()).isEqualTo("VIREMENT");
    }

    @Test
    @Tag("FR-003-01")
    void les_comptes_d_un_client_sont_listes_et_un_conseiller_ou_un_admin_n_en_a_aucun() throws Exception {
        HttpResponse<String> client = envoyer("GET", "/api/coeur-bancaire/comptes", user1(), null, null, null);
        HttpResponse<String> admin = envoyer("GET", "/api/coeur-bancaire/comptes", Jetons.pour(10, "ADMIN"), null,
                null, null);
        HttpResponse<String> conseiller = envoyer("GET", "/api/coeur-bancaire/comptes", Jetons.pour(11, "CONSEILLER"),
                null, null, null);
        HttpResponse<String> anonyme = envoyer("GET", "/api/coeur-bancaire/comptes", null, null, null, null);

        assertThat(client.statusCode()).isEqualTo(200);
        assertThat(json.readTree(client.body())).isEqualTo(json.readTree("""
                [{"id":1,"type":"COURANT","numero":"FR76-0001-0001","solde":"2130.00"},
                {"id":2,"type":"EPARGNE","numero":"FR76-0001-0002","solde":"15200.00"}]"""));
        assertThat(admin.body()).isEqualTo("[]");
        assertThat(conseiller.body()).isEqualTo("[]");
        assertThat(anonyme.statusCode()).isEqualTo(401);
    }

    @Test
    @Tag("FR-003-01")
    void une_methode_non_prevue_rend_le_message_generique() throws Exception {
        HttpResponse<String> reponse = envoyer("GET", "/api/coeur-bancaire/virements", user1(), null, null, null);

        assertThat(reponse.statusCode()).isEqualTo(405);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
    }

    private void assertRefus(String corps, int statut, String message) throws Exception {
        HttpResponse<String> reponse = envoyer("POST", "/api/coeur-bancaire/virements", user1(), corps, null, null);

        assertThat(reponse.statusCode()).as(corps).isEqualTo(statut);
        assertThat(reponse.body()).as(corps).isEqualTo("{\"message\":\"" + message + "\"}");
    }

    private void aucuneEcriture() {
        assertThat(solde(1)).isEqualByComparingTo("2130.00");
        assertThat(solde(2)).isEqualByComparingTo("15200.00");
        assertThat(count("operations")).isZero();
        assertThat(count("virements")).isZero();
        assertThat(count("outbox")).isZero();
        assertThat(count("outbox_audit")).isZero();
    }

    private BigDecimal solde(long compteId) {
        return jdbc.sql("select solde from comptes where id = :id").param("id", compteId)
                .query(BigDecimal.class).single();
    }

    private long count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    private static String user1() {
        return Jetons.pour(1, "CLIENT");
    }

    private static String corps(Integer source, Integer destinataire, String montant, String motif) {
        return "{" + (source == null ? "" : "\"compteSourceId\":" + source + ",")
                + (destinataire == null ? "" : "\"compteDestinataireId\":" + destinataire + ",")
                + "\"montant\":\"" + montant + "\""
                + (motif == null ? "" : ",\"motif\":\"" + motif + "\"") + "}";
    }

    private HttpResponse<String> virement(int source, int destinataire, String montant, String motif, String cle,
            String jeton) throws Exception {
        return envoyer("POST", "/api/coeur-bancaire/virements", jeton, corps(source, destinataire, montant, motif),
                cle, null);
    }

    private HttpResponse<String> envoyer(String methode, String chemin, String jeton, String corps, String cle,
            String adresse) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + chemin))
                .method(methode, corps == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corps));
        if (corps != null) {
            requete.header("Content-Type", "application/json");
        }
        if (jeton != null) {
            requete.header("Authorization", "Bearer " + jeton);
        }
        if (cle != null) {
            requete.header("Idempotency-Key", cle);
        }
        if (adresse != null) {
            requete.header("X-Forwarded-For", adresse);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(requete.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
