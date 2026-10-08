package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.Montant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Port entrant : budget d'un mois du client authentifié (FEAT-006).
 */
public interface ConsulterBudget {

    /**
     * Budget de la période demandée ({@code AAAA-MM}). Une période absente ou illisible n'est pas une erreur : le mois
     * le plus récent ayant des opérations est pris, à défaut le mois du jour.
     */
    Budget budget(long clientId, String periode);

    /** Dépense d'une catégorie sur la période. */
    record LigneRepartition(CategorieBudget categorie, Montant montant) {
    }

    /**
     * Vue du budget : date du jour du service, période retenue, périodes proposées (24 au plus, de la plus récente à la
     * plus ancienne), totaux, nombre de plafonds définis et répartition des débits (catégories non nulles, dans l'ordre
     * des catégories).
     */
    record Budget(LocalDate aujourdhui, YearMonth periode, List<YearMonth> periodes, Montant totalDepenses,
            Montant totalRevenus, long plafondsDefinis, List<LigneRepartition> repartition) {
    }
}
