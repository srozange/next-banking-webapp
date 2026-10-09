package fr.nextbanking.documents.adapter.in.evenements;

import fr.nextbanking.documents.application.port.in.RecevoirCompteModifie;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.TypeCompte;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire de {@code coeur-bancaire.compte-modifie.v1} (schéma
 * {@code contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json}). Lit l'enveloppe JSON (chaîne, sans
 * en-tête de type), ignore un type ou une version inattendus, et confie l'état du compte au cas d'usage idempotent qui
 * alimente la projection locale. Le numéro de compte et l'IBAN ne sont jamais journalisés en clair.
 */
@Component
public class RecepteurCompteModifie {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurCompteModifie.class);
    static final String TOPIC = "coeur-bancaire.compte-modifie.v1";
    static final String TYPE = "compte-modifie";
    static final int VERSION = 1;

    private final RecevoirCompteModifie recevoirCompteModifie;
    private final JsonMapper json;

    public RecepteurCompteModifie(RecevoirCompteModifie recevoirCompteModifie, JsonMapper json) {
        this.recevoirCompteModifie = recevoirCompteModifie;
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
        CompteProjete compte = new CompteProjete(d.compteId(), d.clientId(), d.numero(), d.type(), d.iban(), d.bic(),
                d.solde(), d.versionAgregat());
        switch (recevoirCompteModifie.recevoir(enveloppe.idEvenement(), compte)) {
            case DEJA_TRAITE -> JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
            case IGNORE_ANCIEN -> JOURNAL.info("Événement {} : version {} du compte {} ancienne, ignorée",
                    enveloppe.idEvenement(), d.versionAgregat(), d.compteId());
            case ENREGISTRE -> JOURNAL.debug("Compte {} enregistré en version {}", d.compteId(), d.versionAgregat());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record Enveloppe(UUID idEvenement, String type, int version, Donnees donnees) {
    }

    /** Les huit champs de l'état du compte ; le solde est une chaîne à deux décimales dans le message. */
    record Donnees(long compteId, long clientId, String numero, TypeCompte type, String iban, String bic,
            BigDecimal solde, long versionAgregat) {
    }
}
