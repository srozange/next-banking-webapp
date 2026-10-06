package fr.nextbanking.audit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.audit.application.port.out.EvenementsTraites;
import fr.nextbanking.audit.application.port.out.JournalAudit;
import fr.nextbanking.audit.application.port.out.Transaction;
import fr.nextbanking.audit.domain.ActionAudit;
import fr.nextbanking.audit.domain.EntreeAudit;
import fr.nextbanking.audit.infrastructure.ApplicationAudit;
import fr.nextbanking.audit.infrastructure.Conteneurs;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Réception sur une vraie base et un vrai Kafka : messages bruts de {@code identite-clients.audit.v1} (chaîne JSON
 * de l'enveloppe, sans en-tête de type). Un message livré deux fois n'a qu'un effet ; un code hors catalogue n'écrit
 * rien et est journalisé ; {@code action_date} est l'horodatage de l'enveloppe, jamais l'heure de réception.
 */
@SpringBootTest(classes = ApplicationAudit.class)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
@Tag("FR-026-01")
class ReceptionAuditIT {

    private static final String TOPIC = "identite-clients.audit.v1";
    private static final String TOPIC_COEUR_BANCAIRE = "coeur-bancaire.audit.v1";
    private static final String HORODATAGE_PASSE = "2026-10-05T08:30:00Z";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private Transaction transaction;

    @Autowired
    private EvenementsTraites evenementsTraites;

    @Autowired
    private JournalAudit journal;

    // Le topic est déclaré par son producteur (identite-clients) : le test le crée à sa place.
    @BeforeAll
    static void creerLeTopic() throws Exception {
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                Conteneurs.KAFKA.getBootstrapServers()))) {
            for (String topic : List.of(TOPIC, TOPIC_COEUR_BANCAIRE)) {
                try {
                    admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get();
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof TopicExistsException)) {
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    void un_message_livre_deux_fois_n_ecrit_qu_une_ligne_avec_l_horodatage_de_l_enveloppe() throws Exception {
        String utilisateur = "u-" + UUID.randomUUID();
        UUID idEvenement = UUID.randomUUID();
        UUID idFin = UUID.randomUUID();

        envoyer(utilisateur, enveloppe(idEvenement, utilisateur, "LOGIN", "Connexion réussie"),
                enveloppe(idEvenement, utilisateur, "LOGIN", "Connexion réussie"),
                // Même clé, donc même partition : quand ce dernier message est traité, les deux autres l'ont été.
                enveloppe(idFin, utilisateur, "LOGIN_FAILED", "Tentative échouée"));

        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        assertThat(traite(idEvenement)).isTrue();
        List<Ligne> lignes = lignes(utilisateur);
        assertThat(lignes).extracting(Ligne::action).containsExactly("LOGIN", "LOGIN_FAILED");
        Ligne login = lignes.getFirst();
        assertThat(login.details()).isEqualTo("Connexion réussie");
        assertThat(login.ipaddress()).isEqualTo("203.0.113.7");
        assertThat(login.actionDate().toInstant()).isEqualTo(java.time.Instant.parse(HORODATAGE_PASSE));
    }

    @Test
    @Tag("FR-026-03")
    void les_entrees_d_un_virement_du_coeur_bancaire_sont_ecrites_dans_l_ordre_de_leur_cle() throws Exception {
        String virement = "v-" + UUID.randomUUID();
        UUID idFin = UUID.randomUUID();

        envoyerSur(TOPIC_COEUR_BANCAIRE, virement,
                enveloppe(UUID.randomUUID(), "1", "VIREMENT", "Montant: 100.00 - Motif: loyer - Issue: EXECUTE"),
                enveloppe(idFin, "1", "TRANSFER_EXECUTED", "Montant: 100.00 - Motif: loyer"));

        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        assertThat(lignesDe("1", "Montant: 100.00 - Motif: loyer%"))
                .extracting(Ligne::action).containsSubsequence("VIREMENT", "TRANSFER_EXECUTED");
    }

    @Test
    void un_code_hors_catalogue_n_ecrit_aucune_ligne_et_est_journalise(CapturedOutput sortie) throws Exception {
        String utilisateur = "u-" + UUID.randomUUID();
        UUID idFin = UUID.randomUUID();

        envoyer(utilisateur, enveloppe(UUID.randomUUID(), utilisateur, "CODE_INCONNU", "x"),
                enveloppe(idFin, utilisateur, "LOGOUT", "Déconnexion"));

        await().atMost(Duration.ofSeconds(30)).until(() -> traite(idFin));
        assertThat(lignes(utilisateur)).extracting(Ligne::action).containsExactly("LOGOUT");
        assertThat(sortie.getOut()).contains("Code d'audit inconnu rejeté : CODE_INCONNU");
    }

    @Test
    void une_transaction_annulee_ne_marque_pas_l_evenement_ni_n_ecrit_la_ligne() {
        String utilisateur = "u-" + UUID.randomUUID();
        UUID idEvenement = UUID.randomUUID();

        assertThatThrownBy(() -> transaction.executer(() -> {
            evenementsTraites.marquerTraite(idEvenement);
            journal.enregistrer(new EntreeAudit(ActionAudit.LOGIN, java.time.Instant.parse(HORODATAGE_PASSE),
                    "Connexion réussie", "203.0.113.7", utilisateur));
            throw new IllegalStateException("annulation voulue");
        })).hasMessage("annulation voulue");

        assertThat(traite(idEvenement)).isFalse();
        assertThat(lignes(utilisateur)).isEmpty();
    }

    private record Ligne(String action, OffsetDateTime actionDate, String details, String ipaddress) {
    }

    private List<Ligne> lignes(String utilisateur) {
        return jdbc.sql("select action, action_date, details, ipaddress from audit_logs where username = :u order by id")
                .param("u", utilisateur)
                .query((ligne, numero) -> new Ligne(ligne.getString("action"),
                        ligne.getObject("action_date", OffsetDateTime.class), ligne.getString("details"),
                        ligne.getString("ipaddress")))
                .list();
    }

    private boolean traite(UUID idEvenement) {
        return jdbc.sql("select count(*) from evenements_traites where id_evenement = :id").param("id", idEvenement)
                .query(Long.class).single() == 1L;
    }

    private List<Ligne> lignesDe(String utilisateur, String detailParmi) {
        return jdbc.sql("select action, action_date, details, ipaddress from audit_logs where username = :u"
                        + " and details like :d order by id")
                .param("u", utilisateur).param("d", detailParmi)
                .query((ligne, numero) -> new Ligne(ligne.getString("action"),
                        ligne.getObject("action_date", OffsetDateTime.class), ligne.getString("details"),
                        ligne.getString("ipaddress")))
                .list();
    }

    private static void envoyer(String cle, String... messages) throws Exception {
        envoyerSur(TOPIC, cle, messages);
    }

    private static void envoyerSur(String topic, String cle, String... messages) throws Exception {
        try (KafkaProducer<String, String> producteur = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Conteneurs.KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            for (String message : messages) {
                producteur.send(new ProducerRecord<>(topic, cle, message)).get();
            }
        }
    }

    private static String enveloppe(UUID idEvenement, String utilisateur, String action, String detail) {
        return """
                {"idEvenement":"%s","type":"audit","version":1,"horodatage":"%s","producteur":"identite-clients",\
                "donnees":{"utilisateur":"%s","action":"%s","detail":"%s","adresseIp":"203.0.113.7"}}"""
                .formatted(idEvenement, HORODATAGE_PASSE, utilisateur, action, detail);
    }
}
