package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import java.time.Instant;

/** Port sortant : alertes de fraude. */
public interface AlertesFraude {

    /** Enregistre l'alerte, au statut donné, dans la transaction courante. */
    void enregistrer(AlerteFraude alerte);

    /**
     * Passe à REVIEWED les alertes OPEN du virement, dans la transaction courante ; les autres statuts restent. Rend
     * le nombre d'alertes passées en revue.
     */
    int passerEnRevue(long virementId, String revuPar, Instant revueLe);
}
