package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.publier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
 * Désactivation puis activation de user1 jusqu'au topic {@code identite-clients.client-modifie.v1}, sur une vraie
 * base et un vrai Kafka : clé {@code clientId}, sans en-tête de type, état complet, version croissante ; relais d'une
 * ligne restée non publiée.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-016-11")
class ClientModifieKafkaIT {

    private static final String TOPIC = "identite-clients.client-modifie.v1";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("nb.reseau.mandataires-de-confiance", () -> "127.0.0.1/32,::1/128");
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

    private record Ligne(long id, String cle, JsonNode enveloppe, boolean publie) {
    }

    private long dernierId() {
        return jdbc.sql("select coalesce(max(id), 0) from outbox").query(Long.class).single();
    }

    private List<Ligne> depuis(long id) {
        return jdbc.sql("select id, cle, charge, publie from outbox where id > :id order by id")
                .param("id", id)
                .query((ligne, numero) -> new Ligne(ligne.getLong("id"), ligne.getString("cle"),
                        json.readTree(ligne.getString("charge")), ligne.getBoolean("publie")))
                .list();
    }

    private HttpResponse<String> appeler(String chemin) throws Exception {
        return publier(port, chemin, OutilsHttp.jetonValide("10", "ADMIN"),
                Map.of("X-Forwarded-For", "203.0.113.7"));
    }

    private ConsumerRecord<String, String> publie(Ligne ligne) {
        String idEvenement = ligne.enveloppe().get("idEvenement").asString();
        return LecteurTopic.chercher(Conteneurs.KAFKA.getBootstrapServers(), TOPIC,
                message -> message.value().contains(idEvenement), Duration.ofSeconds(20)).orElseThrow();
    }

    @Test
    void la_desactivation_puis_l_activation_publient_l_etat_complet_avec_une_version_croissante() throws Exception {
        long depuis = dernierId();

        assertThat(appeler("/api/identite-clients/clients/1/desactivation").statusCode()).isEqualTo(204);
        assertThat(appeler("/api/identite-clients/clients/1/activation").statusCode()).isEqualTo(204);

        List<Ligne> lignes = depuis(depuis);
        assertThat(lignes).hasSize(2);
        JsonNode desactive = null;
        long versionPrecedente = 0;
        for (Ligne ligne : lignes) {
            assertThat(ligne.publie()).isTrue();
            ConsumerRecord<String, String> message = publie(ligne);
            assertThat(message.key()).isEqualTo("1");
            assertThat(message.headers().lastHeader("__TypeId__")).isNull();
            JsonNode enveloppe = json.readTree(message.value());
            assertThat(enveloppe.get("type").asString()).isEqualTo("client-modifie");
            assertThat(enveloppe.get("producteur").asString()).isEqualTo("identite-clients");
            assertThat(enveloppe.get("horodatage").asString()).endsWith("Z");
            JsonNode donnees = enveloppe.get("donnees");
            assertThat(donnees.get("clientId").asLong()).isEqualTo(1L);
            assertThat(donnees.get("login").asString()).isEqualTo("user1");
            assertThat(donnees.get("nomComplet").asString()).isEqualTo("Jean Dupont");
            assertThat(donnees.get("role").asString()).isEqualTo("CLIENT");
            assertThat(donnees.get("conseillerId").asLong()).isEqualTo(11L);
            assertThat(donnees.get("dateCreation").asString()).isNotBlank();
            assertThat(donnees.get("supprime").asBoolean()).isFalse();
            assertThat(donnees.get("versionAgregat").asLong()).isGreaterThan(versionPrecedente);
            versionPrecedente = donnees.get("versionAgregat").asLong();
            if (desactive == null) {
                desactive = donnees;
            }
        }
        assertThat(desactive.get("actif").asBoolean()).isFalse();
        assertThat(json.readTree(publie(lignes.get(1)).value()).get("donnees").get("actif").asBoolean()).isTrue();
    }

    @Test
    void le_relais_publie_une_ligne_restee_non_publiee() {
        long depuis = dernierId();
        UUID idEvenement = UUID.randomUUID();
        jdbc.sql("insert into outbox (id_evenement, topic, cle, charge) values (:id, :topic, 'relais', :charge)")
                .param("id", idEvenement)
                .param("topic", TOPIC)
                .param("charge", "{\"idEvenement\":\"" + idEvenement + "\"}")
                .update();

        await().atMost(Duration.ofSeconds(15)).until(() -> depuis(depuis).getFirst().publie());
        assertThat(publie(depuis(depuis).getFirst()).key()).isEqualTo("relais");
    }
}
