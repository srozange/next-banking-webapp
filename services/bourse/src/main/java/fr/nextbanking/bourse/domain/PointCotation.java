package fr.nextbanking.bourse.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Point d'une série de cours : séance (ou semaine) datée, prix en décimal exact et volume échangé.
 */
public record PointCotation(LocalDate date, BigDecimal ouverture, BigDecimal haut, BigDecimal bas, BigDecimal cloture,
        long volume) {
}
