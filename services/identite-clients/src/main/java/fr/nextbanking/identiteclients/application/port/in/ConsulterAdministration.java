package fr.nextbanking.identiteclients.application.port.in;

import fr.nextbanking.identiteclients.domain.FicheUtilisateur;
import fr.nextbanking.identiteclients.domain.IndicateursUtilisateurs;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.List;
import java.util.Set;

/**
 * Port entrant : données de la page d'administration, réservées au rôle ADMIN (refus : {@code AccesRefuse}).
 */
public interface ConsulterAdministration {

    /** Indicateurs globaux (FR-016-02). */
    IndicateursUtilisateurs indicateurs(Set<Role> rolesActeur);

    /** Utilisateurs de rôle CLIENT, triés par nom complet puis identifiant (FR-016-08). */
    List<FicheUtilisateur> clients(Set<Role> rolesActeur);

    /** Utilisateurs de tous rôles, triés par nom complet puis identifiant (FR-016-13). */
    List<FicheUtilisateur> utilisateurs(Set<Role> rolesActeur);
}
