package fr.nextbanking.identiteclients.domain;

/**
 * Client demandé inexistant.
 */
public class ClientIntrouvable extends RuntimeException {

    public ClientIntrouvable() {
        super("Client introuvable");
    }
}
