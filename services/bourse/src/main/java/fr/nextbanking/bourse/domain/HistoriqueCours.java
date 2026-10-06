package fr.nextbanking.bourse.domain;

import java.util.List;

/**
 * Historique des cours d'une valeur sur une période : points filtrés et triés par date croissante ; liste vide quand
 * le fournisseur n'a rien rendu (FR-009-09, FR-029-03).
 */
public record HistoriqueCours(String symbole, Periode periode, List<PointCotation> points) {

    public HistoriqueCours {
        points = List.copyOf(points);
    }
}
