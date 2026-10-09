package fr.nextbanking.documents.adapter.in.evenements;

import fr.nextbanking.documents.application.port.in.RecevoirOperationEnregistree;
import fr.nextbanking.documents.domain.OperationProjetee;
import fr.nextbanking.documents.domain.TypeOperation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire de {@code coeur-bancaire.operation-enregistree.v1} (schéma
 * {@code contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json}). Lit l'enveloppe JSON (chaîne, sans
 * en-tête de type), ignore un type ou une version inattendus, et confie l'opération au cas d'usage idempotent qui
 * alimente la projection locale. Seul l'identifiant de l'opération est journalisé : jamais le libellé.
 */
@Component
public class RecepteurOperationEnregistree {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurOperationEnregistree.class);
    static final String TOPIC = "coeur-bancaire.operation-enregistree.v1";
    static final String TYPE = "operation-enregistree";
    static final int VERSION = 1;

    private final RecevoirOperationEnregistree recevoirOperationEnregistree;
    private final JsonMapper json;

    public RecepteurOperationEnregistree(RecevoirOperationEnregistree recevoirOperationEnregistree, JsonMapper json) {
        this.recevoirOperationEnregistree = recevoirOperationEnregistree;
        this.json = json;
    }

    @KafkaListener(topics = TOPIC, groupId = "documents")
    public void recevoir(String message) {
        Enveloppe enveloppe = json.readValue(message, Enveloppe.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        Donnees d = enveloppe.donnees();
        OperationProjetee operation = new OperationProjetee(d.operationId(), d.compteId(), d.instant(), d.libelle(),
                d.type(), d.montant(), d.soldeApres(), d.versionAgregat());
        switch (recevoirOperationEnregistree.recevoir(enveloppe.idEvenement(), operation)) {
            case DEJA_TRAITE -> JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
            case DEJA_PRESENTE -> JOURNAL.info("Événement {} : opération {} déjà présente, ignorée",
                    enveloppe.idEvenement(), d.operationId());
            case ENREGISTRE -> JOURNAL.debug("Opération {} enregistrée", d.operationId());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record Enveloppe(UUID idEvenement, String type, int version, Donnees donnees) {
    }

    /** Les huit champs de l'opération ; montant et solde après sont des chaînes à deux décimales dans le message. */
    record Donnees(long operationId, long compteId, Instant instant, String libelle, TypeOperation type,
            BigDecimal montant, BigDecimal soldeApres, long versionAgregat) {
    }
}
