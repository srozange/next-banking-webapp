package fr.nextbanking.coeurbancaire.domain;

/**
 * Refus d'un plafond de dépenses (FEAT-006). Les textes sont ceux du legacy, sauf le dépassement de 13 chiffres
 * ({@code budget.erreur.plafond.max}). Une période illisible n'a pas de texte propre : l'adaptateur répond « Requête
 * invalide ».
 */
public class ErreurPlafond extends RuntimeException {

    /** Cause du refus et texte affiché ({@code null} : texte générique « Requête invalide »). */
    public enum Cause {
        CATEGORIE_INVALIDE("Veuillez selectionner une categorie"),
        PLAFOND_INVALIDE("Veuillez saisir un plafond valide"),
        PLAFOND_TROP_GRAND("Le plafond ne doit pas dépasser 13 chiffres avant la virgule"),
        MOIS_INVALIDE(null),
        /** Suppression : plafond d'autrui ou inexistant, sans indice d'existence. */
        INTROUVABLE(TextesVirement.RESSOURCE_INTROUVABLE);

        private final String texte;

        Cause(String texte) {
            this.texte = texte;
        }

        /** Texte du refus, nul pour les causes sans texte propre. */
        public String texte() {
            return texte;
        }
    }

    private final Cause cause;

    public ErreurPlafond(Cause cause) {
        super(cause.texte() == null ? cause.name() : cause.texte());
        this.cause = cause;
    }

    /** Cause du refus. */
    public Cause motif() {
        return cause;
    }
}
