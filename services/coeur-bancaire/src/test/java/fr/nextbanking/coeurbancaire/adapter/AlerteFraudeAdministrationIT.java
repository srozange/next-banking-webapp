package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Alertes de fraude de l'administration, de bout en bout sur une vraie base : toutes les alertes listées (BUG-056)
 * avec le nom du client et celui du relecteur lus dans la projection locale, confirmation et faux positif depuis OPEN
 * seulement, auteur et heure enregistrés, et aucun effet sur le virement suspendu lié.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"nb.reseau.mandataires-de-confiance=127.0.0.0/8", "spring.kafka.listener.auto-startup=false"})
@DirtiesContext
@Import(AlerteFraudeAdministrationIT.ConfigurationHorloge.class)
@Tag("FR-016-14")
@Tag("FR-016-15")
@Tag("FR-016-16")
class AlerteFraudeAdministrationIT {

    private static final String CHEMIN = "/api/coeur-bancaire/administration/alertes-fraude";
    private static final Instant T = HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(1800);

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @TestConfiguration
    static class ConfigurationHorloge {
        @Bean
        @Primary
        HorlogeFixe horlogeFixe() {
            return new HorlogeFixe();
        }
    }

    @LocalServerPort
    private int port;
    @Autowired
    private HorlogeFixe horloge;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private DataSource source;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void recharger() throws Exception {
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS);
        DonneesDeReference.recharger(source, jdbc);
        try (Connection connexion = source.getConnection()) {
            ScriptUtils.executeSqlScript(connexion, new FileSystemResource("reference-data/projection-clients.sql"));
        }
        jdbc.sql("update comptes set solde = 50000.00 where id = 1").update();
        jdbc.sql("""
                insert into beneficiaires_externes (client_id, titulaire, iban, libelle, actif)
                values (1, 'Titulaire', 'FR76-0001-0002', 'Mon épargne', true)""").update();
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    private long alerte(long clientId, Long virementId, String regle, int score, String statut, String creeLe,
            String revuPar) {
        return jdbc.sql("""
                insert into alertes_fraude (client_id, virement_id, regle, score, description, statut, cree_le,
                                            revu_le, revu_par)
                values (:clientId, :virementId, :regle, :score, :description, :statut, cast(:creeLe as timestamptz),
                        case when cast(:revuPar as varchar) is null then null
                             else timestamptz '2026-11-16 12:00:00+00' end, :revuPar)
                returning id""")
                .param("clientId", clientId).param("virementId", virementId).param("regle", regle)
                .param("score", score).param("description", "Alerte " + regle).param("statut", statut)
                .param("creeLe", creeLe).param("revuPar", revuPar)
                .query(Long.class).single();
    }

