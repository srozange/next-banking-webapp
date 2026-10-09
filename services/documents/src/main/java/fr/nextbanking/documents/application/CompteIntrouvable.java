package fr.nextbanking.documents.application;

/** Le compte demandé est absent de la projection des comptes. */
public class CompteIntrouvable extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CompteIntrouvable(long compteId) {
        super("Compte " + compteId + " absent de la projection");
    }
}
