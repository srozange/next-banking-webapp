package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.ClientEtSesComptes;
import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.List;
import java.util.Set;

/**
 * Port entrant : comptes des clients du conseiller désigné par le sujet du jeton, réservé aux rôles CONSEILLER et
 * ADMIN (refus : {@code ErreurVirement} {@code ROLE_INSUFFISANT}). Le portefeuille est déduit de la projection locale,
 * jamais d'un paramètre de la requête.
 */
public interface ConsulterPortefeuille {

    /** Un élément par client du portefeuille, trié par identifiant, comptes triés par identifiant. */
    List<ClientEtSesComptes> consulter(long acteurId, Set<Role> rolesActeur);
}
