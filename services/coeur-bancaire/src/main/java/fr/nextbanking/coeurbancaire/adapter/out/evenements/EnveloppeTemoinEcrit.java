package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code coeur-bancaire.temoin-ecrit.v1}
 * (schéma {@code contrats/evenements/coeur-bancaire.temoin-ecrit.v1.schema.json}).
 */
record EnveloppeTemoinEcrit(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "coeur-bancaire.temoin-ecrit.v1";

    static EnveloppeTemoinEcrit de(TemoinEcrit evenement) {
        return new EnveloppeTemoinEcrit(
                evenement.idEvenement().toString(),
                "temoin-ecrit",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "coeur-bancaire",
                new Donnees(evenement.temoinId()));
    }

    /** Données de l'événement. */
    record Donnees(long temoinId) {
    }
}
