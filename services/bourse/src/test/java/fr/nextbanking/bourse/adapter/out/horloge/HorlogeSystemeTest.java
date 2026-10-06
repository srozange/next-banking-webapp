package fr.nextbanking.bourse.adapter.out.horloge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class HorlogeSystemeTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Test
    void rend_l_instant_de_l_horloge() {
        Instant instant = Instant.parse("2026-10-05T08:30:00Z");
        HorlogeSysteme horloge = new HorlogeSysteme(Clock.fixed(instant, PARIS));

        assertThat(horloge.instant()).isEqualTo(instant);
    }

    @Test
    void rend_la_date_du_jour_dans_le_fuseau_metier() {
        // 22h30 UTC le 5 octobre : déjà le 6 octobre à Paris (UTC+2).
        HorlogeSysteme horloge = new HorlogeSysteme(Clock.fixed(Instant.parse("2026-10-05T22:30:00Z"), PARIS));

        assertThat(horloge.aujourdhui()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(horloge.fuseau()).isEqualTo(PARIS);
    }

    @Test
    void lit_le_fuseau_de_la_propriete() {
        HorlogeSysteme horloge = new HorlogeSysteme("Europe/Paris");

        assertThat(horloge.fuseau()).isEqualTo(PARIS);
        assertThat(horloge.instant()).isNotNull();
    }

    @Test
    void refuse_un_fuseau_invalide() {
        assertThatThrownBy(() -> new HorlogeSysteme("Europe/Atlantide"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nb.fuseau");
    }

    @Test
    void refuse_un_fuseau_mal_forme() {
        assertThatThrownBy(() -> new HorlogeSysteme("+99:99"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuse_un_fuseau_absent() {
        assertThatThrownBy(() -> new HorlogeSysteme(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("obligatoire");
        assertThatThrownBy(() -> new HorlogeSysteme((String) null))
                .isInstanceOf(IllegalStateException.class);
    }
}
