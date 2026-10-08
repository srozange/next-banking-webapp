package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * L'identifiant de compte enveloppe l'identifiant numérique, jamais le numéro de compte.
 */
class CompteIdTest {

    @Test
    void l_identifiant_porte_la_valeur_numerique_et_s_ecrit_en_chiffres() {
        assertThat(new CompteId(42).valeur()).isEqualTo(42L);
        assertThat(new CompteId(42)).hasToString("42").isEqualTo(new CompteId(42)).isNotEqualTo(new CompteId(43));
    }
}
