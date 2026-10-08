package fr.nextbanking.coeurbancaire.domain;

/**
 * Refus de suppression d'un bénéficiaire : le texte est unique (MSG-013-080), sans détail technique (NFR-SEC-04) ;
 * l'adaptateur entrant en déduit le statut HTTP (403 bénéficiaire d'un autre client, 404 absent ou déjà désactivé).
 */
public class ErreurBeneficiaire extends RuntimeException {

    /** Texte du legacy (MSG-013-080). */
    public static final String SUPPRESSION_IMPOSSIBLE = "Erreur lors de la suppression.";

    /** Cause du refus. */
    public enum Cause {
        INTROUVABLE,
        ETRANGER
    }

    private final Cause cause;

    public ErreurBeneficiaire(Cause cause) {
        super(SUPPRESSION_IMPOSSIBLE);
        this.cause = cause;
    }

    /** Cause du refus. */
    public Cause motif() {
        return cause;
    }
}
