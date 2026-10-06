package fr.nextbanking.openbankingconseiller.domain;

import java.util.List;

/**
 * Catalogue des produits actifs, en trois groupes triés par nom (FR-011-02, FR-011-03).
 */
public record Catalogue(List<ProduitEpargne> epargne, List<ProduitCredit> credit,
        List<ProduitInvestissement> investissement) {

    public Catalogue {
        epargne = List.copyOf(epargne);
        credit = List.copyOf(credit);
        investissement = List.copyOf(investissement);
    }
}
