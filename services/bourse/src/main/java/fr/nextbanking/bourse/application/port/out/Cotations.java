package fr.nextbanking.bourse.application.port.out;

import fr.nextbanking.bourse.domain.PointCotation;
import fr.nextbanking.bourse.domain.TypeSerie;
import java.util.List;

/**
 * Port sortant : fournisseur de cotations (AlphaVantage, réel ou simulé). Rend la série demandée telle que fournie,
 * ou une liste vide si le symbole est inconnu ou si le fournisseur est indisponible ; ne lève jamais d'exception.
 */
public interface Cotations {

    List<PointCotation> serie(String symbole, TypeSerie type);
}
