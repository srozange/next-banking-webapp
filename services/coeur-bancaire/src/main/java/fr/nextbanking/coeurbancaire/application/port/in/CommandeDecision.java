package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.Set;

/**
 * Décision de l'administrateur sur un virement suspendu : acteur et rôles du jeton, virement, motif et adresse IP
 * d'origine (résolue par l'adaptateur entrant). Le rôle est vérifié par le cas d'usage, pas par le routage.
 */
public record CommandeDecision(long acteurId, Set<Role> roles, long virementId, String motif, String adresseIp) {

    public CommandeDecision {
        roles = Set.copyOf(roles);
    }
}
