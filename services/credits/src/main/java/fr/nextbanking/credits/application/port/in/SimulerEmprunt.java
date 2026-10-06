package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.Simulation;

/**
 * Port entrant : simuler un emprunt à partir des champs saisis.
 */
public interface SimulerEmprunt {

    /**
     * @throws fr.nextbanking.credits.domain.SimulationInvalide si un champ est refusé
     * @throws fr.nextbanking.credits.application.SimulationEnEchec si le calcul n'a pas pu être effectué
     */
    Simulation simuler(String montant, String tauxAnnuel, String dureeMois);
}
