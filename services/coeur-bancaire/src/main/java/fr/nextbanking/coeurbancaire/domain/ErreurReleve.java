package fr.nextbanking.coeurbancaire.domain;

/**
 * Refus de consultation du relevé d'un compte : la cause fixe le texte affiché (jamais de détail technique,
 * NFR-SEC-04) ; l'adaptateur entrant en déduit le statut HTTP (404 compte inexistant, 403 compte d'un autre client).
 */
public class ErreurReleve extends RuntimeException {

    /** Cause du refus et texte affiché. */
    public enum Cause {
        COMPTE_INTROUVABLE(TextesVirement.RESSOURCE_INTROUVABLE),
        COMPTE_ETRANGER(TextesVirement.ACCES_REFUSE);

        private final String texte;

        Cause(String texte) {
            this.texte = texte;
        }

        /** Texte affiché à l'utilisateur. */
        public String texte() {
            return texte;
        }
    }

    private final Cause cause;

    public ErreurReleve(Cause cause) {
        super(cause.texte());
        this.cause = cause;
    }

    /** Cause du refus. */
    public Cause motif() {
        return cause;
    }
}
