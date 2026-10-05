package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemoinEcritTest {

    private static final UUID ID = UUID.fromString("3f2c1a9e-8b7d-4c6e-9f10-2a3b4c5d6e7f");
    private static final Instant HORODATAGE = Instant.parse("2026-10-05T08:30:00Z");

    @Test
    void porte_l_evenement_d_un_temoin_ecrit() {
        TemoinEcrit evenement = new TemoinEcrit(ID, 42L, HORODATAGE);

        assertThat(evenement.idEvenement()).isEqualTo(ID);
        assertThat(evenement.temoinId()).isEqualTo(42L);
        assertThat(evenement.horodatage()).isEqualTo(HORODATAGE);
    }

    @Test
    void refuse_un_identifiant_d_evenement_absent() {
        assertThatThrownBy(() -> new TemoinEcrit(null, 42L, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_identifiant_de_temoin_nul_ou_negatif() {
        assertThatThrownBy(() -> new TemoinEcrit(ID, 0L, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TemoinEcrit(ID, -1L, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepte_le_plus_petit_identifiant_de_temoin() {
        assertThat(new TemoinEcrit(ID, 1L, HORODATAGE).temoinId()).isEqualTo(1L);
    }

    @Test
    void refuse_un_horodatage_absent() {
        assertThatThrownBy(() -> new TemoinEcrit(ID, 42L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
