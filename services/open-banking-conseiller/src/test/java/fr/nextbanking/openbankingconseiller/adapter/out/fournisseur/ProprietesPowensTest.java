package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-028-12")
class ProprietesPowensTest {

    @Test
    void les_valeurs_par_defaut_sont_celles_de_la_sandbox_et_les_delais_de_15_secondes() {
        ProprietesPowens proprietes = ProprietesPowens.parDefaut();

        assertThat(proprietes.reel()).isFalse();
        assertThat(proprietes.mode()).isEqualTo("simule");
        assertThat(proprietes.domaine()).isEqualTo("demi-sandbox.biapi.pro");
        assertThat(proprietes.clientId()).isEqualTo("99961298");
        assertThat(proprietes.banques()).isEqualTo("59");
        assertThat(proprietes.delaiAttente()).isEqualTo(Duration.ofSeconds(15));
        assertThat(proprietes.seuilEchecs()).isEqualTo(5);
        assertThat(proprietes.dureeOuverture()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void le_mode_reel_est_reconnu() {
        assertThat(new ProprietesPowens("reel", null, null, null, null, null, null).reel()).isTrue();
    }

    @Test
    void une_valeur_invalide_est_refusee() {
        assertThatThrownBy(() -> new ProprietesPowens("autre", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mode");
        assertThatThrownBy(() -> new ProprietesPowens(null, " ", null, null, null, null, null))
                .hasMessageContaining("domaine");
        assertThatThrownBy(() -> new ProprietesPowens(null, null, "", null, null, null, null))
                .hasMessageContaining("client-id");
        assertThatThrownBy(() -> new ProprietesPowens(null, null, null, " ", null, null, null))
                .hasMessageContaining("banques");
        assertThatThrownBy(() -> new ProprietesPowens(null, null, null, null, Duration.ZERO, null, null))
                .hasMessageContaining("delai-attente");
        assertThatThrownBy(() -> new ProprietesPowens(null, null, null, null, null, 0, null))
                .hasMessageContaining("seuil-echecs");
        assertThatThrownBy(() -> new ProprietesPowens(null, null, null, null, null, null, Duration.ofSeconds(-1)))
                .hasMessageContaining("duree-ouverture");
    }
}
