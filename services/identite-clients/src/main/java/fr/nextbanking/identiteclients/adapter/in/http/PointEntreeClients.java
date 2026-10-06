package fr.nextbanking.identiteclients.adapter.in.http;

import fr.nextbanking.identiteclients.application.port.in.ChangerEtatClient;
import fr.nextbanking.identiteclients.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP de la gestion des clients : {@code POST /api/identite-clients/clients/{clientId}/desactivation}
 * et {@code /activation} (204). Le rôle ADMIN est contrôlé par le cas d'usage ; l'adaptateur n'extrait que
 * l'acteur et ses rôles du jeton.
 */
@RestController
public class PointEntreeClients {

    private static final Logger JOURNAL = LoggerFactory.getLogger(PointEntreeClients.class);

    private final ChangerEtatClient changerEtatClient;
    private final ResolveurAdresseIp resolveurAdresseIp;

    public PointEntreeClients(ChangerEtatClient changerEtatClient, ResolveurAdresseIp resolveurAdresseIp) {
        this.changerEtatClient = changerEtatClient;
        this.resolveurAdresseIp = resolveurAdresseIp;
    }

    @PostMapping("/api/identite-clients/clients/{clientId}/desactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactiver(@AuthenticationPrincipal Jwt jeton, @PathVariable long clientId,
            HttpServletRequest requete) {
        long acteurId = acteurId(jeton);
        changerEtatClient.desactiver(acteurId, roles(jeton), clientId, resolveurAdresseIp.resoudre(requete));
        JOURNAL.info("Client {} désactivé par l'administrateur {}", clientId, acteurId);
    }

    @PostMapping("/api/identite-clients/clients/{clientId}/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activer(@AuthenticationPrincipal Jwt jeton, @PathVariable long clientId,
            HttpServletRequest requete) {
        long acteurId = acteurId(jeton);
        changerEtatClient.activer(acteurId, roles(jeton), clientId, resolveurAdresseIp.resoudre(requete));
        JOURNAL.info("Client {} activé par l'administrateur {}", clientId, acteurId);
    }

    private static long acteurId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }

    /** Rôles connus portés par la revendication {@code roles} ; les autres valeurs sont ignorées. */
    private static Set<Role> roles(Jwt jeton) {
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
