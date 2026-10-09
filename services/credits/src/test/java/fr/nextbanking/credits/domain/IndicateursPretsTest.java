package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-016-02")
class IndicateursPretsTest {

    private static Pret pret(String montant) {
        return new Pret(1L, 5L, TypePret.AUTO, new BigDecimal(montant), new BigDecimal("3.9000"), 6,
                new BigDecimal("1517.11"), new BigDecimal("9102.66"), new BigDecimal("102.66"), "Voiture",
                StatutPret.EN_ATTENTE, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 4, 1));
    }

    @Test
    void compte_les_prets_et_cumule_leurs_montants_a_deux_decimales() {
        IndicateursPrets indicateurs = IndicateursPrets.de(List.of(pret("5000.00"), pret("10000")));

        assertThat(indicateurs.nombre()).isEqualTo(2L);
        assertThat(indicateurs.montant().toPlainString()).isEqualTo("15000.00");
    }

    @Test
    void sans_pret_le_cumul_vaut_zero_a_deux_decimales() {
        IndicateursPrets indicateurs = IndicateursPrets.de(List.of());

        assertThat(indicateurs.nombre()).isZero();
        assertThat(indicateurs.montant().toPlainString()).isEqualTo("0.00");
    }
}
