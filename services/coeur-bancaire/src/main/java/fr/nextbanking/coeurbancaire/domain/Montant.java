package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import java.math.BigDecimal;

/**
 * Montant en euros : un {@link BigDecimal} d'échelle exactement 2, jamais négatif. Les règles sont vérifiées à la
 * construction : aucune fabrique ne les contourne. Le texte JSON, d'audit et de notification est {@link #texte()}
 * ({@code "100.00"}).
 */
public record Montant(BigDecimal valeur) implements Comparable<Montant> {

    /** Nombre de décimales d'un montant. */
    public static final int DECIMALES = 2;
    /** Montant nul, à 2 décimales. */
    public static final Montant ZERO = new Montant(BigDecimal.ZERO.setScale(DECIMALES));

    private static final int CHIFFRES_ENTIERS_MAX = 17;

    public Montant {
        if (valeur == null) {
            throw new IllegalArgumentException("Le montant est obligatoire.");
        }
        if (valeur.scale() != DECIMALES) {
            throw new IllegalArgumentException("Un montant a exactement " + DECIMALES + " décimales.");
        }
        if (valeur.signum() < 0) {
            throw new IllegalArgumentException("Un montant n'est jamais négatif.");
        }
    }

    /**
     * Lit un montant saisi : espaces retirés, strictement positif, au plus 2 décimales et 17 chiffres entiers, mis à
     * l'échelle 2.
     *
     * @throws ErreurVirement {@code MONTANT_INVALIDE} si le texte est absent, illisible, nul, négatif ou trop précis
     */
    public static Montant lire(String texte) {
        if (texte == null) {
            throw new ErreurVirement(Cause.MONTANT_INVALIDE);
        }
        BigDecimal lu;
        try {
            lu = new BigDecimal(texte.strip());
        } catch (NumberFormatException e) {
            throw new ErreurVirement(Cause.MONTANT_INVALIDE);
        }
        if (lu.signum() <= 0 || lu.scale() > DECIMALES || lu.precision() - lu.scale() > CHIFFRES_ENTIERS_MAX) {
            throw new ErreurVirement(Cause.MONTANT_INVALIDE);
        }
        return new Montant(lu.setScale(DECIMALES));
    }

    /** Somme de deux montants. */
    public Montant ajouter(Montant autre) {
        return new Montant(valeur.add(autre.valeur));
    }

    /**
     * Différence de deux montants.
     *
     * @throws IllegalArgumentException si l'autre montant dépasse celui-ci (résultat négatif)
     */
    public Montant soustraire(Montant autre) {
        return new Montant(valeur.subtract(autre.valeur));
    }

    /** Vrai si ce montant atteint ou dépasse le seuil. */
    public boolean atteint(BigDecimal seuil) {
        return valeur.compareTo(seuil) >= 0;
    }

    /** Texte décimal à point, 2 décimales ({@code "100.00"}). */
    public String texte() {
        return valeur.toPlainString();
    }

    @Override
    public int compareTo(Montant autre) {
        return valeur.compareTo(autre.valeur);
    }

    @Override
    public String toString() {
        return texte();
    }
}
