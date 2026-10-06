package fr.nextbanking.credits.adapter.out.evenements;

import fr.nextbanking.credits.domain.PretDemande;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code credits.pret-demande.v1}
 * (schéma {@code contrats/evenements/credits.pret-demande.v1.schema.json}).
 */
record EnveloppePretDemande(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "credits.pret-demande.v1";

    static EnveloppePretDemande de(PretDemande evenement) {
        return new EnveloppePretDemande(
                evenement.idEvenement().toString(),
                "pret-demande",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "credits",
                new Donnees(evenement.pretId()));
    }

    /** Données de l'événement. */
    record Donnees(long pretId) {
    }
}
