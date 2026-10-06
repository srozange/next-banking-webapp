package fr.nextbanking.identiteclients.adapter.out.evenements;

import fr.nextbanking.identiteclients.application.port.out.PublicationEtatClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur sortant : outbox ({@code outbox}) et publication par {@link KafkaTemplate} sur
 * {@code identite-clients.client-modifie.v1}, clé = {@code clientId}. L'état est relu dans la transaction de
 * l'écriture, la ligne est publiée après validation puis marquée publiée ; un échec est journalisé et laisse la ligne
 * au relais.
 */
@Component
public class PublicationEtatClientKafka implements PublicationEtatClient {

    private static final Logger JOURNAL = LoggerFactory.getLogger(PublicationEtatClientKafka.class);
    private static final long DELAI_ENVOI_SECONDES = 10;

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public PublicationEtatClientKafka(JdbcClient jdbc, KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public long consigner(long clientId, UUID idEvenement, Instant horodatage) {
        EnveloppeClientModifie.Donnees donnees = jdbc.sql("""
                select id, username, full_name, role, active, advisor_id, date_creation, supprime, version_agregat
                from customers where id = :id""")
                .param("id", clientId)
                .query((ligne, numero) -> new EnveloppeClientModifie.Donnees(ligne.getLong("id"),
                        ligne.getString("username"), ligne.getString("full_name"), ligne.getString("role"),
                        ligne.getBoolean("active"), (Long) ligne.getObject("advisor_id", Long.class),
                        ligne.getDate("date_creation").toLocalDate().toString(), ligne.getBoolean("supprime"),
                        ligne.getLong("version_agregat")))
                .single();
        return jdbc.sql("""
                insert into outbox (id_evenement, topic, cle, charge)
                values (:idEvenement, :topic, :cle, :charge) returning id""")
                .param("idEvenement", idEvenement)
                .param("topic", EnveloppeClientModifie.TOPIC)
                .param("cle", Long.toString(clientId))
                .param("charge", enJson(idEvenement, horodatage, donnees))
                .query(Long.class)
                .single();
    }

    @Override
    public void publier(long idLigne) {
        jdbc.sql("select id, topic, cle, charge from outbox where id = :id and not publie")
                .param("id", idLigne)
                .query(LigneOutbox.class)
                .optional()
                .ifPresent(this::envoyer);
    }

    String enJson(UUID idEvenement, Instant horodatage, EnveloppeClientModifie.Donnees donnees) {
        return json.writeValueAsString(EnveloppeClientModifie.de(idEvenement, horodatage, donnees));
    }

    List<LigneOutbox> nonPubliees() {
        return jdbc.sql("select id, topic, cle, charge from outbox where not publie order by id limit 100")
                .query(LigneOutbox.class)
                .list();
    }

    boolean envoyer(LigneOutbox ligne) {
        try {
            kafka.send(ligne.topic(), ligne.cle(), ligne.charge()).get(DELAI_ENVOI_SECONDES, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            JOURNAL.error("Publication de la ligne d'outbox {} interrompue", ligne.id(), e);
            return false;
        } catch (Exception e) {
            JOURNAL.error("Échec de publication de la ligne d'outbox {} sur {} : laissée au relais",
                    ligne.id(), ligne.topic(), e);
            return false;
        }
        jdbc.sql("update outbox set publie = true where id = :id").param("id", ligne.id()).update();
        return true;
    }
}
