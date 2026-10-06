package fr.nextbanking.openbankingconseiller.domain;

import java.math.BigDecimal;

/**
 * Produit d'investissement du catalogue.
 *
 * @param niveauRisque de 1 (faible) à 5 (très élevé) ; son libellé reste au front
 * @param rendementAttendu fraction décimale (0.0720 = 7,20 %), jamais multipliée côté serveur
 */
public record ProduitInvestissement(long id, String nom, String description, String secteur, Integer niveauRisque,
        BigDecimal rendementAttendu) {
}
