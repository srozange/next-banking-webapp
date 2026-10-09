package fr.nextbanking.credits.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Indicateur « Prets en attente » de l'administration (FR-016-02) : nombre de prêts EN_ATTENTE et cumul de leurs
 * montants à 2 décimales ({@code 0.00} sans prêt).
 */
public record IndicateursPrets(long nombre, BigDecimal montant) {

    /** Indicateur calculé sur les prêts donnés (déjà limités aux prêts EN_ATTENTE). */
    public static IndicateursPrets de(List<Pret> enAttente) {
        BigDecimal cumul = enAttente.stream().map(Pret::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new IndicateursPrets(enAttente.size(), cumul.setScale(2, RoundingMode.HALF_UP));
    }
}
