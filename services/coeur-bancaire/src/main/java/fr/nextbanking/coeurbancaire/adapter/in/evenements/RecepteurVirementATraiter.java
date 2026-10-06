package fr.nextbanking.coeurbancaire.adapter.in.evenements;

import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire du topic interne {@code coeur-bancaire.virement-a-traiter.v1}, produit et
 * consommé par le cœur bancaire (débit d'un virement libéré). Lit l'enveloppe JSON (chaîne, sans en-tête de type), ignore un type ou une version
 * inattendus, et confie le message au cas d'usage idempotent. Il n'émet aucune notification.
 */
@Component
public class RecepteurVirementATraiter {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurVirementATraiter.class);
    static final String TOPIC = "coeur-bancaire.virement-a-traiter.v1";
    static final String TYPE = "virement-a-traiter";
    static final int VERSION = 1;

    private final TraiterVirementATraiter traiterVirementATraiter;
    private final JsonMapper json;

    public RecepteurVirementATraiter(TraiterVirementATraiter traiterVirementATraiter, JsonMapper json) {
        this.traiterVirementATraiter = traiterVirementATraiter;
        this.json = json;
    }

    @KafkaListener(topics = TOPIC)
    public void recevoir(String message) {
        Enveloppe enveloppe = json.readValue(message, Enveloppe.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        switch (traiterVirementATraiter.traiter(enveloppe.idEvenement(), enveloppe.donnees().virementId())) {
            case DEJA_TRAITE -> JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
            case VIREMENT_INCONNU -> JOURNAL.warn("Événement {} : virement {} inconnu, ignoré",
                    enveloppe.idEvenement(), enveloppe.donnees().virementId());
            case DEJA_EXECUTE -> JOURNAL.debug("Virement {} déjà exécuté : aucun effet", enveloppe.donnees().virementId());
            case ETAT_INATTENDU -> JOURNAL.warn("Événement {} : virement {} dans un état inattendu, ignoré",
                    enveloppe.idEvenement(), enveloppe.donnees().virementId());
            case EXECUTE_APRES_LIBERATION -> JOURNAL.info("Virement {} libéré : débité et exécuté",
                    enveloppe.donnees().virementId());
            case REJETE_SOLDE_INSUFFISANT -> JOURNAL.info("Virement {} libéré : rejeté, solde insuffisant au débit",
                    enveloppe.donnees().virementId());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record Enveloppe(UUID idEvenement, String type, int version, Donnees donnees) {

        /** Données du message : seul l'identifiant du virement sert au traitement. */
        record Donnees(long virementId) {
        }
    }
}
