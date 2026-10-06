package fr.nextbanking.identiteclients.adapter.out.evenements;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Enveloppe JSON de l'événement {@code identite-clients.client-modifie.v1}
 * (schéma {@code contrats/evenements/identite-clients.client-modifie.v1.schema.json}).
 */
record EnveloppeClientModifie(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "identite-clients.client-modifie.v1";

    static EnveloppeClientModifie de(UUID idEvenement, Instant horodatage, Donnees donnees) {
        return new EnveloppeClientModifie(idEvenement.toString(), "client-modifie", 1,
                DateTimeFormatter.ISO_INSTANT.format(horodatage), "identite-clients", donnees);
    }

    /** État complet du client ; {@code conseillerId} est écrit {@code null} quand il est absent. */
    record Donnees(long clientId, String login, String nomComplet, String role, boolean actif, Long conseillerId,
            String dateCreation, boolean supprime, long versionAgregat) {
    }
}
