package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement de décision du contrôle : l'état du virement et la notification à en faire, rendue. Topic
 * {@code coeur-bancaire.virement-decide.v1}, clé {@code virementId}.
 */
public record VirementDecide(UUID idEvenement, long virementId, long clientId, EtatVirement etat,
        NotificationVirement notification, Instant horodatage) {

    public VirementDecide {
        if (idEvenement == null || etat == null || notification == null || horodatage == null) {
            throw new IllegalArgumentException("L'identifiant, l'état, la notification et l'horodatage sont obligatoires.");
        }
    }
}
