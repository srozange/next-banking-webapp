package fr.nextbanking.credits.application.port.out;

import fr.nextbanking.credits.domain.DemandeSimulation;
import fr.nextbanking.credits.domain.Simulation;

/**
 * Port sortant : calcule l'emprunt d'une demande valide.
 */
public interface CalculSimulation {

    Simulation calculer(DemandeSimulation demande);
}
