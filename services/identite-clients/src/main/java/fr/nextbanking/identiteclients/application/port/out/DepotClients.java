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

    /** Fixe l'état actif du client ({@code customers.active}) et incrémente {@code version_agregat}, dans la transaction courante. */
    void changerActif(long clientId, boolean actif);
}
