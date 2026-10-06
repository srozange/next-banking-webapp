package fr.nextbanking.bourse.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Graphique SVG d'évolution du cours (FR-009-05, FR-009-06, FR-029-08), 800 x 300 : 5 lignes de grille, 5 dates
 * {@code MM-JJ}, badge de période en bas à droite, courbe verte (dernière clôture au moins égale à la première) ou
 * rouge. Coordonnées calculées en {@link BigDecimal} ; aucun texte saisi par l'utilisateur n'entre dans le SVG.
 */
public final class GraphiqueCours {

    static final int LARGEUR = 800;
    static final int HAUTEUR = 300;
    static final String VERT = "#2e7d32";
    static final String ROUGE = "#E3000F";

    private static final int GAUCHE = 40;
    private static final int DROITE = 760;
    private static final int HAUT = 30;
    private static final int BAS = 240;
    private static final int LIGNES = 5;
    private static final int DATES = 5;
    private static final BigDecimal LARGEUR_TRACE = BigDecimal.valueOf(DROITE - GAUCHE);
    private static final BigDecimal HAUTEUR_TRACE = BigDecimal.valueOf(BAS - HAUT);

    private GraphiqueCours() {
    }

    /** SVG de la courbe des clôtures ; {@code points} doit être non vide, trié par date croissante. */
    public static String generer(Periode periode, List<PointCotation> points) {
        int dernier = points.size() - 1;
        BigDecimal premiere = points.getFirst().cloture();
        BigDecimal derniere = points.getLast().cloture();
        BigDecimal min = points.stream().map(PointCotation::cloture).min(BigDecimal::compareTo).orElseThrow();
        BigDecimal max = points.stream().map(PointCotation::cloture).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal ecart = max.subtract(min);
        String couleur = derniere.compareTo(premiere) >= 0 ? VERT : ROUGE;

        String trace = IntStream.rangeClosed(0, dernier)
                .mapToObj(i -> coordonnee(abscisse(i, dernier), ordonnee(points.get(i).cloture(), min, max, ecart)))
                .collect(Collectors.joining(" "));
        String grille = IntStream.range(0, LIGNES).mapToObj(GraphiqueCours::ligne).collect(Collectors.joining());
        String dates = IntStream.range(0, DATES).mapToObj(k -> date(k, points, dernier))
                .collect(Collectors.joining());

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + LARGEUR + "\" height=\"" + HAUTEUR
                + "\" viewBox=\"0 0 " + LARGEUR + " " + HAUTEUR + "\">"
                + "<rect width=\"" + LARGEUR + "\" height=\"" + HAUTEUR + "\" fill=\"#ffffff\"/>"
                + grille
                + "<polyline fill=\"none\" stroke=\"" + couleur + "\" stroke-width=\"2\" points=\"" + trace + "\"/>"
                + dates
                + "<text x=\"" + (LARGEUR - 10) + "\" y=\"" + (HAUTEUR - 8)
                + "\" text-anchor=\"end\" font-size=\"12\" fill=\"#666666\">" + periode.libelle() + "</text>"
                + "</svg>";
    }

    private static String ligne(int indice) {
        BigDecimal y = BigDecimal.valueOf(HAUT).add(HAUTEUR_TRACE.multiply(BigDecimal.valueOf(indice))
                .divide(BigDecimal.valueOf(LIGNES - 1), 2, RoundingMode.HALF_UP));
        return "<line x1=\"" + GAUCHE + "\" y1=\"" + y.toPlainString() + "\" x2=\"" + DROITE + "\" y2=\""
                + y.toPlainString() + "\" stroke=\"#dddddd\" stroke-width=\"1\"/>";
    }

    private static String date(int rang, List<PointCotation> points, int dernier) {
        int indice = rang * dernier / (DATES - 1);
        BigDecimal x = BigDecimal.valueOf(GAUCHE).add(LARGEUR_TRACE.multiply(BigDecimal.valueOf(rang))
                .divide(BigDecimal.valueOf(DATES - 1), 2, RoundingMode.HALF_UP));
        return "<text x=\"" + x.toPlainString() + "\" y=\"" + (BAS + 20)
                + "\" text-anchor=\"middle\" font-size=\"12\" fill=\"#666666\">"
                + points.get(indice).date().toString().substring(5) + "</text>";
    }

    private static BigDecimal abscisse(int indice, int dernier) {
        BigDecimal rang = BigDecimal.valueOf(indice);
        BigDecimal diviseur = BigDecimal.valueOf(Math.max(dernier, 1));
        return BigDecimal.valueOf(GAUCHE).add(LARGEUR_TRACE.multiply(rang).divide(diviseur, 2, RoundingMode.HALF_UP));
    }

    private static BigDecimal ordonnee(BigDecimal cloture, BigDecimal min, BigDecimal max, BigDecimal ecart) {
        BigDecimal milieu = BigDecimal.valueOf(HAUT).add(HAUTEUR_TRACE.divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP));
        if (ecart.signum() == 0) {
            return milieu;
        }
        return BigDecimal.valueOf(HAUT).add(HAUTEUR_TRACE.multiply(max.subtract(cloture))
                .divide(ecart, 2, RoundingMode.HALF_UP));
    }

    private static String coordonnee(BigDecimal x, BigDecimal y) {
        return x.toPlainString() + "," + y.toPlainString();
    }
}
