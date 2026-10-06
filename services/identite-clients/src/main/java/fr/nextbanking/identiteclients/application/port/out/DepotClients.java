package fr.nextbanking.identiteclients.application.port.out;

import fr.nextbanking.identiteclients.domain.Client;
import java.util.Optional;

/**
 * Port sortant : clients enregistrés.
 */
public interface DepotClients {

    /** Client dont l'identifiant de connexion est donné, s'il existe. */
    Optional<Client> parIdentifiant(String identifiant);

    /** Client dont l'identifiant numérique est donné, s'il existe. */
    Optional<Client> parId(long clientId);
}
