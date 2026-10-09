package fr.nextbanking.documents.adapter.in.evenements;

import fr.nextbanking.documents.application.port.in.RecevoirClientModifie;
import fr.nextbanking.documents.domain.ClientProjete;
import fr.nextbanking.documents.domain.Role;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur entrant : écouteur unitaire de {@code identite-clients.client-modifie.v1} (schéma
 * {@code contrats/evenements/identite-clients.client-modifie.v1.schema.json}). Lit l'enveloppe JSON (chaîne, sans
 * en-tête de type), ignore un type ou une version inattendus, et confie l'état du client au cas d'usage idempotent qui
 * alimente la projection locale.
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

    @KafkaListener(topics = TOPIC, groupId = "documents")
    public void recevoir(String message) {
        Enveloppe enveloppe = json.readValue(message, Enveloppe.class);
        if (!TYPE.equals(enveloppe.type()) || enveloppe.version() != VERSION) {
            JOURNAL.warn("Événement {} ignoré : type {} version {} inattendus",
                    enveloppe.idEvenement(), enveloppe.type(), enveloppe.version());
            return;
        }
        Donnees d = enveloppe.donnees();
        ClientProjete client = new ClientProjete(d.clientId(), d.login(), d.nomComplet(), d.role(), d.actif(),
                d.conseillerId(), d.dateCreation(), d.supprime(), d.versionAgregat());
        switch (recevoirClientModifie.recevoir(enveloppe.idEvenement(), client)) {
            case DEJA_TRAITE -> JOURNAL.info("Événement {} déjà traité : ignoré", enveloppe.idEvenement());
            case IGNORE_ANCIEN -> JOURNAL.info("Événement {} : version {} du client {} ancienne, ignorée",
                    enveloppe.idEvenement(), d.versionAgregat(), d.clientId());
            case ENREGISTRE -> JOURNAL.debug("Client {} enregistré en version {}", d.clientId(), d.versionAgregat());
        }
    }

    /** Enveloppe lue ; les champs non utilisés sont ignorés. */
    record Enveloppe(UUID idEvenement, String type, int version, Donnees donnees) {
    }

    /** Les neuf champs de l'état du client. */
    record Donnees(long clientId, String login, String nomComplet, Role role, boolean actif, Long conseillerId,
            LocalDate dateCreation, boolean supprime, long versionAgregat) {
    }
}
