package fr.nextbanking.credits.domain;

/**
 * Demande de prêt refusée ; le motif désigne le premier champ invalide, dans l'ordre type, montant, taux, durée, objet.
 */
public class DemandePretInvalide extends RuntimeException {

    /** Champ refusé. */
    public enum Motif {
        TYPE, MONTANT, TAUX, DUREE, OBJET
    }

    private final Motif motif;

    public DemandePretInvalide(Motif motif) {
        super("Demande de prêt invalide : " + motif);
        this.motif = motif;
    }

    public Motif motif() {
        return motif;
    }
}
