package fr.nextbanking.coeurbancaire.domain;

import java.time.Duration;

/**
 * Délai {@code nb.fraude.acces-historique.delai} (2 s par défaut) au-delà duquel l'accès à l'historique du contrôle de
 * fraude est en erreur, validé au démarrage (NFR-SEC-08) : un délai absent, nul ou négatif est invalide.
 */
public record DelaiAccesHistorique(Duration valeur) {

    public DelaiAccesHistorique {
        if (valeur == null || valeur.isNegative() || valeur.isZero()) {
            throw new IllegalArgumentException(
                    "La propriété nb.fraude.acces-historique.delai doit être strictement positive.");
        }
    }

    /** Délai en millisecondes, au moins une. */
    public long millisecondes() {
        return Math.max(1, valeur.toMillis());
    }
}
