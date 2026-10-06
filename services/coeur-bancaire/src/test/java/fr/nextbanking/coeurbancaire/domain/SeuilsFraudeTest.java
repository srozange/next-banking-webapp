package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-003-10")
class SeuilsFraudeTest {

    @Test
    void des_seuils_strictement_positifs_sont_valides() {
        SeuilsFraude seuils = new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("0.01"), new BigDecimal("10000"));

        assertThat(seuils.seuilAttente()).isEqualByComparingTo("5000");
        assertThat(seuils.seuilAlerte()).isEqualByComparingTo("0.01");
        assertThat(seuils.seuilFraude()).isEqualByComparingTo("10000");
    }

    @Test
    void un_seuil_absent_nul_ou_negatif_est_refuse_avec_le_nom_de_la_propriete() {
        BigDecimal ok = new BigDecimal("5000");
        for (BigDecimal invalide : new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("-1")}) {
            assertThatThrownBy(() -> new SeuilsFraude(invalide, ok, ok))
                    .hasMessageContaining("nb.fraude.seuil-attente");
            assertThatThrownBy(() -> new SeuilsFraude(ok, invalide, ok))
                    .hasMessageContaining("nb.fraude.seuil-alerte");
            assertThatThrownBy(() -> new SeuilsFraude(ok, ok, invalide))
                    .hasMessageContaining("nb.fraude.seuil-fraude");
        }
    }
}
