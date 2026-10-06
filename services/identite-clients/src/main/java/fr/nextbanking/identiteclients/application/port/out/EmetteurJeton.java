package fr.nextbanking.identiteclients.application.port.out;

import fr.nextbanking.identiteclients.domain.Client;

/**
 * Port sortant : émission du jeton d'accès signé d'un client authentifié.
 */
public interface EmetteurJeton {

    /** Émet un jeton neuf pour le client. */
    String emettre(Client client);
}
