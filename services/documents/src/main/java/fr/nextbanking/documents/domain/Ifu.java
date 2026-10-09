package fr.nextbanking.documents.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Imprimé fiscal unique d'une année : le total des intérêts crédités et le prélèvement forfaitaire unique, 30 % du
 * total arrondi au demi supérieur à 2 décimales.
 */
public record Ifu(int annee, BigDecimal totalInterets, BigDecimal pfu) {

    private static final BigDecimal TAUX_PFU = new BigDecimal("0.30");

    /** IFU de l'année pour ce total d'intérêts (calcul en {@link BigDecimal}, jamais en flottant). */
    public static Ifu de(int annee, BigDecimal totalInterets) {
        BigDecimal total = totalInterets.setScale(2, RoundingMode.HALF_UP);
        return new Ifu(annee, total, total.multiply(TAUX_PFU).setScale(2, RoundingMode.HALF_UP));
    }
}
