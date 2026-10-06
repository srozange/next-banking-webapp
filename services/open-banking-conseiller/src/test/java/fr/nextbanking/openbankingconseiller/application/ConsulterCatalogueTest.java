package fr.nextbanking.openbankingconseiller.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotProduits;
import fr.nextbanking.openbankingconseiller.domain.Catalogue;
import fr.nextbanking.openbankingconseiller.domain.ProduitCredit;
import fr.nextbanking.openbankingconseiller.domain.ProduitEpargne;
import fr.nextbanking.openbankingconseiller.domain.ProduitInvestissement;
import fr.nextbanking.openbankingconseiller.domain.TypeProduit;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterCatalogueTest {

    private static final ProduitEpargne EPARGNE = new ProduitEpargne(1, "Livret A", "d", null, null, null, null);
    private static final ProduitCredit CREDIT = new ProduitCredit(5, "Auto", "d", null, 72, null, null);
    private static final ProduitInvestissement INVESTISSEMENT =
            new ProduitInvestissement(9, "ETF", "d", "Actions", 3, null);

    private final DepotProduits depot = new DepotProduits() {
        @Override
        public List<ProduitEpargne> listerEpargneActifs() {
            return List.of(EPARGNE);
        }

        @Override
        public List<ProduitCredit> listerCreditActifs() {
            return List.of(CREDIT);
        }

        @Override
        public List<ProduitInvestissement> listerInvestissementActifs() {
            return List.of(INVESTISSEMENT);
        }
    };

    private final ConsulterCatalogueCasUsage casUsage = new ConsulterCatalogueCasUsage(depot);

    @Test
    @Tag("FR-011-02")
    void sans_type_rend_les_trois_groupes() {
        Catalogue catalogue = casUsage.consulter(null);

        assertThat(catalogue.epargne()).containsExactly(EPARGNE);
        assertThat(catalogue.credit()).containsExactly(CREDIT);
        assertThat(catalogue.investissement()).containsExactly(INVESTISSEMENT);
    }

    @Test
    @Tag("FR-011-03")
    void un_type_ne_remplit_que_son_groupe() {
        Catalogue epargne = casUsage.consulter(TypeProduit.EPARGNE);
        assertThat(epargne.epargne()).containsExactly(EPARGNE);
        assertThat(epargne.credit()).isEmpty();
        assertThat(epargne.investissement()).isEmpty();

        Catalogue credit = casUsage.consulter(TypeProduit.CREDIT);
        assertThat(credit.epargne()).isEmpty();
        assertThat(credit.credit()).containsExactly(CREDIT);
        assertThat(credit.investissement()).isEmpty();

        Catalogue investissement = casUsage.consulter(TypeProduit.INVESTISSEMENT);
        assertThat(investissement.epargne()).isEmpty();
        assertThat(investissement.credit()).isEmpty();
        assertThat(investissement.investissement()).containsExactly(INVESTISSEMENT);
    }
}
