package fr.nextbanking.documents.application;

/** Le compte existe mais appartient à un autre client : seul le titulaire télécharge ses documents, quel que soit son rôle. */
public class AccesCompteRefuse extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccesCompteRefuse(long compteId) {
        super("Compte " + compteId + " d'un autre client");
    }
}
