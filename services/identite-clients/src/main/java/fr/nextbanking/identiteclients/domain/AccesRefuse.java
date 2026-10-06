package fr.nextbanking.identiteclients.domain;

/**
 * Accès refusé : le rôle de l'acteur ne permet pas l'action demandée. Le message ne porte aucun identifiant.
 */
public class AccesRefuse extends RuntimeException {

    public AccesRefuse() {
        super("Accès refusé : rôle insuffisant");
    }
}
