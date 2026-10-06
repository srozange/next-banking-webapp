package fr.nextbanking.credits.adapter.out.calcul;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.domain.DemandeSimulation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CalculSimulationAnnuiteTest {

    @Test
    @Tag("FR-007-03")
    void calcule_l_emprunt_de_reference() {
        var simulation = new CalculSimulationAnnuite().calculer(DemandeSimulation.lire("10000", "3.5", "36"));

        assertThat(simulation.mensualite()).isEqualByComparingTo("293.02");
    }
}
