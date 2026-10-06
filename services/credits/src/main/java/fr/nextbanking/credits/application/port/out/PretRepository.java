package fr.nextbanking.credits.application.port.out;

import fr.nextbanking.credits.domain.Pret;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : prêts persistés.
 */
public interface PretRepository {

    /** Prêts du client, date de début décroissante. */
    List<Pret> parClient(long clientId);

    Optional<Pret> parId(long pretId);
}
