package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.SimulerEmprunt;
import fr.nextbanking.credits.application.port.out.CalculSimulation;
import fr.nextbanking.credits.domain.DemandeSimulation;
import fr.nextbanking.credits.domain.Simulation;

/**
 * Cas d'usage : valide les champs saisis (FR-007-02, FR-004-05) puis fait calculer l'emprunt par le port sortant ;
 * toute panne du calcul devient {@link SimulationEnEchec}.
 */
public class SimulerEmpruntCasUsage implements SimulerEmprunt {

    private final CalculSimulation calcul;

    public SimulerEmpruntCasUsage(CalculSimulation calcul) {
        this.calcul = calcul;
    }

    @Override
    public Simulation simuler(String montant, String tauxAnnuel, String dureeMois) {
        DemandeSimulation demande = DemandeSimulation.lire(montant, tauxAnnuel, dureeMois);
        try {
            return calcul.calculer(demande);
        } catch (RuntimeException panne) {
            throw new SimulationEnEchec(panne);
        }
    }
}
