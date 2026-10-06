package fr.nextbanking.credits.domain;

import java.math.BigDecimal;

/**
 * Ligne d'un échéancier : montants à 2 décimales.
 */
public record LignePret(int numero, BigDecimal capital, BigDecimal interets, BigDecimal total,
        BigDecimal capitalRestant) {
}
