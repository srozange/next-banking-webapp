package fr.nextbanking.documents.application;

/** Le titulaire est absent de la projection des clients. */
public class ClientIntrouvable extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClientIntrouvable(long clientId) {
        super("Client " + clientId + " absent de la projection");
    }
}
