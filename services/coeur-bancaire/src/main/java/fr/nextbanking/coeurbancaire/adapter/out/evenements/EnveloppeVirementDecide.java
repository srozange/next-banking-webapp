package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.VirementDecide;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code coeur-bancaire.virement-decide.v1}
 * (schéma {@code contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json}).
 */
record EnveloppeVirementDecide(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "coeur-bancaire.virement-decide.v1";

    static EnveloppeVirementDecide de(VirementDecide evenement) {
        return new EnveloppeVirementDecide(
                evenement.idEvenement().toString(),
                "virement-decide",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "coeur-bancaire",
                new Donnees(evenement.virementId(), evenement.clientId(), evenement.etat().name(),
                        new Notification(evenement.notification().clientId(), evenement.notification().type().name(),
                                evenement.notification().titre(), evenement.notification().message())));
    }

    /** Données de l'événement. */
    record Donnees(long virementId, long clientId, String etat, Notification notification) {
    }

    /** Notification rendue par le producteur ; le titre est absent (null) tant qu'aucun texte n'est défini. */
    record Notification(long clientId, String type, String titre, String message) {
    }
}
