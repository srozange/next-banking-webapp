package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.function.Function;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Seuils du contrôle de fraude : strictement positifs, sinon refus avec le nom de la propriété (NFR-SEC-08). */
@Tag("FR-027-01")
@Tag("NFR-SEC-08")
class ParametresControleFraudeTest {

    private static final BigDecimal DIX = BigDecimal.TEN;

    @Test
    void des_seuils_strictement_positifs_sont_valides() {
        assertThat(new ParametresControleFraude(1, new BigDecimal("0.01"), DIX, 1, 1, 1, 1, DIX).plafondQuotidien())
                .isEqualTo(1);
    }

    @Test
    void un_seuil_entier_nul_ou_negatif_est_refuse_avec_le_nom_de_la_propriete() {
        verifier("score-cumule-max", v -> new ParametresControleFraude(v, DIX, DIX, 8, 30, 60, 5, DIX));
        verifier("plafond-quotidien", v -> new ParametresControleFraude(75, DIX, DIX, v, 30, 60, 5, DIX));
        verifier("succession-rapide-secondes", v -> new ParametresControleFraude(75, DIX, DIX, 8, v, 60, 5, DIX));
        verifier("velocite-fenetre-minutes", v -> new ParametresControleFraude(75, DIX, DIX, 8, 30, v, 5, DIX));
        verifier("velocite-nombre-max", v -> new ParametresControleFraude(75, DIX, DIX, 8, 30, 60, v, DIX));
    }

    @Test
    void un_seuil_decimal_absent_nul_ou_negatif_est_refuse_avec_le_nom_de_la_propriete() {
        for (BigDecimal invalide : new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("-0.01")}) {
            assertThatThrownBy(() -> new ParametresControleFraude(75, invalide, DIX, 8, 30, 60, 5, DIX))
                    .hasMessageContaining("nb.fraude.montant-eleve");
            assertThatThrownBy(() -> new ParametresControleFraude(75, DIX, invalide, 8, 30, 60, 5, DIX))
                    .hasMessageContaining("nb.fraude.montant-tres-eleve");
            assertThatThrownBy(() -> new ParametresControleFraude(75, DIX, DIX, 8, 30, 60, 5, invalide))
                    .hasMessageContaining("nb.fraude.velocite-montant");
        }
    }

    private static void verifier(String propriete, Function<Integer, ParametresControleFraude> fabrique) {
        for (int invalide : new int[] {0, -1}) {
            assertThatThrownBy(() -> fabrique.apply(invalide)).hasMessageContaining("nb.fraude." + propriete);
        }
    }
}
