package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.ClientEtSesPrets;
import fr.nextbanking.credits.domain.Role;
import java.util.List;
import java.util.Set;

/**
 * Port entrant : prêts des clients du conseiller désigné par le sujet du jeton, réservé aux rôles CONSEILLER et ADMIN
 * (refus : {@code AccesAdministrationRefuse}). Le portefeuille est déduit de la projection locale, jamais d'un
 * paramètre de la requête.
 */
public interface ConsulterPortefeuille {

    /** Un élément par client du portefeuille, trié par identifiant ; prêts date de début décroissante. */
    List<ClientEtSesPrets> consulter(long acteurId, Set<Role> rolesActeur);
}
