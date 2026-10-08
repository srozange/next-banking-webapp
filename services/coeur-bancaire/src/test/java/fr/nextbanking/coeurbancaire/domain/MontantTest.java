package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Règles du montant : échelle 2, jamais négatif ni absent, lecture de la saisie, opérations et texte.
 */
@Tag("FR-003-04")
class MontantTest {

    @Test
    void la_construction_refuse_une_echelle_differente_de_deux() {
        for (String valeur : new String[] {"1", "1.0", "1.000"}) {
            assertThatThrownBy(() -> new Montant(new BigDecimal(valeur))).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void la_construction_refuse_une_valeur_negative_ou_absente() {
        assertThatThrownBy(() -> new Montant(new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Montant(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zero_a_deux_decimales_est_un_montant_valide() {
        assertThat(new Montant(new BigDecimal("0.00"))).isEqualTo(Montant.ZERO);
        assertThat(Montant.ZERO.texte()).isEqualTo("0.00");
    }

    @Test
    void la_lecture_rend_le_montant_a_deux_decimales() {
        assertThat(Montant.lire("100").texte()).isEqualTo("100.00");
        assertThat(Montant.lire(" 12.5 ").texte()).isEqualTo("12.50");
        assertThat(Montant.lire("1E+3").texte()).isEqualTo("1000.00");
        assertThat(Montant.lire("99999999999999999.99").valeur().scale()).isEqualTo(2);
    }

    @Test
    void la_lecture_refuse_un_montant_illisible_nul_negatif_ou_trop_precis() {
        for (String texte : new String[] {null, "", "abc", "0", "-1", "1.234", "999999999999999999.99"}) {
            assertThatThrownBy(() -> Montant.lire(texte)).isInstanceOfSatisfying(ErreurVirement.class,
                    e -> assertThat(e.motif()).isEqualTo(Cause.MONTANT_INVALIDE));
        }
    }

    @Test
    void les_operations_ajoutent_soustraient_et_comparent() {
        Montant dix = Montant.lire("10");
        Montant trois = Montant.lire("3.50");

        assertThat(dix.ajouter(trois).texte()).isEqualTo("13.50");
        assertThat(dix.soustraire(trois).texte()).isEqualTo("6.50");
        assertThat(dix.soustraire(dix)).isEqualTo(Montant.ZERO);
        assertThatThrownBy(() -> trois.soustraire(dix)).isInstanceOf(IllegalArgumentException.class);
        assertThat(dix.compareTo(trois)).isPositive();
        assertThat(trois.compareTo(dix)).isNegative();
        assertThat(dix.compareTo(Montant.lire("10.00"))).isZero();
    }

    @Test
    void le_seuil_est_atteint_a_egalite_et_au_dessus() {
        Montant dix = Montant.lire("10");

        assertThat(dix.atteint(new BigDecimal("10"))).isTrue();
        assertThat(dix.atteint(new BigDecimal("9.99"))).isTrue();
        assertThat(dix.atteint(new BigDecimal("10.01"))).isFalse();
    }

    @Test
    void le_texte_est_decimal_a_point_et_sert_de_representation() {
        assertThat(Montant.lire("100.5").texte()).isEqualTo("100.50");
        assertThat(Montant.lire("100.5")).hasToString("100.50");
    }
}
