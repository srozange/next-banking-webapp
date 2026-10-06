package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.Echeance;
import java.util.List;

/**
 * Port entrant : échéancier d'un prêt de son propriétaire (FR-007-11).
 */
public interface ConsulterEcheances {

    /**
     * Échéances triées par numéro ; vide si le prêt n'est ni EN_COURS ni REMBOURSE.
     *
     * @throws fr.nextbanking.credits.application.PretIntrouvable si le prêt n'existe pas
     * @throws fr.nextbanking.credits.application.AccesPretRefuse si le prêt appartient à un autre client
     */
    List<Echeance> consulter(long clientId, long pretId);
}
