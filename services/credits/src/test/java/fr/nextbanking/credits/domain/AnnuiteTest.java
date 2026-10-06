package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-007-03")
class AnnuiteTest {

    private static Simulation simuler(String montant, String taux, String duree) {
        return Annuite.simuler(DemandeSimulation.lire(montant, taux, duree));
    }

    @Test
    void la_reference_10000_3_5_36_donne_293_02_10548_72_et_548_72() {
        Simulation simulation = simuler("10000", "3.5", "36");

        assertThat(simulation.mensualite()).isEqualByComparingTo("293.02");
        assertThat(simulation.coutTotal()).isEqualByComparingTo("10548.72");
        assertThat(simulation.totalInterets()).isEqualByComparingTo("548.72");
        assertThat(simulation.echeancier()).hasSize(36);
        assertThat(simulation.mensualite().scale()).isEqualTo(2);
        assertThat(simulation.coutTotal().scale()).isEqualTo(2);
        assertThat(simulation.totalInterets().scale()).isEqualTo(2);
    }

    @Test
    void un_taux_nul_divise_le_montant_par_la_duree() {
        Simulation simulation = simuler("12000", "0", "24");

        assertThat(simulation.mensualite()).isEqualByComparingTo("500.00");
        assertThat(simulation.totalInterets()).isEqualByComparingTo("0.00");
        assertThat(simulation.echeancier()).allSatisfy(ligne -> assertThat(ligne.interets()).isEqualByComparingTo("0"));
    }

    @Test
    void la_premiere_ligne_applique_le_taux_mensuel_au_montant() {
        LignePret premiere = simuler("10000", "3.5", "36").echeancier().get(0);

        // 10000 x 3,5 % / 12 = 29,1666... arrondi HALF_UP.
        assertThat(premiere.numero()).isEqualTo(1);
        assertThat(premiere.interets()).isEqualByComparingTo("29.17");
        assertThat(premiere.capital()).isEqualByComparingTo("263.85");
        assertThat(premiere.total()).isEqualByComparingTo("293.02");
        assertThat(premiere.capitalRestant()).isEqualByComparingTo("9736.15");
    }

    @Test
    void la_derniere_ligne_solde_le_capital_restant_du() {
        List<LignePret> echeancier = simuler("10000", "3.5", "36").echeancier();
        LignePret derniere = echeancier.get(35);

        assertThat(derniere.numero()).isEqualTo(36);
        assertThat(derniere.capitalRestant()).isEqualByComparingTo("0");
        BigDecimal capitalAvant = echeancier.get(34).capitalRestant();
        assertThat(derniere.capital()).isEqualByComparingTo(capitalAvant);
        assertThat(derniere.total()).isEqualByComparingTo(derniere.capital().add(derniere.interets()));
    }

    @Test
    void le_capital_rembourse_total_vaut_le_montant() {
        Simulation simulation = simuler("7777.77", "4.9", "17");

        BigDecimal capital = simulation.echeancier().stream().map(LignePret::capital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(capital).isEqualByComparingTo("7777.77");
        assertThat(simulation.echeancier()).hasSize(17);
    }

    @Test
    void les_bornes_taux_30_et_duree_360_sont_calculees() {
        Simulation simulation = simuler("10000", "30", "360");

        assertThat(simulation.echeancier()).hasSize(360);
        assertThat(simulation.mensualite()).isEqualByComparingTo("250.03");
    }

    @Test
    void une_duree_d_un_mois_rembourse_tout_en_une_ligne() {
        Simulation simulation = simuler("1000", "12", "1");

        assertThat(simulation.mensualite()).isEqualByComparingTo("1010.00");
        assertThat(simulation.echeancier()).hasSize(1);
        assertThat(simulation.echeancier().get(0).capital()).isEqualByComparingTo("1000.00");
    }
}
