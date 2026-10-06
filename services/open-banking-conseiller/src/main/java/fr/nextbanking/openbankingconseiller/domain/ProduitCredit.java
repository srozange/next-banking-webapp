package fr.nextbanking.openbankingconseiller.domain;

import java.math.BigDecimal;

/**
 * Produit de crédit du catalogue. Sans précision d'assurance, elle n'est pas requise (FR-011-07).
 *
 * @param taux fraction décimale (0.0449 = 4,49 %), jamais multipliée côté serveur
 * @param dureeMaxMois durée maximale en mois
 * @param ltvMax quotité maximale financée (0.8000 = 80 %) ; absente si non applicable
 */
public record ProduitCredit(long id, String nom, String description, BigDecimal taux, Integer dureeMaxMois,
        BigDecimal ltvMax, Boolean assuranceRequise) {

    public ProduitCredit {
        if (assuranceRequise == null) {
            assuranceRequise = Boolean.FALSE;
        }
    }
}
