package fr.nextbanking.credits.adapter.out.evenements;

import fr.nextbanking.credits.domain.Notification;
import fr.nextbanking.credits.domain.PretDecide;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code credits.pret-decide.v1}
 * (schéma {@code contrats/evenements/credits.pret-decide.v1.schema.json}).
 */
record EnveloppePretDecide(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "credits.pret-decide.v1";

    static EnveloppePretDecide de(PretDecide evenement) {
        Notification notification = evenement.notification();
        return new EnveloppePretDecide(
                evenement.idEvenement().toString(),
                "pret-decide",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "credits",
                new Donnees(evenement.pretId(), evenement.clientId(), evenement.statut().name(),
                        new DonneesNotification(notification.clientId(), notification.type().name(),
                                notification.titre(), notification.message())));
    }

    /** Données de l'événement. */
    record Donnees(long pretId, long clientId, String statut, DonneesNotification notification) {
    }

    /** Notification à stocker par le centre de notifications. */
    record DonneesNotification(long clientId, String type, String titre, String message) {
    }
}
