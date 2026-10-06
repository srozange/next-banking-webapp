package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.ExecutionTraitement;
import fr.nextbanking.coeurbancaire.domain.LimiteInvalide;
import java.util.List;

/**
 * Port entrant : journal des traitements nocturnes, du plus récent au plus ancien (FR-011-09).
 */
public interface ConsulterTraitements {

    /** Nombre maximal d'exécutions rendues, et valeur par défaut. */
    int LIMITE_MAXIMALE = 50;

    /**
     * @param limite nombre d'exécutions voulu, ramené à {@link #LIMITE_MAXIMALE}
     * @throws LimiteInvalide si la limite est inférieure ou égale à 0
     */
    List<ExecutionTraitement> consulter(long limite);
}
