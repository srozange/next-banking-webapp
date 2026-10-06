package fr.nextbanking.credits.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement : le scoring a décidé d'une demande de prêt ; statut EN_COURS ou REFUSE, notification rendue pour le
 * client.
 */
public record PretDecide(UUID idEvenement, long pretId, long clientId, StatutPret statut, Notification notification,
        Instant horodatage) implements EvenementSortant {

    public PretDecide {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        if (statut != StatutPret.EN_COURS && statut != StatutPret.REFUSE) {
            throw new IllegalArgumentException("Le statut décidé est EN_COURS ou REFUSE.");
        }
        if (horodatage == null) {
            throw new IllegalArgumentException("L'horodatage de l'événement est obligatoire.");
        }
    }

    /** Événement de la décision prise sur le prêt. */
    public static PretDecide de(UUID idEvenement, Pret pret, DecisionScoring decision, Instant horodatage) {
        StatutPret statut = statut(decision);
        return new PretDecide(idEvenement, pret.id(), pret.clientId(), statut, Notification.de(pret, decision),
                horodatage);
    }

    /** Statut du prêt après la décision. */
    public static StatutPret statut(DecisionScoring decision) {
        return decision == DecisionScoring.APPROUVE ? StatutPret.EN_COURS : StatutPret.REFUSE;
    }
}
