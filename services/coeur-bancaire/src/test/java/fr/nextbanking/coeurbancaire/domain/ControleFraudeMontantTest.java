package fr.nextbanking.coeurbancaire.domain;

import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.evaluerMontant;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.signal;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Signaux fondés sur le montant : LARGE_AMOUNT (un seul, 40 puis 80) et ROUND_AMOUNT. */
class ControleFraudeMontantTest {

    @Test
    @Tag("FR-003-17")
    @Tag("FR-027-05")
    void large_amount_n_est_pas_leve_a_4999_99() {
        assertThat(signal(evaluerMontant("4999.99"), TypeSignal.LARGE_AMOUNT)).isEmpty();
    }

    @Test
    @Tag("FR-003-17")
    @Tag("FR-027-05")
    void large_amount_vaut_40_a_5000_00_et_14999_99_une_seule_fois() {
        for (String montant : new String[] {"5000.00", "14999.99"}) {
            Evaluation evaluation = evaluerMontant(montant);

            assertThat(evaluation.signaux()).filteredOn(s -> s.type() == TypeSignal.LARGE_AMOUNT).singleElement()
                    .isEqualTo(new Signal(TypeSignal.LARGE_AMOUNT, 40,
                            "Transaction amount " + montant + " exceeds the large threshold of 5000"));
        }
    }

    @Test
    @Tag("FR-003-17")
    @Tag("FR-027-05")
    void large_amount_vaut_80_a_15000_00_et_au_dela_une_seule_fois() {
        for (String montant : new String[] {"15000.00", "99999.99"}) {
            Evaluation evaluation = evaluerMontant(montant);

            assertThat(evaluation.signaux()).filteredOn(s -> s.type() == TypeSignal.LARGE_AMOUNT).singleElement()
                    .isEqualTo(new Signal(TypeSignal.LARGE_AMOUNT, 80,
                            "Transaction amount " + montant + " exceeds the very-large threshold of 15000"));
        }
    }

    @Test
    @Tag("FR-003-21")
    void round_amount_vaut_20_pour_1000_et_3000_seulement() {
        assertThat(signal(evaluerMontant("999.00"), TypeSignal.ROUND_AMOUNT)).isEmpty();
        assertThat(signal(evaluerMontant("1500.00"), TypeSignal.ROUND_AMOUNT)).isEmpty();
        assertThat(signal(evaluerMontant("1000.01"), TypeSignal.ROUND_AMOUNT)).isEmpty();
        for (String montant : new String[] {"1000.00", "3000.00"}) {
            assertThat(signal(evaluerMontant(montant), TypeSignal.ROUND_AMOUNT)).contains(new Signal(
                    TypeSignal.ROUND_AMOUNT, 20,
                    "Transaction amount " + montant + " is a suspicious round figure (multiple of 1000)"));
        }
    }

    @Test
    @Tag("FR-027-01")
    void un_montant_ordinaire_ne_leve_aucun_signal() {
        Evaluation evaluation = evaluerMontant("100.00");

        assertThat(evaluation.signaux()).isEmpty();
        assertThat(evaluation.score()).isZero();
        assertThat(evaluation.alerter()).isFalse();
    }
}
