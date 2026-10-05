package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.Temoin;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EcrireTemoinCasUsageTest {

    private final List<Temoin> enregistres = new ArrayList<>();
    private final EcrireTemoinCasUsage casUsage = new EcrireTemoinCasUsage(enregistres::add);

    @Test
    void enregistre_le_temoin_ecrit() {
        Temoin temoin = casUsage.ecrire("premier témoin");

        assertThat(temoin).isEqualTo(new Temoin("premier témoin"));
        assertThat(enregistres).containsExactly(temoin);
    }

    @Test
    void n_enregistre_rien_si_le_libelle_est_refuse() {
        assertThatThrownBy(() -> casUsage.ecrire(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(enregistres).isEmpty();
    }
}
