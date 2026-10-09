package fr.nextbanking.identiteclients.application.port.in;

import fr.nextbanking.identiteclients.domain.ClientDuPortefeuille;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.List;
import java.util.Set;

/**
 * Port entrant : portefeuille du conseiller désigné par le sujet du jeton, réservé aux rôles CONSEILLER et ADMIN
 * (refus : {@code AccesRefuse}). Le portefeuille est déduit de l'acteur seul, jamais d'un paramètre de la requête.
 */
public interface ConsulterPortefeuille {

    /** Clients non supprimés affectés à l'acteur, actifs et inactifs, triés par nom complet puis identifiant. */
    List<ClientDuPortefeuille> consulter(long acteurId, Set<Role> rolesActeur);
}
