package fr.nextbanking.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.notifications.domain.TemoinRecu;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecevoirTemoinCasUsageTest {

    private final List<TemoinRecu> enregistres = new ArrayList<>();
    private final RecevoirTemoinCasUsage casUsage = new RecevoirTemoinCasUsage(enregistres::add);

    @Test
    void enregistre_le_temoin_recu() {
        TemoinRecu temoin = casUsage.recevoir(42L);

        assertThat(temoin).isEqualTo(new TemoinRecu(42L));
        assertThat(enregistres).containsExactly(temoin);
    }

    @Test
    void n_enregistre_rien_si_l_identifiant_est_refuse() {
        assertThatThrownBy(() -> casUsage.recevoir(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(enregistres).isEmpty();
    }
}
