package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterTraitements;
import fr.nextbanking.coeurbancaire.application.port.out.DepotTraitements;
import fr.nextbanking.coeurbancaire.domain.ExecutionTraitement;
import fr.nextbanking.coeurbancaire.domain.LimiteInvalide;
import java.util.List;

/**
 * Lit le journal des traitements, le même pour tout utilisateur authentifié, plafonné à 50 exécutions.
 */
public class ConsulterTraitementsCasUsage implements ConsulterTraitements {

    private final DepotTraitements depot;

    public ConsulterTraitementsCasUsage(DepotTraitements depot) {
        this.depot = depot;
    }

    @Override
    public List<ExecutionTraitement> consulter(long limite) {
        if (limite <= 0) {
            throw new LimiteInvalide(limite);
        }
        return depot.dernieres((int) Math.min(limite, LIMITE_MAXIMALE));
    }
}
