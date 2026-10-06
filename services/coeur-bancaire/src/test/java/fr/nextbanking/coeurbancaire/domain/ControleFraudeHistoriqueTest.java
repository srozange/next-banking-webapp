package fr.nextbanking.coeurbancaire.domain;

import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.DIX_HEURES;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.evaluerHistorique;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.signal;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Signaux d'historique : HIGH_FREQUENCY, RAPID_SUCCESSION et VELOCITY_CHECK. */
class ControleFraudeHistoriqueTest {

    private static Instant avant(long secondes) {
        return DIX_HEURES.minusSeconds(secondes);
    }

    @Test
    @Tag("FR-003-18")
    @Tag("FR-027-06")
    @Tag("BUG-010")
    @Tag("BUG-085")
    void high_frequency_n_est_pas_leve_avec_8_operations_du_jour() {
        assertThat(signal(evaluerHistorique(new HistoriqueClient(8, null, 0)), TypeSignal.HIGH_FREQUENCY)).isEmpty();
    }

    @Test
    @Tag("FR-003-18")
    @Tag("FR-027-06")
    @Tag("BUG-010")
    @Tag("BUG-085")
    void high_frequency_vaut_50_avec_9_operations_du_jour() {
        assertThat(signal(evaluerHistorique(new HistoriqueClient(9, null, 0)), TypeSignal.HIGH_FREQUENCY))
                .contains(new Signal(TypeSignal.HIGH_FREQUENCY, 50,
                        "Customer has 9 transactions today, exceeding the daily limit of 8"));
    }

    @Test
    @Tag("FR-003-23")
    void rapid_succession_vaut_55_a_29_secondes_et_pas_a_30_ni_31() {
        assertThat(signal(evaluerHistorique(new HistoriqueClient(0, avant(29), 0)), TypeSignal.RAPID_SUCCESSION))
                .contains(new Signal(TypeSignal.RAPID_SUCCESSION, 55,
                        "Previous transaction was only 29 seconds ago (threshold: 30s)"));
        assertThat(signal(evaluerHistorique(new HistoriqueClient(0, avant(30), 0)), TypeSignal.RAPID_SUCCESSION))
                .isEmpty();
        assertThat(signal(evaluerHistorique(new HistoriqueClient(0, avant(31), 0)), TypeSignal.RAPID_SUCCESSION))
                .isEmpty();
    }

    @Test
    @Tag("FR-003-23")
    void rapid_succession_n_est_pas_evalue_sans_virement_precedent() {
        assertThat(signal(evaluerHistorique(new HistoriqueClient(0, null, 0)), TypeSignal.RAPID_SUCCESSION)).isEmpty();
    }

    @Test
    @Tag("FR-003-24")
    @Tag("FR-027-10")
    void velocity_check_vaut_65_avec_5_virements_importants_et_pas_avec_4() {
        assertThat(signal(evaluerHistorique(new HistoriqueClient(0, null, 4)), TypeSignal.VELOCITY_CHECK)).isEmpty();
        assertThat(signal(evaluerHistorique(new HistoriqueClient(0, null, 5)), TypeSignal.VELOCITY_CHECK))
                .contains(new Signal(TypeSignal.VELOCITY_CHECK, 65,
                        "Customer executed 5 transactions above 500 EUR in the last 60 minutes (limit: 5)"));
    }
}
