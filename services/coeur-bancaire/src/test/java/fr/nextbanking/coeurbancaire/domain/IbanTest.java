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

    @Test
    @Tag("FR-013-03")
    void l_affichage_garde_quatre_caracteres_de_chaque_cote_derriere_trois_etoiles() {
        assertThat(new Iban("FR3530004000000123456789010").affichage()).isEqualTo("FR35***9010");
        assertThat(new Iban("123456789").affichage()).isEqualTo("1234***6789");
        assertThat(new Iban("12345678").affichage()).isEqualTo("***");
        assertThat(new Iban("FR3530004000000123456789010")).hasToString("FR35****9010");
    }

    @Test
    @Tag("FR-013-03")
    void la_zone_sepa_est_calculee_depuis_le_code_pays() {
        assertThat(new Iban("FR3530004000000123456789010").zoneSepa()).isTrue();
        assertThat(new Iban("de89370400440532013000").zoneSepa()).isTrue();
        assertThat(new Iban("GB29NWBK60161331926819").zoneSepa()).isTrue();
        assertThat(new Iban("US12345678901234").zoneSepa()).isFalse();
        assertThat(new Iban("F").zoneSepa()).isFalse();
        assertThat(new Iban("FR").zoneSepa()).isTrue();
    }

    @Test
    @Tag("FR-013-07")
    void la_normalisation_supprime_les_blancs_et_met_en_majuscules() {
        assertThat(Iban.normaliser(" fr35 3000\t4000 0001 2345 6789 010 ")).isEqualTo("FR3530004000000123456789010");
        assertThat(Iban.normaliser(null)).isEmpty();
        assertThat(Iban.normaliser("   ")).isEmpty();
    }

    @Test
    @Tag("FR-013-04")
    void le_code_pays_est_en_majuscules() {
        assertThat(new Iban("fr35").codePays()).isEqualTo("FR");
        assertThat(new Iban("F").codePays()).isEqualTo("F");
    }

    @Test
    @Tag("FR-013-04")
    void le_controle_mod_97_accepte_les_cles_justes_et_refuse_les_autres() {
        assertThat(new Iban("FR3530004000000123456789010").controleMod97()).isTrue();
        assertThat(new Iban("fr3530004000000123456789010").controleMod97()).isTrue();
        assertThat(new Iban("TR330006100519786457841326").controleMod97()).isTrue();
        assertThat(new Iban("GB29NWBK60161331926819").controleMod97()).isTrue();
        assertThat(new Iban("FR3530004000000123456789011").controleMod97()).isFalse();
        assertThat(new Iban("FR76-0001-0002").controleMod97()).isFalse();
        assertThat(new Iban("FR35300040").controleMod97()).isFalse();
    }
}
