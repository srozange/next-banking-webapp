package fr.nextbanking.credits.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Échéance d'un prêt telle que persistée : identifiant, prêt, numéro, date, montants, état de paiement et date du
 * paiement ({@code null} tant qu'elle n'est pas payée).
 */
public record Echeance(long id, long pretId, int numero, LocalDate dateEcheance, BigDecimal capital,
        BigDecimal interets, BigDecimal total, BigDecimal capitalRestant, boolean paye, Instant datePaiement) {
}
