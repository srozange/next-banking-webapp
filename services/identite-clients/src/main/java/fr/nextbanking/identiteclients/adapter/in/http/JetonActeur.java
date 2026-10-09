package fr.nextbanking.identiteclients.adapter.in.http;

import fr.nextbanking.identiteclients.domain.Role;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Lecture de l'acteur d'un jeton : identifiant numérique du sujet (sinon 401) et rôles connus de la revendication
 * {@code roles}. Aucun contrôle de rôle ici : il appartient aux cas d'usage.
 */
final class JetonActeur {

    private JetonActeur() {
    }

    static long acteurId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }

    /** Rôles connus portés par la revendication {@code roles} ; les autres valeurs sont ignorées. */
    static Set<Role> roles(Jwt jeton) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        List<String> valeurs = jeton.getClaimAsStringList("roles");
        if (valeurs != null) {
            for (String valeur : valeurs) {
                for (Role role : Role.values()) {
                    if (role.name().equals(valeur)) {
                        roles.add(role);
                    }
                }
            }
        }
        return roles;
    }
}
