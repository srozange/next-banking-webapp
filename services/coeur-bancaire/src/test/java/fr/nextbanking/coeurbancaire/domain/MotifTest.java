package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Règles du motif : absent ou blanc, aucun motif retenu ; 100 caractères au plus.
 */
@Tag("FR-003-01")
class MotifTest {

    @Test
    void un_motif_absent_ou_blanc_n_est_pas_retenu() {
        for (String saisi : new String[] {null, "", "  "}) {
            assertThat(Motif.lire(saisi)).isEqualTo(Motif.AUCUN);
            assertThat(Motif.depuis(saisi)).isEqualTo(Motif.AUCUN);
        }
        assertThat(Motif.AUCUN.present()).isFalse();
        assertThat(Motif.AUCUN.texte()).isNull();
    }

    @Test
    void un_motif_saisi_est_conserve_tel_quel() {
        Motif motif = Motif.lire(" loyer ");

        assertThat(motif.texte()).isEqualTo(" loyer ");
        assertThat(motif.present()).isTrue();
        assertThat(Motif.lire("x".repeat(Motif.LONGUEUR_MAX)).texte()).hasSize(100);
    }

    @Test
    void la_lecture_refuse_plus_de_100_caracteres() {
        assertThatThrownBy(() -> Motif.lire("x".repeat(101))).isInstanceOfSatisfying(ErreurVirement.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.MOTIF_TROP_LONG));
    }

    @Test
    void la_construction_refuse_un_motif_blanc_ou_trop_long() {
        assertThatThrownBy(() -> new Motif(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Motif("x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
    }
}
