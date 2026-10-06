package fr.nextbanking.identiteclients.application.port.in;

import fr.nextbanking.identiteclients.domain.Client;
import java.util.Optional;

/**
 * Port entrant : consulter le profil du client connecté.
 */
public interface ConsulterProfil {

    /** Rend le client désigné par son identifiant numérique, s'il existe. */
    Optional<Client> consulter(long clientId);
}
