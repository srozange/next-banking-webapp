package fr.nextbanking.coeurbancaire.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Une exécution d'un traitement nocturne, telle qu'inscrite au journal (FR-011-09).
 *
 * @param debut instant de début ; absent si l'exécution n'a pas démarré
 * @param fin instant de fin ; absent si l'exécution n'est pas terminée
 */
public record ExecutionTraitement(long id, String job, Instant debut, Instant fin, Integer enregistrements,
        StatutTraitement statut, String details) {

    /** Durée en millisecondes, 0 si le début ou la fin manque. */
    public long dureeMs() {
        if (debut == null || fin == null) {
            return 0;
        }
        return Duration.between(debut, fin).toMillis();
    }
}
