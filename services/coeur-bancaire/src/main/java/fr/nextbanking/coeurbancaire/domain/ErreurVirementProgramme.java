package fr.nextbanking.coeurbancaire.domain;

/**
 * Refus de la création d'un virement programmé. Le texte est celui du legacy (FEAT-004) ou « Compte destinataire
 * introuvable » (A-004-4) ; {@code champ} désigne la saisie fautive quand il y en a une. Un dépassement de longueur ou
 * une fréquence inconnue n'ont pas de texte propre : l'adaptateur répond « Requête invalide ».
 */
public class ErreurVirementProgramme extends RuntimeException {

    /** Champ du formulaire de création, tel que nommé dans le corps JSON. */
    public enum Champ {
        COMPTE_SOURCE("compteSourceId"),
        NUMERO_BENEFICIAIRE("numeroBeneficiaire"),
        LIBELLE_BENEFICIAIRE("libelleBeneficiaire"),
        MONTANT("montant"),
        FREQUENCE("frequence"),
        DATE_PREMIERE_EXECUTION("datePremiereExecution"),
        MOTIF("motif");

        private final String nom;

        Champ(String nom) {
            this.nom = nom;
        }

        /** Nom du champ dans le corps JSON. */
        public String nom() {
            return nom;
        }
    }

    /** Cause du refus et texte affiché ({@code null} : texte générique « Requête invalide »). */
    public enum Cause {
        COMPTE_SOURCE_OBLIGATOIRE("Veuillez selectionner un compte source", Champ.COMPTE_SOURCE),
        NUMERO_OBLIGATOIRE("Veuillez saisir le numero de compte destinataire", Champ.NUMERO_BENEFICIAIRE),
        LIBELLE_OBLIGATOIRE("Veuillez saisir le libelle du beneficiaire", Champ.LIBELLE_BENEFICIAIRE),
        MONTANT_INVALIDE("Montant invalide", Champ.MONTANT),
        DATE_OBLIGATOIRE("Veuillez saisir la date de premiere execution", Champ.DATE_PREMIERE_EXECUTION),
        DATE_INVALIDE("Format de date invalide (attendu : yyyy-MM-dd)", Champ.DATE_PREMIERE_EXECUTION),
        FREQUENCE_INVALIDE(null, Champ.FREQUENCE),
        TROP_LONG(null, null),
        COMPTE_SOURCE_INTROUVABLE("Erreur : Client ou compte introuvable", Champ.COMPTE_SOURCE),
        COMPTE_SOURCE_ETRANGER("Erreur : Compte non autorise", Champ.COMPTE_SOURCE),
        DESTINATAIRE_INTROUVABLE("Compte destinataire introuvable", Champ.NUMERO_BENEFICIAIRE),
        /** Suspension, réactivation ou suppression : virement d'autrui, inexistant ou déjà dans l'état demandé. */
        INTROUVABLE(TextesVirement.RESSOURCE_INTROUVABLE, null);

        private final String texte;
        private final Champ champ;

        Cause(String texte, Champ champ) {
            this.texte = texte;
            this.champ = champ;
        }

        /** Texte du refus, nul pour les causes sans texte propre. */
        public String texte() {
            return texte;
        }

        /** Champ en cause, nul si le refus ne vise aucun champ en particulier. */
        public Champ champ() {
            return champ;
        }
    }

    private final Cause cause;

    public ErreurVirementProgramme(Cause cause) {
        super(cause.texte() == null ? cause.name() : cause.texte());
        this.cause = cause;
    }

    /** Cause du refus. */
    public Cause motif() {
        return cause;
    }
}
