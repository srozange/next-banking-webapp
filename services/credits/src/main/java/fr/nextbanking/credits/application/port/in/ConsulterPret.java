package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.Pret;

/**
 * Port entrant : consulter un prêt de son propriétaire.
 */
public interface ConsulterPret {

    /**
     * @throws fr.nextbanking.credits.application.PretIntrouvable si le prêt n'existe pas
     * @throws fr.nextbanking.credits.application.AccesPretRefuse si le prêt appartient à un autre client
     */
    Pret consulter(long clientId, long pretId);
}
