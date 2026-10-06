package fr.nextbanking.bourse.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProprietesAlphaVantageTest {

    @Test
    void les_valeurs_par_defaut_sont_dans_le_code_et_le_mode_simule() {
        ProprietesAlphaVantage defaut = ProprietesAlphaVantage.parDefaut();

        assertThat(defaut.mode()).isEqualTo("simule");
        assertThat(defaut.reel()).isFalse();
        assertThat(defaut.delaiAttente()).isEqualTo(Duration.ofSeconds(5));
        assertThat(defaut.seuilEchecs()).isEqualTo(5);
        assertThat(defaut.dureeOuverture()).isEqualTo(Duration.ofSeconds(60));
        assertThat(defaut.dureeCache()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void accepte_le_mode_reel_et_des_reglages_positifs() {
        ProprietesAlphaVantage reel = new ProprietesAlphaVantage("reel", Duration.ofMillis(300), 1,
                Duration.ofSeconds(1), Duration.ofMinutes(1));

        assertThat(reel.reel()).isTrue();
        assertThat(reel.seuilEchecs()).isEqualTo(1);
    }

    @Test
    void refuse_un_mode_inconnu_une_duree_nulle_ou_negative_et_un_seuil_inferieur_a_un() {
        assertThatThrownBy(() -> new ProprietesAlphaVantage("REEL", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mode");
        assertThatThrownBy(() -> new ProprietesAlphaVantage(null, Duration.ZERO, null, null, null))
                .hasMessageContaining("delai-attente");
        assertThatThrownBy(() -> new ProprietesAlphaVantage(null, null, null, Duration.ofSeconds(-1), null))
                .hasMessageContaining("duree-ouverture");
        assertThatThrownBy(() -> new ProprietesAlphaVantage(null, null, null, null, Duration.ZERO))
                .hasMessageContaining("duree-cache");
        assertThatThrownBy(() -> new ProprietesAlphaVantage(null, null, 0, null, null))
                .hasMessageContaining("seuil-echecs");
    }
}
