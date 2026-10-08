package fr.nextbanking.credits.application;

/**
 * Le rôle ADMIN est exigé : un autre rôle est refusé, sans rien lire ni modifier.
 */
public class AccesAdministrationRefuse extends RuntimeException {

    public AccesAdministrationRefuse() {
        super("Rôle ADMIN exigé");
    }
}