    private long suspendre() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", "/api/coeur-bancaire/virements",
                Jetons.pour(1, "CLIENT"),
                "{\"compteSourceId\":1,\"compteDestinataireId\":2,\"montant\":\"10000.00\",\"motif\":\"loyer\"}");
        assertThat(reponse.statusCode()).isEqualTo(201);
        assertThat(json.readTree(reponse.body()).get("etat").asString()).isEqualTo("SUSPENDU");
        jdbc.sql("delete from alertes_fraude").update();
        return json.readTree(reponse.body()).get("virementId").asLong();
    }

    private JsonNode lire() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, Jetons.pour(10, "ADMIN"), null);
        assertThat(reponse.statusCode()).isEqualTo(200);
        return json.readTree(reponse.body());
    }

    private HttpResponse<String> revoir(long alerteId, String issue) throws Exception {
        return Jetons.envoyer(port, "POST", CHEMIN + "/" + alerteId + "/" + issue, Jetons.pour(10, "ADMIN"), null);
    }

    private Map<String, Object> ligne(long alerteId) {
        return jdbc.sql("select statut, revu_par, revu_le from alertes_fraude where id = :id").param("id", alerteId)
                .query().singleRow();
    }

    @Test
    @Tag("BUG-056")
    void toutes_les_alertes_sont_listees_la_plus_recente_d_abord_avec_les_noms_de_la_projection() throws Exception {
        long confirmee = alerte(1, null, "ROUND_AMOUNT", 20, "CONFIRMED", "2026-11-15 08:00:00+00", "10");
        long fauxPositif = alerte(2, null, "FOREIGN_IBAN", 30, "FALSE_POSITIVE", "2026-11-16 08:00:00+00", "10");
        long ouverte = alerte(1, null, "LARGE_AMOUNT", 45, "OPEN", "2026-11-17 08:00:00+00", null);
        long horsProjection = alerte(99, null, "MULTIPLE_FAILED_AUTH", 50, "REVIEWED", "2026-11-15 08:00:00+00",
                "77");

        JsonNode alertes = lire();

        assertThat(alertes).hasSize(4);
        assertThat(List.of(alertes.get(0).get("alerteId").asLong(), alertes.get(1).get("alerteId").asLong(),
                alertes.get(2).get("alerteId").asLong(), alertes.get(3).get("alerteId").asLong()))
                .containsExactly(ouverte, fauxPositif, horsProjection, confirmee);
        assertThat(alertes.get(0).toString()).isEqualTo("{\"alerteId\":" + ouverte + ",\"clientId\":1,"
                + "\"nomClient\":\"Jean Dupont\",\"regle\":\"LARGE_AMOUNT\",\"score\":45,\"statut\":\"OPEN\","
                + "\"description\":\"Alerte LARGE_AMOUNT\",\"creeLe\":\"2026-11-17T08:00:00Z\",\"revuPar\":null,"
                + "\"nomRelecteur\":null}");
        assertThat(alertes.get(1).get("statut").asString()).isEqualTo("FALSE_POSITIVE");
        assertThat(alertes.get(1).get("nomClient").asString()).isEqualTo("Marie Lebrun");
        assertThat(alertes.get(1).get("revuPar").asString()).isEqualTo("10");
        assertThat(alertes.get(1).get("nomRelecteur").asString()).isEqualTo("Alice Martin");
        assertThat(alertes.get(3).get("statut").asString()).isEqualTo("CONFIRMED");
        assertThat(alertes.get(3).get("nomRelecteur").asString()).isEqualTo("Alice Martin");
        assertThat(alertes.get(2).get("nomClient").isNull()).isTrue();
        assertThat(alertes.get(2).get("clientId").asLong()).isEqualTo(99);
        assertThat(alertes.get(2).get("revuPar").asString()).isEqualTo("77");
        assertThat(alertes.get(2).get("nomRelecteur").isNull()).isTrue();
    }

    @Test
    @Tag("BUG-056")
    void sans_alerte_la_liste_est_vide_et_le_parametre_de_requete_est_ignore() throws Exception {
        assertThat(lire().toString()).isEqualTo("[]");
        alerte(1, null, "LARGE_AMOUNT", 45, "OPEN", "2026-11-17 08:00:00+00", null);

        HttpResponse<String> filtre = Jetons.envoyer(port, "GET", CHEMIN + "?statut=CONFIRMED&clientId=2",
                Jetons.pour(10, "ADMIN"), null);

        assertThat(json.readTree(filtre.body())).hasSize(1);
    }

    @Test
    @Tag("FR-027-12")
    void la_confirmation_passe_l_alerte_open_a_confirmed_avec_l_auteur_et_l_heure() throws Exception {
        long id = alerte(1, null, "LARGE_AMOUNT", 45, "OPEN", "2026-11-17 08:00:00+00", null);
        horloge.regler(T);

        HttpResponse<String> reponse = revoir(id, "confirmation");

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(reponse.body()).isEmpty();
        Map<String, Object> ligne = ligne(id);
        assertThat(ligne.get("statut")).isEqualTo("CONFIRMED");
        assertThat(ligne.get("revu_par")).isEqualTo("10");
        assertThat(((java.sql.Timestamp) ligne.get("revu_le")).toInstant()).isEqualTo(T);
        JsonNode relue = lire().get(0);
        assertThat(relue.get("statut").asString()).isEqualTo("CONFIRMED");
        assertThat(relue.get("nomRelecteur").asString()).isEqualTo("Alice Martin");
    }

    @Test
    @Tag("FR-027-12")
    void le_faux_positif_passe_l_alerte_open_a_false_positive_avec_l_auteur_et_l_heure() throws Exception {
        long id = alerte(1, null, "LARGE_AMOUNT", 45, "OPEN", "2026-11-17 08:00:00+00", null);
        horloge.regler(T);

        HttpResponse<String> reponse = revoir(id, "faux-positif");

        assertThat(reponse.statusCode()).isEqualTo(204);
        Map<String, Object> ligne = ligne(id);
        assertThat(ligne.get("statut")).isEqualTo("FALSE_POSITIVE");
        assertThat(ligne.get("revu_par")).isEqualTo("10");
        assertThat(((java.sql.Timestamp) ligne.get("revu_le")).toInstant()).isEqualTo(T);
    }

    @Test
    @Tag("FR-027-14")
    void une_alerte_deja_traitee_ou_inconnue_rend_204_sans_rien_changer() throws Exception {
        long revue = alerte(1, null, "LARGE_AMOUNT", 45, "REVIEWED", "2026-11-17 08:00:00+00", "99");
        long confirmee = alerte(1, null, "ROUND_AMOUNT", 20, "CONFIRMED", "2026-11-17 08:00:00+00", "99");
        long fauxPositif = alerte(1, null, "FOREIGN_IBAN", 30, "FALSE_POSITIVE", "2026-11-17 08:00:00+00", "99");
        horloge.regler(T);

        for (long id : new long[] {revue, confirmee, fauxPositif}) {
            Map<String, Object> avant = ligne(id);
            assertThat(revoir(id, "confirmation").statusCode()).isEqualTo(204);
            assertThat(revoir(id, "faux-positif").statusCode()).isEqualTo(204);
            assertThat(ligne(id)).isEqualTo(avant);
        }
        assertThat(revoir(123456, "confirmation").statusCode()).isEqualTo(204);
        assertThat(jdbc.sql("select count(*) from alertes_fraude").query(Long.class).single()).isEqualTo(3L);
    }

    @Test
    @Tag("FR-027-13")
    @Tag("FR-027-14")
    void confirmer_l_alerte_d_un_virement_suspendu_ne_change_pas_le_virement_puis_la_liberation_garde_confirmed()
            throws Exception {
        long virementId = suspendre();
        long confirmee = alerte(1, virementId, "LARGE_AMOUNT", 40, "OPEN", "2026-11-17 09:00:00+00", null);
        long ouverte = alerte(1, virementId, "ROUND_AMOUNT", 20, "OPEN", "2026-11-17 09:00:00+00", null);
        Map<String, Object> avant = etatGlobal(virementId);
        horloge.regler(T);

        assertThat(revoir(confirmee, "confirmation").statusCode()).isEqualTo(204);

        assertThat(etatGlobal(virementId)).isEqualTo(avant);
        assertThat(avant.get("etat")).isEqualTo("SUSPENDU");
        assertThat(ligne(confirmee).get("statut")).isEqualTo("CONFIRMED");
        assertThat(ligne(ouverte).get("statut")).isEqualTo("OPEN");

        HttpResponse<String> liberation = Jetons.envoyer(port, "POST",
                "/api/coeur-bancaire/revue/virements/" + virementId + "/liberer", Jetons.pour(10, "ADMIN"),
                "{\"motif\":\"vérifié\"}");

        assertThat(liberation.statusCode()).isEqualTo(200);
        assertThat(ligne(confirmee).get("statut")).isEqualTo("CONFIRMED");
        assertThat(ligne(confirmee).get("revu_par")).isEqualTo("10");
        assertThat(ligne(ouverte).get("statut")).isEqualTo("REVIEWED");
    }

    /** État du virement, des soldes et des volumes d'opérations, d'outbox et d'audit. */
    private Map<String, Object> etatGlobal(long virementId) {
        return jdbc.sql("""
                select (select etat from virements where id = :id) as etat,
                       (select count(*) from virements) as virements,
                       (select string_agg(id || ':' || solde, ',' order by id) from comptes) as soldes,
                       (select count(*) from operations) as operations,
                       (select count(*) from outbox) as outbox,
                       (select count(*) from outbox_audit) as audit""")
                .param("id", virementId).query().singleRow();
    }
}
