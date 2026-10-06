package fr.nextbanking.coeurbancaire.domain;

/**
 * Refus d'un virement : la cause fixe le texte affiché (jamais de détail technique, NFR-SEC-04) ; l'adaptateur entrant
 * en déduit le statut HTTP. {@code CLE_IDEMPOTENCE_DEJA_UTILISEE} est interne : la soumission est alors une répétition.
 */
public class ErreurVirement extends RuntimeException {

    /** Cause du refus et texte affiché. */
    public enum Cause {
        COMPTES_OBLIGATOIRES(TextesVirement.COMPTES_OBLIGATOIRES),
        COMPTES_IDENTIQUES(TextesVirement.COMPTES_IDENTIQUES),
        MONTANT_INVALIDE(TextesVirement.MONTANT_INVALIDE),
        MOTIF_TROP_LONG(TextesVirement.MOTIF_TROP_LONG),
        COMPTE_INTROUVABLE(TextesVirement.COMPTE_INTROUVABLE),
        COMPTE_ETRANGER(TextesVirement.ACCES_REFUSE),
        SOLDE_INSUFFISANT(TextesVirement.SOLDE_INSUFFISANT),
        REMISE_IMPOSSIBLE(TextesVirement.REMISE_IMPOSSIBLE),
        /** Contrôle de fraude en erreur : le message complet est rendu avec la référence de trace par l'adaptateur. */
        CONTROLE_EN_ERREUR(TextesVirement.refusControle("")),
        ROLE_INSUFFISANT(TextesVirement.ACCES_REFUSE),
        MOTIF_DECISION_OBLIGATOIRE(TextesVirement.MOTIF_DECISION_OBLIGATOIRE),
        VIREMENT_NON_SUSPENDU(TextesVirement.VIREMENT_NON_SUSPENDU),
        CLE_IDEMPOTENCE_DEJA_UTILISEE(TextesVirement.REMISE_IMPOSSIBLE);

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

    public ErreurVirement(Cause cause) {
        this(cause, null);
    }

    public ErreurVirement(Cause cause, Throwable origine) {
        super(cause.texte(), origine);
        this.cause = cause;
    }

    /** Cause du refus. */
    public Cause motif() {
        return cause;
    }
}
