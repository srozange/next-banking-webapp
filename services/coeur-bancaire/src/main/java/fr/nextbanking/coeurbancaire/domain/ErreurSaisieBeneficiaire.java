package fr.nextbanking.coeurbancaire.domain;

/**
 * Refus de l'ajout d'un bénéficiaire : le texte est celui du legacy (MSG-013-071 à 075), le champ désigne la saisie
 * fautive pour que l'écran la marque sans analyser le texte. Un dépassement de longueur n'a pas de texte propre
 * (RG-013-029 et 031 sont suspectes) : l'adaptateur répond alors « Requête invalide ».
 */
public class ErreurSaisieBeneficiaire extends RuntimeException {

    /** Champ du formulaire d'ajout. */
    public enum Champ {
        LIBELLE("libelle"),
        TITULAIRE("titulaire"),
        IBAN("iban"),
        BIC("bic"),
        BANQUE("banque");

        private final String nom;

        Champ(String nom) {
            this.nom = nom;
        }

        /** Nom du champ dans le corps JSON. */
        public String nom() {
            return nom;
        }
    }

    /** Cause du refus. */
    public enum Cause {
        OBLIGATOIRE,
        IBAN_INVALIDE,
        BIC_INVALIDE,
        TROP_LONG
    }

    static final String LIBELLE_OBLIGATOIRE = "Le libellé est obligatoire.";
    static final String IBAN_OBLIGATOIRE = "L'IBAN est obligatoire.";
    static final String IBAN_INVALIDE = "IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97).";
    static final String TITULAIRE_OBLIGATOIRE = "Le nom du titulaire est obligatoire.";
    static final String BIC_INVALIDE = "BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques.";
    static final String TROP_LONG = "Longueur maximale dépassée.";

    private final Champ champ;
    private final Cause cause;

    ErreurSaisieBeneficiaire(Champ champ, Cause cause, String message) {
        super(message);
        this.champ = champ;
        this.cause = cause;
    }

    /** Champ fautif. */
    public Champ champ() {
        return champ;
    }

    /** Cause du refus. */
    public Cause motif() {
        return cause;
    }
}
