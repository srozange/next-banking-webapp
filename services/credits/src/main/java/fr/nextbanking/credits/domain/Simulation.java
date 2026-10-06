package fr.nextbanking.credits.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat d'une simulation d'emprunt : mensualité, coût total, total des intérêts et échéancier complet.
 */
public record Simulation(BigDecimal mensualite, BigDecimal coutTotal, BigDecimal totalInterets,
        List<LignePret> echeancier) {
}
