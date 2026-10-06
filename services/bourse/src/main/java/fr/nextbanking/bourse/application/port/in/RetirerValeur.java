package fr.nextbanking.bourse.application.port.in;

import fr.nextbanking.bourse.domain.ValeurIntrouvable;

/**
 * Port entrant : retire une valeur de la watchlist commune, sans confirmation (FR-009-13).
 */
public interface RetirerValeur {

    /**
     * Retire la valeur d'identifiant donné.
     *
     * @throws ValeurIntrouvable si aucune valeur ne porte cet identifiant
     */
    void retirer(long id);
}
