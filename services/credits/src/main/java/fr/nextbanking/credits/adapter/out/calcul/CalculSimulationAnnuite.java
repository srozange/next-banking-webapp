package fr.nextbanking.credits.adapter.out.calcul;

import fr.nextbanking.credits.application.port.out.CalculSimulation;
import fr.nextbanking.credits.domain.Annuite;
import fr.nextbanking.credits.domain.DemandeSimulation;
import fr.nextbanking.credits.domain.Simulation;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : calcul d'emprunt à annuité constante du domaine.
 */
@Component
public class CalculSimulationAnnuite implements CalculSimulation {

    @Override
    public Simulation calculer(DemandeSimulation demande) {
        return Annuite.simuler(demande);
    }
}
