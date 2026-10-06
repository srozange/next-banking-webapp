package fr.nextbanking.bourse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.bourse.domain.AjoutRefuse;
import fr.nextbanking.bourse.domain.ValeurIntrouvable;
import fr.nextbanking.bourse.domain.ValeurWatchlist;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CasUsageWatchlistTest {

    private final DepotWatchlistEnMemoire depot = new DepotWatchlistEnMemoire()
            .avec("MSFT", "Microsoft", "NASDAQ", "USD")
            .avec("BNP.PA", "BNP Paribas", "Euronext Paris", "EUR")
            .avec("AI.PA", "Air Liquide", "Euronext Paris", "EUR");

    @Test
    @Tag("FR-009-10")
    @Tag("FR-029-11")
    void consulte_toute_la_watchlist_triee_par_marche_puis_symbole() {
        assertThat(new ConsulterWatchlistCasUsage(depot).consulter()).extracting(ValeurWatchlist::symbole)
                .containsExactly("AI.PA", "BNP.PA", "MSFT");
    }

    @Test
    @Tag("FR-009-12")
    @Tag("FR-029-10")
    void ajoute_la_valeur_normalisee() {
        ValeurWatchlist ajoutee = new AjouterValeurCasUsage(depot).ajouter("  sg.pa ", " Société Générale ",
                "Euronext Paris", "eur");

        assertThat(ajoutee).isEqualTo(new ValeurWatchlist(4, "SG.PA", "Société Générale", "Euronext Paris", "EUR"));
        assertThat(depot.valeurs).contains(ajoutee);
    }

    @Test
    @Tag("FR-009-11")
    @Tag("FR-029-09")
    void refuse_un_doublon_sans_tenir_compte_de_la_casse() {
        assertThatThrownBy(() -> new AjouterValeurCasUsage(depot).ajouter("bnp.pa", "BNP", "Euronext Paris", "EUR"))
                .isInstanceOfSatisfying(AjoutRefuse.class, refus -> {
                    assertThat(refus.motif()).isEqualTo(AjoutRefuse.Motif.DOUBLON);
                    assertThat(refus.symbole()).isEqualTo("BNP.PA");
                });
        assertThat(depot.valeurs).hasSize(3);
    }

    @Test
    @Tag("FR-029-09")
    void controle_la_saisie_avant_le_doublon() {
        assertThatThrownBy(() -> new AjouterValeurCasUsage(depot).ajouter("bnp.pa", " ", "Euronext Paris", "EUR"))
                .isInstanceOfSatisfying(AjoutRefuse.class,
                        refus -> assertThat(refus.motif()).isEqualTo(AjoutRefuse.Motif.NOM_REQUIS));
        assertThat(depot.valeurs).hasSize(3);
    }

    @Test
    @Tag("FR-009-13")
    void retire_une_valeur_sans_confirmation() {
        new RetirerValeurCasUsage(depot).retirer(3);

        assertThat(depot.valeurs).extracting(ValeurWatchlist::symbole).containsExactly("MSFT", "BNP.PA");
    }

    @Test
    @Tag("FR-009-13")
    void refuse_de_retirer_une_valeur_inconnue() {
        assertThatThrownBy(() -> new RetirerValeurCasUsage(depot).retirer(99))
                .isInstanceOfSatisfying(ValeurIntrouvable.class, erreur -> assertThat(erreur.id()).isEqualTo(99));
        assertThat(depot.valeurs).hasSize(3);
    }
}
