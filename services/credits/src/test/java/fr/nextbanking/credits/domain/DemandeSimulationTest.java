package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.credits.domain.SimulationInvalide.Motif;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DemandeSimulationTest {

    private static Motif motif(String montant, String taux, String duree) {
        try {
            DemandeSimulation.lire(montant, taux, duree);
            return null;
        } catch (SimulationInvalide refus) {
            return refus.motif();
        }
    }

    @Test
    @Tag("FR-007-02")
    void lit_les_valeurs_par_defaut() {
        DemandeSimulation demande = DemandeSimulation.lire("10000", "3.50", " 36 ");

        assertThat(demande.montant()).isEqualByComparingTo("10000.00");
        assertThat(demande.montant().scale()).isEqualTo(2);
        assertThat(demande.tauxAnnuel()).isEqualByComparingTo("3.5");
        assertThat(demande.dureeMois()).isEqualTo(36);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "abc", "0", "0.00", "0.004", "-5", "1e30", "NaN"})
    @Tag("FR-004-05")
    @Tag("FR-007-02")
    void un_montant_absent_non_numerique_ou_non_positif_est_refuse(String montant) {
        assertThat(motif(montant, "3.5", "36")).isEqualTo(Motif.MONTANT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "x", "-1", "-0.01", "30.01", "31"})
    @Tag("FR-007-02")
    void un_taux_non_numerique_ou_hors_0_30_est_refuse(String taux) {
        assertThat(motif("10000", taux, "36")).isEqualTo(Motif.TAUX);
    }

    @Test
    @Tag("FR-007-02")
    void un_taux_absent_est_refuse() {
        assertThat(motif("10000", null, "36")).isEqualTo(Motif.TAUX);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"x", "36.5", "0", "-1", "361", "99999999999"})
    @Tag("FR-007-02")
    void une_duree_non_entiere_ou_hors_1_360_est_refusee(String duree) {
        assertThat(motif("10000", "3.5", duree)).isEqualTo(Motif.DUREE);
    }

    @Test
    @Tag("FR-007-02")
    void les_bornes_sont_acceptees() {
        assertThatCode(() -> DemandeSimulation.lire("0.01", "0", "1")).doesNotThrowAnyException();
        assertThatCode(() -> DemandeSimulation.lire("10000", "30", "360")).doesNotThrowAnyException();
    }

    @Test
    @Tag("FR-007-02")
    void les_champs_sont_controles_dans_l_ordre_montant_taux_duree() {
        assertThat(motif("", "-1", "0")).isEqualTo(Motif.MONTANT);
        assertThat(motif("100", "-1", "0")).isEqualTo(Motif.TAUX);
        assertThat(motif("100", "5", "0")).isEqualTo(Motif.DUREE);
    }

    @Test
    void le_message_de_l_exception_nomme_le_motif() {
        assertThatThrownBy(() -> DemandeSimulation.lire("", "3", "3"))
                .isInstanceOf(SimulationInvalide.class).hasMessageContaining("MONTANT");
    }
}
