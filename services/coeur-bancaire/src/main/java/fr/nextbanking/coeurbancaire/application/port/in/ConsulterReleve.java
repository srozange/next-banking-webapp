package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.Operation;
import java.util.List;

/**
 * Port entrant : relevé d'un compte du client authentifié.
 */
public interface ConsulterReleve {

    /**
     * Toutes les opérations du compte, de la plus récente à la plus ancienne.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurReleve compte inexistant ou d'un autre client
     */
    List<Operation> operations(long clientId, long compteId);
}
