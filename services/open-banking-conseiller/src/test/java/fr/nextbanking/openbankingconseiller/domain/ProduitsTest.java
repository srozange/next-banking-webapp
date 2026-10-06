package fr.nextbanking.openbankingconseiller.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProduitsTest {

    @Test
    @Tag("FR-011-07")
    void un_produit_d_epargne_sans_precision_de_garantie_garantit_le_capital() {
        ProduitEpargne produit = new ProduitEpargne(1, "Livret", "d", new BigDecimal("0.0300"), null, null, null);

        assertThat(produit.capitalGaranti()).isTrue();
    }

    @Test
    @Tag("FR-011-07")
    void une_garantie_explicite_est_conservee() {
        assertThat(new ProduitEpargne(1, "Livret", "d", null, null, false, 12).capitalGaranti()).isFalse();
        assertThat(new ProduitEpargne(1, "Livret", "d", null, null, true, 12).capitalGaranti()).isTrue();
    }

    @Test
    @Tag("FR-011-07")
    void un_credit_sans_precision_d_assurance_ne_la_requiert_pas() {
        assertThat(new ProduitCredit(5, "Auto", "d", null, 72, null, null).assuranceRequise()).isFalse();
    }

    @Test
    @Tag("FR-011-07")
    void une_assurance_explicite_est_conservee() {
        assertThat(new ProduitCredit(5, "Immo", "d", null, 300, null, true).assuranceRequise()).isTrue();
        assertThat(new ProduitCredit(5, "Immo", "d", null, 300, null, false).assuranceRequise()).isFalse();
    }

    @Test
    @Tag("FR-011-02")
    void le_catalogue_est_une_copie_immuable() {
        List<ProduitEpargne> epargne = new ArrayList<>();
        Catalogue catalogue = new Catalogue(epargne, List.of(), List.of());
        epargne.add(new ProduitEpargne(1, "Livret", "d", null, null, null, null));

        assertThat(catalogue.epargne()).isEmpty();
        assertThat(catalogue.credit()).isEmpty();
        assertThat(catalogue.investissement()).isEmpty();
    }

    @Test
    void le_produit_d_investissement_porte_ses_champs() {
        ProduitInvestissement produit = new ProduitInvestissement(9, "ETF", "d", "Actions", 3,
                new BigDecimal("0.0720"));

        assertThat(produit.niveauRisque()).isEqualTo(3);
        assertThat(TypeProduit.valueOf("INVESTISSEMENT")).isEqualTo(TypeProduit.INVESTISSEMENT);
    }
}
