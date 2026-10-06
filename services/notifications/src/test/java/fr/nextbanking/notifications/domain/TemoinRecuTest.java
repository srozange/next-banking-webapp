package fr.nextbanking.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-002-09")
@Tag("BUG-007")
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
