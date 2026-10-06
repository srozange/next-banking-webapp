package fr.nextbanking.openbankingconseiller.application.port.in;

import fr.nextbanking.openbankingconseiller.domain.Catalogue;
import fr.nextbanking.openbankingconseiller.domain.TypeProduit;

/**
 * Port entrant : le catalogue des produits actifs, identique pour tout utilisateur authentifié (FR-010-01).
 */
public interface ConsulterCatalogue {

    /**
     * @param type famille à rendre ; {@code null} pour les trois. Les groupes non demandés sont vides.
     */
    Catalogue consulter(TypeProduit type);
}
