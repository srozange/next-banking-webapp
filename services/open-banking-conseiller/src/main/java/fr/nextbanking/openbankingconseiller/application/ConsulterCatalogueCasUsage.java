package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterCatalogue;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotProduits;
import fr.nextbanking.openbankingconseiller.domain.Catalogue;
import fr.nextbanking.openbankingconseiller.domain.ProduitCredit;
import fr.nextbanking.openbankingconseiller.domain.ProduitEpargne;
import fr.nextbanking.openbankingconseiller.domain.ProduitInvestissement;
import fr.nextbanking.openbankingconseiller.domain.TypeProduit;
import java.util.List;

/**
 * Lit le catalogue des produits actifs ; une famille non demandée n'est pas lue et reste vide.
 */
public class ConsulterCatalogueCasUsage implements ConsulterCatalogue {

    private final DepotProduits depot;

    public ConsulterCatalogueCasUsage(DepotProduits depot) {
        this.depot = depot;
    }

    @Override
    public Catalogue consulter(TypeProduit type) {
        List<ProduitEpargne> epargne = type == null || type == TypeProduit.EPARGNE
                ? depot.listerEpargneActifs() : List.of();
        List<ProduitCredit> credit = type == null || type == TypeProduit.CREDIT
                ? depot.listerCreditActifs() : List.of();
        List<ProduitInvestissement> investissement = type == null || type == TypeProduit.INVESTISSEMENT
                ? depot.listerInvestissementActifs() : List.of();
        return new Catalogue(epargne, credit, investissement);
    }
}
