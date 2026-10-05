package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.EcrireTemoin;
import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import fr.nextbanking.coeurbancaire.domain.Temoin;

/**
 * Cas d'usage : valide le témoin par le domaine, puis le confie au dépôt.
 */
public class EcrireTemoinCasUsage implements EcrireTemoin {

    private final DepotTemoins depot;

    public EcrireTemoinCasUsage(DepotTemoins depot) {
        this.depot = depot;
    }

    @Override
    public Temoin ecrire(String libelle) {
        Temoin temoin = new Temoin(libelle);
        depot.enregistrer(temoin);
        return temoin;
    }
}
