package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ExecutionTraitement;
import fr.nextbanking.coeurbancaire.domain.LimiteInvalide;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-011-09")
class ConsulterTraitementsCasUsageTest {

    private final List<Integer> limitesDemandees = new ArrayList<>();
    private final ExecutionTraitement execution = new ExecutionTraitement(1, "job", null, null, null, null, null);
    private final ConsulterTraitementsCasUsage casUsage = new ConsulterTraitementsCasUsage(limite -> {
        limitesDemandees.add(limite);
        return List.of(execution);
    });

    @Test
    void rend_les_executions_du_depot() {
        assertThat(casUsage.consulter(10)).containsExactly(execution);
        assertThat(limitesDemandees).containsExactly(10);
    }

    @Test
    void plafonne_la_limite_a_50() {
        casUsage.consulter(50);
        casUsage.consulter(51);
        casUsage.consulter(Long.MAX_VALUE);

        assertThat(limitesDemandees).containsExactly(50, 50, 50);
    }

    @Test
    void refuse_une_limite_nulle_ou_negative() {
        assertThatThrownBy(() -> casUsage.consulter(0)).isInstanceOf(LimiteInvalide.class);
        assertThatThrownBy(() -> casUsage.consulter(-3)).isInstanceOf(LimiteInvalide.class);
        assertThat(limitesDemandees).isEmpty();
    }
}
