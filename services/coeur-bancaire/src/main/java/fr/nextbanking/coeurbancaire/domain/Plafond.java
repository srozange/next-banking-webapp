package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Plafond mensuel de dépenses d'un client pour une catégorie (FEAT-006) : au plus un par client, catégorie et mois
 * ({@code AAAA-MM}).
 */
public record Plafond(long id, CategorieBudget categorie, String mois, Montant montant) {

    /** Nombre maximal de chiffres avant la virgule. */
    public static final int CHIFFRES_ENTIERS_MAX = 13;

    /**
     * Lit un plafond saisi : espaces retirés, strictement positif, arrondi à 2 décimales (moitié vers le haut), au plus
     * 13 chiffres entiers.
     *
     * @throws ErreurPlafond {@code PLAFOND_INVALIDE} si le texte est absent, illisible, nul ou négatif ;
     *     {@code PLAFOND_TROP_GRAND} au-delà de 13 chiffres entiers
     */
    public static Montant lire(String texte) {
        if (texte == null) {
            throw new ErreurPlafond(ErreurPlafond.Cause.PLAFOND_INVALIDE);
        }
        BigDecimal lu;
        try {
            lu = new BigDecimal(texte.strip());
        } catch (NumberFormatException e) {
            throw new ErreurPlafond(ErreurPlafond.Cause.PLAFOND_INVALIDE);
        }
        if (lu.signum() <= 0) {
            throw new ErreurPlafond(ErreurPlafond.Cause.PLAFOND_INVALIDE);
        }
        if (chiffresEntiers(lu) > CHIFFRES_ENTIERS_MAX) {
            throw new ErreurPlafond(ErreurPlafond.Cause.PLAFOND_TROP_GRAND);
        }
        BigDecimal arrondi = lu.setScale(Montant.DECIMALES, RoundingMode.HALF_UP);
        if (chiffresEntiers(arrondi) > CHIFFRES_ENTIERS_MAX) {
            throw new ErreurPlafond(ErreurPlafond.Cause.PLAFOND_TROP_GRAND);
        }
        if (arrondi.signum() == 0) {
            throw new ErreurPlafond(ErreurPlafond.Cause.PLAFOND_INVALIDE);
        }
        return new Montant(arrondi);
    }

    private static long chiffresEntiers(BigDecimal valeur) {
        return (long) valeur.precision() - valeur.scale();
    }
}
