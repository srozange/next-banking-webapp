package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Objet-valeur {@code Iban} : test de référence commun du masquage (BUG-086). */
@Tag("FR-027-15")
@Tag("BUG-086")
class IbanTest {

    @Test
    void toString_masque_tout_sauf_les_quatre_premiers_et_les_quatre_derniers_caracteres() {
        Iban iban = new Iban("FR7630006000011234567890189");

        assertThat(iban.toString()).isEqualTo("FR76****0189").doesNotContain("1234567890");
        assertThat("compte " + iban).isEqualTo("compte FR76****0189");
        assertThat(new Iban("FR76-0001-0002")).hasToString("FR76****0002");
    }

    @Test
    void un_numero_trop_court_est_masque_en_entier() {
        assertThat(new Iban("12345678")).hasToString("****");
        assertThat(new Iban("123456789")).hasToString("1234****6789");
    }

    @Test
    void le_texte_en_clair_reste_disponible_pour_la_description_d_alerte() {
        assertThat(new Iban("DE89370400440532013000").enClair()).isEqualTo("DE89370400440532013000");
    }

    @Test
    void commencePar_ignore_la_casse() {
        assertThat(new Iban("fr76abc").commencePar("FR")).isTrue();
        assertThat(new Iban("FR76abc").commencePar("FR")).isTrue();
        assertThat(new Iban("DE89abc").commencePar("FR")).isFalse();
        assertThat(new Iban("F").commencePar("FR")).isFalse();
    }

    @Test
    void un_numero_absent_ou_vide_est_refuse() {
        assertThatThrownBy(() -> new Iban(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Iban("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
