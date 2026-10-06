package fr.nextbanking.bourse.domain;

/**
 * Ajout d'une valeur à la watchlist refusé, avec son motif ; le symbole n'est porté que pour un doublon.
 */
public class AjoutRefuse extends RuntimeException {

    /** Motifs de refus, dans l'ordre des contrôles. */
    public enum Motif {
        SYMBOLE_REQUIS,
        NOM_REQUIS,
        REQUETE_INVALIDE,
        DOUBLON
    }

    private final Motif motif;
    private final String symbole;

    private AjoutRefuse(Motif motif, String symbole) {
        super("Ajout refusé : " + motif);
        this.motif = motif;
        this.symbole = symbole;
    }

    /** Refus sans symbole : champ requis ou requête invalide. */
    public static AjoutRefuse pour(Motif motif) {
        return new AjoutRefuse(motif, null);
    }

    /** Refus d'un symbole déjà présent, en majuscules. */
    public static AjoutRefuse doublon(String symbole) {
        return new AjoutRefuse(Motif.DOUBLON, symbole);
    }

    public Motif motif() {
        return motif;
    }

    /** Symbole en double ; {@code null} pour les autres motifs. */
    public String symbole() {
        return symbole;
    }
}
