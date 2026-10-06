package fr.nextbanking.credits.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Décision automatique de bout en bout (Postgres et Kafka réels) : demande déposée par HTTP puis décidée par
 * l'écouteur, messages bruts livrés deux fois, prêts non EN_ATTENTE ou inconnus ignorés, prêt sans type refusé.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class DecisionPretKafkaIT {

    private static final String TOPIC_DEMANDE = "credits.pret-demande.v1";
    private static final String TOPIC_DECIDE = "credits.pret-decide.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Groupe neuf, depuis la fin du topic : les messages laissés par les autres classes de test ne sont pas relus.
        registre.add("spring.kafka.consumer.group-id", () -> "credits-it-" + UUID.randomUUID());
        registre.add("spring.kafka.consumer.auto-offset-reset", () -> "latest");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private KafkaListenerEndpointRegistry ecouteurs;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void donnees() throws Exception {
        OutilsHttp.chargerDonnees(base);
        await().atMost(Duration.ofSeconds(30)).until(() -> ecouteurs.getListenerContainers().stream()
                .allMatch(conteneur -> !conteneur.getAssignedPartitions().isEmpty()));
        Thread.sleep(1000);
    }

    private long inserer(String type, String montant, int duree, String statut) {
        return jdbc.sql("""
                insert into loans (customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (5, :type, :montant::numeric, 3.5, :duree, 100, 1200, 0, 'x', :statut, date '2026-10-01',
                        date '2029-10-01') returning id""")
                .param("type", type).param("montant", montant).param("duree", duree).param("statut", statut)
                .query(Long.class).single();
    }

    private static String enveloppe(UUID idEvenement, long pretId) {
        return """
                {"idEvenement":"%s","type":"pret-demande","version":1,"horodatage":"2026-10-06T08:30:00Z",\
                "producteur":"credits","donnees":{"pretId":%d}}""".formatted(idEvenement, pretId);
    }

    private static void envoyer(long pretId, String... messages) throws Exception {
        try (KafkaProducer<String, String> producteur = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Conteneurs.KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            for (String message : messages) {
                producteur.send(new ProducerRecord<>(TOPIC_DEMANDE, Long.toString(pretId), message)).get();
            }
        }
    }

    private String statut(long pretId) {
        return jdbc.sql("select status from loans where id = :id").param("id", pretId).query(String.class).single();
    }

    private long compter(String sql, Object parametre) {
        return jdbc.sql(sql).param("p", parametre).query(Long.class).single();
    }

    private long traites() {
        return jdbc.sql("select count(*) from evenements_traites").query(Long.class).single();
    }

    private void attendreStatut(long pretId, String attendu) {
        await().atMost(Duration.ofSeconds(30)).until(() -> attendu.equals(statut(pretId)));
    }

    private JsonNode decision(long pretId) {
        String charge = jdbc.sql("select charge from outbox where topic = :topic and cle = :cle")
                .param("topic", TOPIC_DECIDE).param("cle", Long.toString(pretId)).query(String.class).single();
        return json.readTree(charge);
    }

    @Test
    @Tag("FR-007-10")
    @Tag("FR-024-06")
    void une_demande_deposee_est_approuvee_avec_son_echeancier_et_publie_la_decision() throws Exception {
        // Identifiant neuf : le topic garde les messages des autres tests, la lecture se fait par clé.
        jdbc.sql("select setval(pg_get_serial_sequence('loans', 'id'), :n)")
                .param("n", 1_000_000L + System.nanoTime() % 1_000_000_000L).query(Long.class).single();
        HttpResponse<String> reponse = OutilsHttp.publier(port, "/api/credits/prets",
                "{\"type\":\"CONSOMMATION\",\"montant\":\"10000\",\"tauxAnnuel\":\"3.5\",\"dureeMois\":\"36\","
                        + "\"objet\":\"Voiture\"}", OutilsHttp.jetonValide("5"));
        long pretId = json.readTree(reponse.body()).get("id").asLong();

        attendreStatut(pretId, "EN_COURS");

        assertThat(OutilsHttp.avecJeton(port, "GET", "/api/credits/prets", OutilsHttp.jetonValide("5")).body())
                .contains("\"statut\":\"EN_COURS\"");
        assertThat(compter("select count(*) from loan_repayments where loan_id = :p and not paye", pretId))
                .isEqualTo(36L);
        assertThat(traites()).isEqualTo(1L);
        JsonNode enveloppe = decision(pretId);
        assertThat(enveloppe.get("type").asString()).isEqualTo("pret-decide");
        assertThat(enveloppe.get("producteur").asString()).isEqualTo("credits");
        JsonNode donnees = enveloppe.get("donnees");
        assertThat(donnees.get("pretId").asLong()).isEqualTo(pretId);
        assertThat(donnees.get("clientId").asLong()).isEqualTo(5L);
        assertThat(donnees.get("statut").asString()).isEqualTo("EN_COURS");
        assertThat(donnees.get("notification").get("clientId").asLong()).isEqualTo(5L);
        assertThat(donnees.get("notification").get("type").asString()).isEqualTo("CONFIRMATION");
        assertThat(donnees.get("notification").get("titre").asString()).isEqualTo("Demande de prêt approuvée");
        assertThat(donnees.get("notification").get("message").asString()).isEqualTo(
                "Votre demande de pret Consommation de 10000.00 EUR a ete approuvee par le service credit.");

        ConsumerRecord<String, String> publie = LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(),
                TOPIC_DECIDE, Long.toString(pretId), Duration.ofSeconds(20)).orElseThrow();
        assertThat(publie.headers().lastHeader("__TypeId__")).isNull();
        assertThat(json.readTree(publie.value()).get("donnees").get("statut").asString()).isEqualTo("EN_COURS");
    }

    @Test
    @Tag("FR-024-03")
    @Tag("FR-024-07")
    void un_plafond_depasse_ou_une_duree_excessive_est_refuse_sans_echeancier() throws Exception {
        long plafond = inserer("CONSOMMATION", "25000.01", 36, "EN_ATTENTE");
        long duree = inserer("CONSOMMATION", "100.00", 301, "EN_ATTENTE");
        envoyer(plafond, enveloppe(UUID.randomUUID(), plafond));
        envoyer(duree, enveloppe(UUID.randomUUID(), duree));

        attendreStatut(plafond, "REFUSE");
        attendreStatut(duree, "REFUSE");

        assertThat(compter("select count(*) from loan_repayments where loan_id = :p", plafond)).isZero();
        assertThat(compter("select count(*) from loan_repayments where loan_id = :p", duree)).isZero();
        JsonNode notification = decision(plafond).get("donnees").get("notification");
        assertThat(decision(plafond).get("donnees").get("statut").asString()).isEqualTo("REFUSE");
        assertThat(notification.get("type").asString()).isEqualTo("ALERTE");
        assertThat(notification.get("titre").asString()).isEqualTo("Demande de prêt refusée");
        assertThat(notification.get("message").asString()).isEqualTo(
                "Votre demande de pret Consommation de 25000.01 EUR a ete refusee (plafond depasse ou duree excessive).");
    }

    @Test
    @Tag("FR-024-05")
    @Tag("BUG-078")
    void un_pret_sans_type_est_refuse_et_la_liste_le_montre_sans_planter() throws Exception {
        long pretId = jdbc.sql("""
                insert into loans (customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (5, null, 10000, 3.5, 60, 100, 6000, 0, 'x', 'EN_ATTENTE', date '2026-10-01',
                        date '2031-10-01') returning id""").query(Long.class).single();
        envoyer(pretId, enveloppe(UUID.randomUUID(), pretId));

        attendreStatut(pretId, "REFUSE");

        assertThat(decision(pretId).get("donnees").get("notification").get("message").asString()).isEqualTo(
                "Votre demande de pret (type non reconnu) de 10000.00 EUR a ete refusee (plafond depasse ou duree excessive).");
        HttpResponse<String> liste = OutilsHttp.avecJeton(port, "GET", "/api/credits/prets",
                OutilsHttp.jetonValide("5"));
        assertThat(liste.statusCode()).isEqualTo(200);
        assertThat(liste.body()).contains("\"type\":null", "\"statut\":\"REFUSE\"");
    }

    @Test
    @Tag("FR-024-08")
    void un_pret_non_en_attente_ou_inconnu_est_ignore_sans_erreur() throws Exception {
        long enCours = inserer("CONSOMMATION", "10000.00", 36, "EN_COURS");
        long refuse = inserer("CONSOMMATION", "10000.00", 36, "REFUSE");
        long rembourse = inserer("CONSOMMATION", "10000.00", 36, "REMBOURSE");
        long sentinelle = inserer("CONSOMMATION", "10000.00", 36, "EN_ATTENTE");
        for (long pretId : new long[] {enCours, refuse, rembourse, 999_999L}) {
            envoyer(pretId, enveloppe(UUID.randomUUID(), pretId));
        }
        envoyer(sentinelle, enveloppe(UUID.randomUUID(), sentinelle));

        attendreStatut(sentinelle, "EN_COURS");
        // Tous les messages sont consommés quand le dernier de la fin (envoyé après les autres) est décidé.
        await().atMost(Duration.ofSeconds(30)).until(
                () -> traites() == 5L);

        assertThat(statut(enCours)).isEqualTo("EN_COURS");
        assertThat(statut(refuse)).isEqualTo("REFUSE");
        assertThat(statut(rembourse)).isEqualTo("REMBOURSE");
        for (long pretId : new long[] {enCours, refuse, rembourse}) {
            assertThat(compter("select count(*) from loan_repayments where loan_id = :p", pretId)).isZero();
            assertThat(compter("select count(*) from outbox where topic = 'credits.pret-decide.v1' and cle = :p",
                    Long.toString(pretId))).isZero();
        }
        assertThat(compter("select count(*) from outbox where topic = 'credits.pret-decide.v1' and cle = :p",
                "999999")).isZero();
    }

    @Test
    @Tag("FR-024-08")
    void le_meme_message_brut_livre_deux_fois_n_a_qu_un_effet() throws Exception {
        long pretId = inserer("TRAVAUX", "12000.00", 24, "EN_ATTENTE");
        UUID idEvenement = UUID.randomUUID();
        String message = enveloppe(idEvenement, pretId);
        long sentinelle = inserer("TRAVAUX", "12000.00", 24, "EN_ATTENTE");

        // Même clé, donc même partition : quand la sentinelle est décidée, les deux livraisons sont passées.
        envoyer(pretId, message, message);
        envoyer(pretId, enveloppe(UUID.randomUUID(), sentinelle));
        attendreStatut(pretId, "EN_COURS");
        attendreStatut(sentinelle, "EN_COURS");

        assertThat(compter("select count(*) from loan_repayments where loan_id = :p", pretId)).isEqualTo(24L);
        assertThat(compter("select count(*) from outbox where topic = 'credits.pret-decide.v1' and cle = :p",
                Long.toString(pretId))).isEqualTo(1L);
        assertThat(compter("select count(*) from evenements_traites where id_evenement = :p", idEvenement))
                .isEqualTo(1L);
    }
}
