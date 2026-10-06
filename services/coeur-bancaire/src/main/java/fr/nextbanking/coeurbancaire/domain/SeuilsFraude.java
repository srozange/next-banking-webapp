package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;

/**
 * Seuils de configuration {@code nb.fraude.*} du cœur bancaire, validés au démarrage (NFR-SEC-08) : un seuil absent,
 * nul ou négatif est invalide. {@code seuilAttente} : montant à partir duquel le virement est PENDING ;
 * {@code seuilAlerte} : montant à partir duquel la notification est une ALERTE (story 3.3).
 */
public record SeuilsFraude(BigDecimal seuilAttente, BigDecimal seuilAlerte) {

    public SeuilsFraude {
        exigerStrictementPositif("nb.fraude.seuil-attente", seuilAttente);
        exigerStrictementPositif("nb.fraude.seuil-alerte", seuilAlerte);
    }

    private static void exigerStrictementPositif(String propriete, BigDecimal seuil) {
        if (seuil == null || seuil.signum() <= 0) {
            throw new IllegalArgumentException("La propriété " + propriete + " doit être strictement positive.");
        }
    }
}
