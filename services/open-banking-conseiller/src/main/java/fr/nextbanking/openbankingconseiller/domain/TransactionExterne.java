package fr.nextbanking.openbankingconseiller.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Transaction d'un compte externe : libellé simplifié, sinon libellé d'origine. */
public record TransactionExterne(long id, LocalDate date, String libelle, BigDecimal montant) {
}
