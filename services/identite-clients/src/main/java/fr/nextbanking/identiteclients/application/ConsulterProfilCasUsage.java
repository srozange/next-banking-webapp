package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.ConsulterProfil;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.domain.Client;
import java.util.Optional;

/**
 * Cas d'usage : rend le profil du client connecté, désigné par le sujet de son jeton.
 */
public class ConsulterProfilCasUsage implements ConsulterProfil {

    private final DepotClients depot;

    public ConsulterProfilCasUsage(DepotClients depot) {
        this.depot = depot;
    }

    @Override
    public Optional<Client> consulter(long clientId) {
        return depot.parId(clientId);
    }
}
