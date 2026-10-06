package fr.nextbanking.openbankingconseiller.application.port.out;

import fr.nextbanking.openbankingconseiller.domain.ProduitCredit;
import fr.nextbanking.openbankingconseiller.domain.ProduitEpargne;
import fr.nextbanking.openbankingconseiller.domain.ProduitInvestissement;
import java.util.List;

/**
 * Port sortant : produits actifs du catalogue, chaque famille triée par nom (ordre binaire).
 */
public interface DepotProduits {

    List<ProduitEpargne> listerEpargneActifs();

    List<ProduitCredit> listerCreditActifs();

    List<ProduitInvestissement> listerInvestissementActifs();
}
