package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.LigneSuivi;
import java.util.List;

/** Port entrant : l'état des virements passés en revue du client authentifié. */
public interface ConsulterSuiviVirements {

    /** Les quatre premiers virements passés en revue du client, du plus récent au plus ancien. */
    List<LigneSuivi> suivi(long clientId);
}
