package fr.nextbanking.openbankingconseiller.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-012-10")
@Tag("BUG-042")
class SituationPowensTest {

    private static SituationPowens avec(CompteExterne... comptes) {
        return new SituationPowens(new EtatPowens("j", true, null), List.of(comptes), null, List.of());
    }

    @Test
    void un_solde_absent_n_est_pas_compte_et_zero_l_est_sans_changer_le_total() {
        SituationPowens situation = avec(new CompteExterne(1, "A", "checking", null, null),
                new CompteExterne(2, "B", "checking", null, ""), new CompteExterne(3, "C", "checking", new BigDecimal("0"), null),
                new CompteExterne(4, "D", "checking", new BigDecimal("120.5"), null));

        assertThat(situation.patrimoineTotal()).isEqualByComparingTo("120.50");
        assertThat(situation.patrimoineTotal().scale()).isEqualTo(2);
    }

    @Test
    void sans_compte_le_patrimoine_est_zero_a_deux_decimales() {
        assertThat(SituationPowens.vide().patrimoineTotal()).hasToString("0.00");
        assertThat(SituationPowens.vide().etat().connecte()).isFalse();
    }

    @Test
    void les_listes_sont_copiees() {
        List<CompteExterne> source = new java.util.ArrayList<>();
        SituationPowens situation = new SituationPowens(EtatPowens.nonConnecte(), source, null, List.of());
        source.add(new CompteExterne(1, "A", "x", null, null));

        assertThat(situation.comptes()).isEmpty();
    }
}
