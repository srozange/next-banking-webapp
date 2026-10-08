package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;

/**
 * Soumission d'un virement : son identifiant, le client qui le soumet, la demande contrôlée et l'instant de soumission.
 */
public record Soumission(long id, long clientId, DemandeVirement demande, Instant soumisLe) {
}
