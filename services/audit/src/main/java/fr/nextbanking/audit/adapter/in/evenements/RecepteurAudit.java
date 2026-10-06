package fr.nextbanking.audit.adapter.in.evenements;

import fr.nextbanking.audit.application.port.in.EnregistrerEntreeAudit;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire des topics d'audit ({@code identite-clients.audit.v1}). Lit l'enveloppe
 * JSON (chaîne, sans en-tête de type), ignore un type ou une version inattendus, et confie l'entrée au cas d'usage
 * idempotent avec l'horodatage de l'enveloppe. Un code hors catalogue est rejeté et journalisé.
 */
@Component
public class RecepteurAudit {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurAudit.class);
    static final String TYPE = "audit";
    static final int VERSION = 1;

    private final EnregistrerEntreeAudit enregistrerEntreeAudit;
    private final JsonMapper json;

    public RecepteurAudit(EnregistrerEntreeAudit enregistrerEntreeAudit, JsonMapper json) {
        this.enregistrerEntreeAudit = enregistrerEntreeAudit;
        this.json = json;
    }

    @KafkaListener(topics = "identite-clients.audit.v1")
    public void recevoir(String message) {
        EnveloppeAudit enveloppe = json.readValue(message, EnveloppeAudit.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        EnveloppeAudit.Donnees donnees = enveloppe.donnees();
        EnregistrerEntreeAudit.Issue issue = enregistrerEntreeAudit.enregistrer(enveloppe.idEvenement(),
                donnees.utilisateur(), donnees.action(), donnees.detail(), donnees.adresseIp(),
                Instant.parse(enveloppe.horodatage()));
        switch (issue) {
            case CODE_INCONNU -> JOURNAL.warn("Code d'audit inconnu rejeté : {}", donnees.action());
            case DEJA_TRAITEE -> JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
            case ENREGISTREE -> JOURNAL.debug("Événement {} enregistré", enveloppe.idEvenement());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record EnveloppeAudit(UUID idEvenement, String type, int version, String horodatage, String producteur,
            Donnees donnees) {

        /** Données de l'entrée d'audit. */
        record Donnees(String utilisateur, String action, String detail, String adresseIp) {
        }
    }
}
