package fr.nextbanking.coeurbancaire.adapter.in.evenements;

import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie;
import fr.nextbanking.coeurbancaire.domain.ClientProjete;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire de {@code identite-clients.client-modifie.v1} (schéma
 * {@code contrats/evenements/identite-clients.client-modifie.v1.schema.json}). Lit l'enveloppe JSON (chaîne, sans
 * en-tête de type), ignore un type ou une version inattendus, et confie le nom complet, le conseiller affecté et la suppression au cas d'usage idempotent qui
 * alimente la projection locale des clients. Les champs non utilisés sont ignorés.
 */
@Component
public class RecepteurClientModifie {

    private static final Logger JOURNAL = LoggerFactory.getLogger(RecepteurClientModifie.class);
    static final String TOPIC = "identite-clients.client-modifie.v1";
    static final String TYPE = "client-modifie";
    static final int VERSION = 1;

    private final RecevoirClientModifie recevoirClientModifie;
    private final JsonMapper json;

    public RecepteurClientModifie(RecevoirClientModifie recevoirClientModifie, JsonMapper json) {
        this.recevoirClientModifie = recevoirClientModifie;
        this.json = json;
    }

    @KafkaListener(topics = TOPIC, groupId = "coeur-bancaire-clients")
    public void recevoir(String message) {
        Enveloppe enveloppe = json.readValue(message, Enveloppe.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        Donnees donnees = enveloppe.donnees();
        switch (recevoirClientModifie.recevoir(enveloppe.idEvenement(), new ClientProjete(donnees.clientId(),
                donnees.nomComplet(), donnees.conseillerId(), donnees.supprime(), donnees.versionAgregat()))) {
            case DEJA_TRAITE -> JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
            case IGNORE_ANCIEN -> JOURNAL.info("Événement {} : version {} du client {} ancienne, ignorée",
                    enveloppe.idEvenement(), donnees.versionAgregat(), donnees.clientId());
            case ENREGISTRE -> JOURNAL.debug("Client {} enregistré en version {}", donnees.clientId(),
                    donnees.versionAgregat());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record Enveloppe(UUID idEvenement, String type, int version, Donnees donnees) {
    }

    /** Données utilisées : identifiant, nom complet, conseiller affecté (nul possible), suppression et version. */
    record Donnees(long clientId, String nomComplet, Long conseillerId, boolean supprime, long versionAgregat) {
    }
}
