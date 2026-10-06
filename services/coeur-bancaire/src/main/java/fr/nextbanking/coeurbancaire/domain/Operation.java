package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Opération inscrite au relevé d'un compte, avec le solde après l'opération.
 */
public record Operation(long compteId, TypeOperation type, BigDecimal montant, BigDecimal soldeApres,
        String description, Instant date, long virementId) {

    public Operation {
        if (type == null || montant == null || soldeApres == null || description == null || date == null) {
            throw new IllegalArgumentException("Une opération porte un type, un montant, un solde après, "
                    + "un libellé et une date.");
        }
    }
}
