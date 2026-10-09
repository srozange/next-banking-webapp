package fr.nextbanking.documents.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ligne imprimée au relevé : le jour de l'opération dans le fuseau métier, son libellé, son sens et ses montants. */
public record LigneReleve(LocalDate date, String libelle, TypeOperation type, BigDecimal montant,
        BigDecimal soldeApres) {
}
