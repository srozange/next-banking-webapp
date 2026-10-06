package fr.nextbanking.credits.domain;

/**
 * Données de simulation refusées ; le motif désigne le premier champ invalide, dans l'ordre montant, taux, durée.
 */
public class SimulationInvalide extends RuntimeException {

    /** Champ refusé. */
    public enum Motif {
        MONTANT, TAUX, DUREE
    }

    private final Motif motif;

    public SimulationInvalide(Motif motif) {
        super("Simulation invalide : " + motif);
        this.motif = motif;
    }

    public Motif motif() {
        return motif;
    }
}
