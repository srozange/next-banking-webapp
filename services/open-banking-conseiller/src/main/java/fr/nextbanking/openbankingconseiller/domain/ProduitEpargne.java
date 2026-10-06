package fr.nextbanking.openbankingconseiller.domain;

import java.math.BigDecimal;

/**
 * Produit d'épargne du catalogue. Sans précision de garantie, le capital est garanti (FR-011-07).
 *
 * @param taux fraction décimale (0.0300 = 3 %), jamais multipliée côté serveur
 * @param depotMin dépôt minimum à l'ouverture ; absent si inconnu
 * @param dureeMaxMois durée maximale en mois ; absente si illimitée
 */
public record ProduitEpargne(long id, String nom, String description, BigDecimal taux, BigDecimal depotMin,
        Boolean capitalGaranti, Integer dureeMaxMois) {

    public ProduitEpargne {
        if (capitalGaranti == null) {
            capitalGaranti = Boolean.TRUE;
        }
    }
}
