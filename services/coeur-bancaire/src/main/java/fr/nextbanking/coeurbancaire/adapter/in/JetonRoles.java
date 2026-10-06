package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;

/** Rôles connus portés par la revendication {@code roles} du jeton ; les autres valeurs sont ignorées. */
final class JetonRoles {

    private JetonRoles() {
    }

    static Set<Role> depuis(Jwt jeton) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        List<String> valeurs = jeton.getClaimAsStringList("roles");
        if (valeurs != null) {
            valeurs.forEach(valeur -> Role.depuis(valeur).ifPresent(roles::add));
        }
        return roles;
    }
}
