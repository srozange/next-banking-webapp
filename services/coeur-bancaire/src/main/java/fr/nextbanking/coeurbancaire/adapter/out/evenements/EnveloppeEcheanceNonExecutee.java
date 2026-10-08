package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.EcheanceNonExecutee;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code coeur-bancaire.echeance-non-executee.v1}
 * (schéma {@code contrats/evenements/coeur-bancaire.echeance-non-executee.v1.schema.json}).
 */
record EnveloppeEcheanceNonExecutee(String idEvenement, String type, int version, String horodatage,
        String producteur, Donnees donnees) {

    static final String TOPIC = "coeur-bancaire.echeance-non-executee.v1";

    static EnveloppeEcheanceNonExecutee de(EcheanceNonExecutee evenement) {
        return new EnveloppeEcheanceNonExecutee(
                evenement.idEvenement().toString(),
                "echeance-non-executee",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "coeur-bancaire",
                new Donnees(evenement.virementProgrammeId(),
                        new Notification(evenement.notification().clientId(), evenement.notification().type().name(),
                                evenement.notification().titre(), evenement.notification().message())));
    }

    /** Données de l'événement. */
    record Donnees(long virementProgrammeId, Notification notification) {
    }

    /** Notification rendue par le producteur, titre compris. */
    record Notification(long clientId, String type, String titre, String message) {
    }
}
