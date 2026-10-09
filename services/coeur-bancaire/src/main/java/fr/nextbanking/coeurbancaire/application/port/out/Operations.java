package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Operation;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * Port sortant : opérations au relevé.
 */
public interface Operations {

    /** Enregistre l'opération et rend son identifiant généré. */
    long enregistrer(Operation operation);

    /** Toutes les opérations du compte, sans limite, de la plus récente à la plus ancienne (date puis identifiant). */
    List<Operation> duCompte(long compteId);

    /**
     * Les {@code limite} mois les plus récents (calculés dans le fuseau) où le client a au moins une opération sur
     * l'un de ses comptes, du plus récent au plus ancien.
     */
    List<YearMonth> moisAvecOperations(long clientId, ZoneId fuseau, int limite);

    /**
     * Opérations de tous les comptes du client dont la date est dans {@code [debut, fin[}, de la plus ancienne à la
     * plus récente.
     */
    List<Operation> duClientSurPeriode(long clientId, Instant debut, Instant fin);
}
