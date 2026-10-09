package fr.nextbanking.credits.application;

import fr.nextbanking.credits.domain.Role;
import java.util.Set;

/**
 * Point unique du contrôle de rôle (seule classe qui lit {@code Role.ADMIN}) : rôle ADMIN pour l'administration (D6),
 * CONSEILLER ou ADMIN pour le portefeuille du conseiller.
 */
final class DroitsAdministrateur {

    private DroitsAdministrateur() {
    }

    /** Refuse tout rôle autre qu'ADMIN. */
    static void exigerAdmin(Set<Role> roles) {
        if (!roles.contains(Role.ADMIN)) {
            throw new AccesAdministrationRefuse();
        }
    }

    /** Refuse tout rôle autre que CONSEILLER ou ADMIN. */
    static void exigerConseillerOuAdmin(Set<Role> roles) {
        if (!roles.contains(Role.CONSEILLER) && !roles.contains(Role.ADMIN)) {
            throw new AccesAdministrationRefuse();
        }
    }
}
