package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TemoinTest {

    @Test
    void accepte_un_libelle_renseigne() {
        assertThat(new Temoin("premier témoin").libelle()).isEqualTo("premier témoin");
    }

    @Test
    void refuse_un_libelle_vide() {
        assertThatThrownBy(() -> new Temoin("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_libelle_absent() {
        assertThatThrownBy(() -> new Temoin(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
