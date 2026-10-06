package fr.nextbanking.credits.adapter;

import static fr.nextbanking.credits.adapter.OutilsHttp.publier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import fr.nextbanking.credits.application.port.out.EvenementsSortants;
import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code POST /api/credits/prets} de bout en bout (Postgres et Kafka réels) : prêt EN_ATTENTE et ligne d'outbox dans
 * la même transaction, publication, refus 400 sans écriture, panne de l'outbox en 503 avec retour arrière, erreur
 * inattendue en 500 sans détail.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class DemandePretHttpIT {

    private static final String CHEMIN = "/api/credits/prets";
    private static final String TOPIC = "credits.pret-demande.v1";
    private static final String ERREUR_DEMANDE =
            "{\"message\":\"Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer.\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Ces tests constatent le prêt EN_ATTENTE : le scoring (écouteur) ne doit pas le décider.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @MockitoSpyBean
    private EvenementsSortants evenements;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    private HttpResponse<String> deposer(String corps) throws Exception {
        return publier(port, CHEMIN, corps, OutilsHttp.jetonValide("5"));
    }

    private static String corps(String type, String montant, String taux, String duree, String objet) {
        return "{\"type\":" + texte(type) + ",\"montant\":" + texte(montant) + ",\"tauxAnnuel\":" + texte(taux)
                + ",\"dureeMois\":" + texte(duree) + ",\"objet\":" + texte(objet) + "}";
    }

    private static String texte(String valeur) {
        return valeur == null ? "null" : "\"" + valeur + "\"";
    }

    private static String nominal() {
        return corps("CONSOMMATION", "10000", "3.5", "300", "Voiture");
    }

    private long compter(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    private void assertRefus(HttpResponse<String> reponse, String message) {
        assertThat(reponse.statusCode()).isEqualTo(400);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"" + message + "\"}");
    }

    @Test
    @Tag("FR-007-07")
    @Tag("FR-024-01")
    void une_demande_valide_cree_un_pret_en_attente_et_une_ligne_d_outbox_publiee() throws Exception {
        HttpResponse<String> reponse = deposer(nominal());

        assertThat(reponse.statusCode()).isEqualTo(201);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        JsonNode pret = json.readTree(reponse.body());
        long pretId = pret.get("id").asLong();
        assertThat(pret.get("statut").asString()).isEqualTo("EN_ATTENTE");
        assertThat(pret.get("type").asString()).isEqualTo("CONSOMMATION");
        assertThat(pret.get("montant").asString()).isEqualTo("10000.00");
        assertThat(pret.get("tauxAnnuel").asString()).isEqualTo("3.5000");
        assertThat(pret.get("dureeMois").asInt()).isEqualTo(300);
        assertThat(pret.get("objet").asString()).isEqualTo("Voiture");
        LocalDate debut = LocalDate.now(ZoneId.of("Europe/Paris")).withDayOfMonth(1);
        assertThat(pret.get("dateDebut").asString()).isEqualTo(debut.toString());
        assertThat(pret.get("dateFin").asString()).isEqualTo(debut.plusMonths(300).toString());

        assertThat(jdbc.sql("select status || '/' || motif || '/' || customer_id from loans where id = :id")
                .param("id", pretId).query(String.class).single()).isEqualTo("EN_ATTENTE/Voiture/5");
        assertThat(jdbc.sql("select topic || '/' || publie from outbox where cle = :cle")
                .param("cle", Long.toString(pretId)).query(String.class).single()).isEqualTo(TOPIC + "/true");

        ConsumerRecord<String, String> message = LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(), TOPIC,
                Long.toString(pretId), Duration.ofSeconds(20)).orElseThrow();
        assertThat(message.headers().lastHeader("__TypeId__")).isNull();
        JsonNode enveloppe = json.readTree(message.value());
        assertThat(enveloppe.get("type").asString()).isEqualTo("pret-demande");
        assertThat(enveloppe.get("producteur").asString()).isEqualTo("credits");
        assertThat(enveloppe.get("donnees").get("pretId").asLong()).isEqualTo(pretId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENT", "CONSEILLER", "ADMIN"})
    @Tag("FR-007-07")
    void tout_role_authentifie_depose_une_demande_pour_son_propre_identifiant(String role) throws Exception {
        HttpResponse<String> reponse = publier(port, CHEMIN, nominal(), OutilsHttp.jetonValide("9", role));

        assertThat(reponse.statusCode()).isEqualTo(201);
        long pretId = json.readTree(reponse.body()).get("id").asLong();
        assertThat(jdbc.sql("select customer_id from loans where id = :id").param("id", pretId)
                .query(Long.class).single()).isEqualTo(9L);
    }

    @Test
    @Tag("FR-007-07")
    void le_pret_depose_apparait_dans_la_liste_du_client() throws Exception {
        deposer(nominal());

        HttpResponse<String> liste = OutilsHttp.avecJeton(port, "GET", CHEMIN, OutilsHttp.jetonValide("5"));

        assertThat(liste.body()).contains("\"statut\":\"EN_ATTENTE\"", "\"objet\":\"Voiture\"");
    }

    @Test
    @Tag("FR-007-06")
    @Tag("BUG-025")
    void les_durees_301_et_360_sont_refusees_sans_rien_ecrire_et_300_est_acceptee() throws Exception {
        long prets = compter("loans");
        long lignes = compter("outbox");

        for (String duree : new String[] {"301", "360"}) {
            assertRefus(deposer(corps("CONSOMMATION", "10000", "3.5", duree, "Voiture")), "Duree invalide");
        }
        assertThat(compter("loans")).isEqualTo(prets);
        assertThat(compter("outbox")).isEqualTo(lignes);

        assertThat(deposer(corps("CONSOMMATION", "10000", "3.5", "300", "Voiture")).statusCode()).isEqualTo(201);
        assertThat(compter("loans")).isEqualTo(prets + 1);
        assertThat(compter("outbox")).isEqualTo(lignes + 1);
    }

    @Test
    @Tag("FR-007-05")
    void les_champs_invalides_rendent_400_aux_textes_du_legacy_sans_rien_ecrire() throws Exception {
        long prets = compter("loans");
        long lignes = compter("outbox");

        for (String taux : new String[] {"-1", "31"}) {
            assertRefus(deposer(corps("CONSOMMATION", "10000", taux, "36", "x")), "Taux invalide");
        }
        for (String duree : new String[] {"0", "361"}) {
            assertRefus(deposer(corps("CONSOMMATION", "10000", "3.5", duree, "x")), "Duree invalide");
        }
        for (String objet : new String[] {"", "   ", null}) {
            assertRefus(deposer(corps("CONSOMMATION", "10000", "3.5", "36", objet)), "Veuillez indiquer un motif");
        }
        for (String montant : new String[] {"", "0", null}) {
            assertRefus(deposer(corps("CONSOMMATION", montant, "3.5", "36", "x")), "Montant invalide");
        }
        for (String type : new String[] {"", "INCONNU", null}) {
            HttpResponse<String> reponse = deposer(corps(type, "10000", "3.5", "36", "x"));
            assertThat(reponse.statusCode()).isEqualTo(400);
            assertThat(reponse.body()).isEqualTo(ERREUR_DEMANDE);
        }
        assertThat(compter("loans")).isEqualTo(prets);
        assertThat(compter("outbox")).isEqualTo(lignes);
    }

    @Test
    @Tag("FR-007-05")
    void un_corps_illisible_rend_400() throws Exception {
        HttpResponse<String> reponse = deposer("pas du json");

        assertThat(reponse.statusCode()).isEqualTo(400);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Requête invalide\"}");
    }

    @Test
    @Tag("FR-024-02")
    @Tag("BUG-077")
    void une_panne_de_l_outbox_annule_la_demande_et_rend_503() throws Exception {
        doThrow(new IllegalStateException("détail interne sensible")).when(evenements).consigner(any());
        long prets = compter("loans");

        HttpResponse<String> reponse = deposer(nominal());

        assertThat(reponse.statusCode()).isEqualTo(503);
        assertThat(reponse.body()).isEqualTo(
                "{\"message\":\"Service credit temporairement indisponible, veuillez reessayer plus tard\"}");
        assertThat(reponse.body()).doesNotContain("détail interne", "Exception", "at fr.");
        assertThat(compter("loans")).isEqualTo(prets);
        assertThat(compter("outbox")).isZero();
        assertThat(jdbc.sql("select count(*) from loans where status = 'EN_ATTENTE'").query(Long.class).single())
                .isZero();
    }

    @Test
    @Tag("FR-007-07")
    void un_objet_de_plus_de_255_caracteres_est_une_erreur_inattendue_en_500_avec_retour_arriere() throws Exception {
        long prets = compter("loans");

        HttpResponse<String> reponse = deposer(corps("CONSOMMATION", "10000", "3.5", "36", "x".repeat(256)));

        assertThat(reponse.statusCode()).isEqualTo(500);
        assertThat(reponse.body()).isEqualTo(ERREUR_DEMANDE);
        assertThat(reponse.body()).doesNotContain("varchar", "SQL", "Exception");
        assertThat(compter("loans")).isEqualTo(prets);
        assertThat(compter("outbox")).isZero();
    }

    @Test
    @Tag("FR-007-07")
    void sans_jeton_la_reponse_est_401() throws Exception {
        HttpResponse<String> reponse = OutilsHttp.envoyer(port, "POST", CHEMIN, nominal(),
                java.util.Map.of("Content-Type", "application/json"));

        assertThat(reponse.statusCode()).isEqualTo(401);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Authentification requise\"}");
    }
}
