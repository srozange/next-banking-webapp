package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.domain.AccesRefuse;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.Set;

/**
 * Point unique du contrôle des rôles d'administration et de conseil : seule classe de production qui lit
 * {@code Role.ADMIN}. Un refus lève {@link AccesRefuse} (403), sans rien modifier ni consigner.
 */
final class DroitsRoles {

    private DroitsRoles() {
    }

    /** Exige le rôle ADMIN. */
    static void exigerAdmin(Set<Role> roles) {
        if (roles == null || !roles.contains(Role.ADMIN)) {
            throw new AccesRefuse();
        }
    }

    /** Exige le rôle CONSEILLER ou ADMIN. */
    static void exigerConseillerOuAdmin(Set<Role> roles) {
        if (roles == null || !(roles.contains(Role.CONSEILLER) || roles.contains(Role.ADMIN))) {
            throw new AccesRefuse();
        }
    }
}
