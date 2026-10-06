package fr.nextbanking.bourse.application.port.in;

import fr.nextbanking.bourse.domain.HistoriqueCours;

/**
 * Port entrant : historique des cours d'une valeur sur une période (FR-009-08, FR-029-01, FR-029-02).
 */
public interface ConsulterHistorique {

    /**
     * @param symbole symbole saisi, rogné, casse inchangée ; absent ou blanc → {@code SymboleManquant}
     * @param periode code de période (1W, 1M, 1Y, 5Y) ; absent ou inconnu → 1M
     */
    HistoriqueCours consulter(String symbole, String periode);
}
