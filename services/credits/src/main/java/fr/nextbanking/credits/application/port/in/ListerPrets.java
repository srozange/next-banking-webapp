package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.Pret;
import java.util.List;

/**
 * Port entrant : lister les prêts d'un client.
 */
public interface ListerPrets {

    /** Prêts du client, date de début décroissante. */
    List<Pret> lister(long clientId);
}
