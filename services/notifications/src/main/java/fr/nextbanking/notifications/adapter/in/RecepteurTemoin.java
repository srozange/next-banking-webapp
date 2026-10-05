package fr.nextbanking.notifications.adapter.in;

import fr.nextbanking.notifications.application.port.in.RecevoirTemoin;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire du topic {@code coeur-bancaire.temoin-ecrit.v1}. Lit l'enveloppe
 * JSON (chaîne, sans en-tête de type) et confie le témoin au cas d'usage idempotent.
 */
@Component
public class RecepteurTemoin {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurTemoin.class);
    static final String TYPE = "temoin-ecrit";
    static final int VERSION = 1;

    private final RecevoirTemoin recevoirTemoin;
    private final JsonMapper json;

    public RecepteurTemoin(RecevoirTemoin recevoirTemoin, JsonMapper json) {
        this.recevoirTemoin = recevoirTemoin;
        this.json = json;
    }

    @KafkaListener(topics = "coeur-bancaire.temoin-ecrit.v1")
    public void recevoir(String message) {
        EnveloppeTemoinEcrit enveloppe = json.readValue(message, EnveloppeTemoinEcrit.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        if (!recevoirTemoin.recevoir(enveloppe.idEvenement(), enveloppe.donnees().temoinId())) {
            JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record EnveloppeTemoinEcrit(UUID idEvenement, String type, int version, String horodatage, String producteur,
            Donnees donnees) {

        /** Données de l'événement. */
        record Donnees(long temoinId) {
        }
    }
}
