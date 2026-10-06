package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.MDP_CLIENT;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.connexion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
 * Connexions par HTTP jusqu'au topic {@code identite-clients.audit.v1}, sur une vraie base et un vrai Kafka :
 * LOGIN pour une connexion réussie, LOGIN_FAILED pour un compte inconnu et pour un compte désactivé, rien pour un
 * champ requis manquant ; messages en chaîne JSON, sans en-tête de type.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-001-12")
@Tag("FR-026-01")
class AuditKafkaIT {

    private static final String TOPIC = "identite-clients.audit.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    @Test
    void une_connexion_reussie_publie_login_avec_la_cle_client_id() throws Exception {
        long depuis = LignesAudit.dernierId(jdbc);

        assertThat(connexion(port, "{\"identifiant\":\"user1\",\"motDePasse\":\"" + MDP_CLIENT + "\"}").statusCode())
                .isEqualTo(200);

        LignesAudit.Ligne ligne = LignesAudit.depuis(jdbc, depuis).getFirst();
        assertThat(ligne.publie()).isTrue();
        ConsumerRecord<String, String> message = publie(ligne);
        assertThat(message.key()).isEqualTo("1");
        assertThat(message.headers().lastHeader("__TypeId__")).isNull();
        JsonNode enveloppe = json.readTree(message.value());
        assertThat(enveloppe.get("type").asString()).isEqualTo("audit");
        assertThat(enveloppe.get("version").asInt()).isEqualTo(1);
        assertThat(enveloppe.get("producteur").asString()).isEqualTo("identite-clients");
        assertThat(Instant.parse(enveloppe.get("horodatage").asString())).isNotNull();
        assertThat(enveloppe.get("horodatage").asString()).endsWith("Z");
        JsonNode donnees = enveloppe.get("donnees");
        assertThat(donnees.get("utilisateur").asString()).isEqualTo("1");
        assertThat(donnees.get("action").asString()).isEqualTo("LOGIN");
        assertThat(donnees.get("detail").asString()).isEqualTo("Connexion réussie");
        assertThat(donnees.get("adresseIp").asString()).isNotBlank();
    }

    @Test
    void un_compte_inconnu_et_un_compte_desactive_publient_login_failed_avec_l_identifiant_saisi() throws Exception {
        jdbc.sql("update customers set active = false where username = 'user1'").update();
        long depuis = LignesAudit.dernierId(jdbc);

        assertThat(connexion(port,
                "{\"identifiant\":\"retro-compte-inexistant\",\"motDePasse\":\"retro-mauvais-mot-de-passe\"}")
                .statusCode()).isEqualTo(401);
        assertThat(connexion(port, "{\"identifiant\":\"user1\",\"motDePasse\":\"" + MDP_CLIENT + "\"}").statusCode())
                .isEqualTo(401);

        List<LignesAudit.Ligne> lignes = LignesAudit.depuis(jdbc, depuis);
        assertThat(lignes).extracting(LignesAudit.Ligne::cle).containsExactly("retro-compte-inexistant", "user1");
        for (LignesAudit.Ligne ligne : lignes) {
            JsonNode donnees = json.readTree(publie(ligne).value()).get("donnees");
            assertThat(donnees.get("utilisateur").asString()).isEqualTo(ligne.cle());
            assertThat(donnees.get("action").asString()).isEqualTo("LOGIN_FAILED");
            assertThat(donnees.get("detail").asString()).isEqualTo("Tentative échouée");
            assertThat(donnees.get("adresseIp").asString()).isNotBlank();
        }
    }

    @Test
    void un_champ_requis_manquant_n_est_pas_audite() throws Exception {
        long depuis = LignesAudit.dernierId(jdbc);

        assertThat(connexion(port, "{\"identifiant\":\"\",\"motDePasse\":\"x\"}").statusCode()).isEqualTo(400);
        assertThat(connexion(port, "{\"identifiant\":\"user1\",\"motDePasse\":\"\"}").statusCode()).isEqualTo(400);

        assertThat(LignesAudit.depuis(jdbc, depuis)).isEmpty();
    }

    @Test
    void le_relais_publie_une_ligne_restee_non_publiee() {
        long depuis = LignesAudit.dernierId(jdbc);
        UUID idEvenement = UUID.randomUUID();
        jdbc.sql("insert into outbox_audit (id_evenement, topic, cle, charge) values (:id, :topic, 'relais', :charge)")
                .param("id", idEvenement)
                .param("topic", TOPIC)
                .param("charge", "{\"idEvenement\":\"" + idEvenement + "\"}")
                .update();

        await().atMost(Duration.ofSeconds(15)).until(() -> LignesAudit.depuis(jdbc, depuis).getFirst().publie());
        assertThat(publie(LignesAudit.depuis(jdbc, depuis).getFirst()).key()).isEqualTo("relais");
    }

    private ConsumerRecord<String, String> publie(LignesAudit.Ligne ligne) {
        String idEvenement = ligne.enveloppe().get("idEvenement").asString();
        return LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(), TOPIC,
                message -> message.value().contains(idEvenement), Duration.ofSeconds(20)).orElseThrow();
    }
}
