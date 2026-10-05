package fr.nextbanking.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TemoinRecuTest {

    @Test
    void accepte_un_identifiant_strictement_positif() {
        assertThat(new TemoinRecu(1L).temoinId()).isEqualTo(1L);
    }

    @Test
    void refuse_un_identifiant_nul() {
        assertThatThrownBy(() -> new TemoinRecu(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_identifiant_negatif() {
        assertThatThrownBy(() -> new TemoinRecu(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
