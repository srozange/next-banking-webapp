package fr.nextbanking.identiteclients.application.port.out;

import fr.nextbanking.identiteclients.domain.ClientDuPortefeuille;
import java.util.List;

/**
 * Port sortant : clients affectés à un conseiller.
 */
public interface PortefeuilleClients {

    /** Clients non supprimés dont le conseiller est celui donné, actifs et inactifs. */
    List<ClientDuPortefeuille> duConseiller(long conseillerId);
}
