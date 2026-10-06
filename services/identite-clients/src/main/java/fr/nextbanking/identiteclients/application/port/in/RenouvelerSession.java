package fr.nextbanking.identiteclients.application.port.in;

import java.util.Optional;

/**
 * Port entrant : renouveler la session d'un client déjà authentifié.
 */
public interface RenouvelerSession {

    /** Jeton neuf du client ; rien si le client est inconnu ou désactivé. */
    Optional<String> renouveler(long clientId);
}
