package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.ExecutionTraitement;
import java.util.List;

/**
 * Port sortant : journal des traitements nocturnes.
 */
public interface DepotTraitements {

    /** Les dernières exécutions, par début décroissant (début absent en dernier), au plus {@code limite}. */
    List<ExecutionTraitement> dernieres(int limite);
}
