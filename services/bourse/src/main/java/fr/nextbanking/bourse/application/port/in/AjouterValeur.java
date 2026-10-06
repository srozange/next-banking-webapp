package fr.nextbanking.bourse.application.port.in;

import fr.nextbanking.bourse.domain.AjoutRefuse;
import fr.nextbanking.bourse.domain.ValeurWatchlist;

/**
 * Port entrant : ajoute une valeur à la watchlist commune (FR-009-11, FR-009-12, FR-029-09 à FR-029-14).
 */
public interface AjouterValeur {

    /**
     * Ajoute la valeur saisie, normalisée.
     *
     * @throws AjoutRefuse si la saisie est refusée ; rien n'est alors enregistré
     */
    ValeurWatchlist ajouter(String symbole, String nom, String marche, String devise);
}
