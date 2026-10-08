package fr.nextbanking.notifications.adapter.in;

import fr.nextbanking.notifications.application.port.in.RecevoirEcheanceNonExecutee;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire du topic {@code coeur-bancaire.echeance-non-executee.v1}. Lit l'enveloppe JSON
 * (chaîne, sans en-tête de type) et confie la notification rendue au cas d'usage idempotent.
 */
@Component
public class RecepteurEcheanceNonExecutee {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurEcheanceNonExecutee.class);
    static final String TYPE = "echeance-non-executee";
    static final int VERSION = 1;

    private final RecevoirEcheanceNonExecutee recevoir;
    private final JsonMapper json;

    public RecepteurEcheanceNonExecutee(RecevoirEcheanceNonExecutee recevoir, JsonMapper json) {
        this.recevoir = recevoir;
        this.json = json;
    }

    @KafkaListener(topics = "coeur-bancaire.echeance-non-executee.v1")
    public void recevoir(String message) {
        EnveloppeVirementDecide enveloppe = json.readValue(message, EnveloppeVirementDecide.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        Donnees donnees = enveloppe.donnees();
        Notification notification = donnees.notification();
        if (!recevoir.recevoir(enveloppe.idEvenement(), donnees.virementProgrammeId(),
                new RecevoirEcheanceNonExecutee.Notification(notification.clientId(), notification.type(),
                        notification.titre(), notification.message()))) {
            JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record EnveloppeVirementDecide(UUID idEvenement, String type, int version, String horodatage, String producteur,
            Donnees donnees) {
    }

    /** Données de l'événement. */
    record Donnees(long virementProgrammeId, Notification notification) {
    }

    /** Notification rendue par le producteur. */
    record Notification(long clientId, String type, String titre, String message) {
    }
}
