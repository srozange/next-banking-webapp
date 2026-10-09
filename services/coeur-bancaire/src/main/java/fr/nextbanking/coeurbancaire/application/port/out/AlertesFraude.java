package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.AlerteAdministree;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import java.time.Instant;
import java.util.List;

/** Port sortant : alertes de fraude. */
public interface AlertesFraude {

    /** Enregistre l'alerte, au statut donné, dans la transaction courante. */
    void enregistrer(AlerteFraude alerte);

    /**
     * Passe à REVIEWED les alertes OPEN du virement, dans la transaction courante ; les autres statuts restent. Rend
     * le nombre d'alertes passées en revue.
     */
    int passerEnRevue(long virementId, String revuPar, Instant revueLe);

    /**
     * Toutes les alertes, tous statuts, la plus récente d'abord (création décroissante, puis identifiant
     * décroissant), avec le nom du client et celui du relecteur lus dans la projection locale.
     */
    List<AlerteAdministree> toutes();

    /**
     * Passe l'alerte à l'issue donnée si elle est encore OPEN ; rend vrai si l'alerte a changé. Aucune autre table
     * n'est écrite.
     */
    boolean revoir(long alerteId, StatutAlerte issue, String revuPar, Instant revueLe);
}
