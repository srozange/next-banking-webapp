package fr.nextbanking.credits.adapter.in.evenements;

import fr.nextbanking.credits.application.port.in.DecisionPret;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire du topic {@code credits.pret-demande.v1}. Lit l'enveloppe JSON (chaîne, sans
 * en-tête de type) et confie la demande au cas d'usage de décision, idempotent.
 */
@Component
public class RecepteurPretDemande {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurPretDemande.class);
    static final String TYPE = "pret-demande";
    static final int VERSION = 1;

    private final DecisionPret decisionPret;
    private final JsonMapper json;

    public RecepteurPretDemande(DecisionPret decisionPret, JsonMapper json) {
        this.decisionPret = decisionPret;
        this.json = json;
    }

    @KafkaListener(topics = "credits.pret-demande.v1")
    public void recevoir(String message) {
        EnveloppePretDemande enveloppe = json.readValue(message, EnveloppePretDemande.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        if (!decisionPret.decider(enveloppe.idEvenement(), enveloppe.donnees().pretId())) {
            JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record EnveloppePretDemande(UUID idEvenement, String type, int version, String horodatage, String producteur,
            Donnees donnees) {

        /** Données de l'événement. */
        record Donnees(long pretId) {
        }
    }
}
