package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur sortant : outbox transactionnelle et publication par {@link KafkaTemplate}.
 * La ligne est écrite dans la transaction de l'écriture métier ; elle est publiée après validation,
 * puis marquée publiée. Un échec de publication est journalisé et laisse la ligne au relais.
 */
@Component
public class EvenementsSortantsKafka implements EvenementsSortants {

    private static final Logger JOURNAL = LoggerFactory.getLogger(EvenementsSortantsKafka.class);
    private static final long DELAI_ENVOI_SECONDES = 10;

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public EvenementsSortantsKafka(JdbcClient jdbc, KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public long consigner(TemoinEcrit evenement) {
        return jdbc.sql("""
                insert into outbox (id_evenement, topic, cle, charge)
                values (:idEvenement, :topic, :cle, :charge) returning id""")
                .param("idEvenement", evenement.idEvenement())
                .param("topic", EnveloppeTemoinEcrit.TOPIC)
                .param("cle", Long.toString(evenement.temoinId()))
                .param("charge", enJson(evenement))
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

    String enJson(TemoinEcrit evenement) {
        return json.writeValueAsString(EnveloppeTemoinEcrit.de(evenement));
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
