package fr.nextbanking.credits.application;

import fr.nextbanking.credits.domain.Role;
import java.util.Set;

/** Contrôle commun aux cas d'usage de l'administration : rôle ADMIN exigé (D6). */
final class DroitsAdministrateur {

    private DroitsAdministrateur() {
    }

    /** Refuse tout rôle autre qu'ADMIN. */
    static void exigerAdmin(Set<Role> roles) {
        if (!roles.contains(Role.ADMIN)) {
            throw new AccesAdministrationRefuse();
        }
    }
}
