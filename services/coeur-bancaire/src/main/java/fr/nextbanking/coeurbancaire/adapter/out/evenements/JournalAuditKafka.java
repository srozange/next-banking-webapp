package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur sortant : outbox d'audit ({@code outbox_audit}) et publication par {@link KafkaTemplate} sur
 * {@code coeur-bancaire.audit.v1}, clé = identifiant du virement. La ligne est écrite dans la transaction ouverte par
 * le cas d'usage (indépendante du virement) ; elle est publiée après validation, puis marquée publiée. Un échec de
 * publication est journalisé et laisse la ligne au relais.
 */
@Component
public class JournalAuditKafka implements JournalAudit {

    private static final Logger JOURNAL = LoggerFactory.getLogger(JournalAuditKafka.class);
    private static final long DELAI_ENVOI_SECONDES = 10;

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public JournalAuditKafka(JdbcClient jdbc, KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.json = json;
    }

    @Override
    public long consigner(EntreeAudit entree) {
        return jdbc.sql("""
                insert into outbox_audit (id_evenement, topic, cle, charge)
                values (:idEvenement, :topic, :cle, :charge) returning id""")
                .param("idEvenement", entree.idEvenement())
                .param("topic", EnveloppeAudit.TOPIC)
                .param("cle", Long.toString(entree.virementId()))
                .param("charge", enJson(entree))
                .query(Long.class)
                .single();
    }

    @Override
    public void publier(long idLigne) {
        jdbc.sql("select id, topic, cle, charge from outbox_audit where id = :id and not publie")
                .param("id", idLigne)
                .query(LigneOutboxAudit.class)
                .optional()
                .ifPresent(this::envoyer);
    }

    String enJson(EntreeAudit entree) {
        return json.writeValueAsString(EnveloppeAudit.de(entree));
    }

    List<LigneOutboxAudit> nonPubliees() {
        return jdbc.sql("select id, topic, cle, charge from outbox_audit where not publie order by id limit 100")
                .query(LigneOutboxAudit.class)
                .list();
    }

    boolean envoyer(LigneOutboxAudit ligne) {
        try {
            kafka.send(ligne.topic(), ligne.cle(), ligne.charge()).get(DELAI_ENVOI_SECONDES, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            JOURNAL.error("Publication de la ligne d'outbox d'audit {} interrompue", ligne.id(), e);
            return false;
        } catch (Exception e) {
            JOURNAL.error("Échec de publication de la ligne d'outbox d'audit {} sur {} : laissée au relais",
                    ligne.id(), ligne.topic(), e);
            return false;
        }
        jdbc.sql("update outbox_audit set publie = true where id = :id").param("id", ligne.id()).update();
        return true;
    }
}
