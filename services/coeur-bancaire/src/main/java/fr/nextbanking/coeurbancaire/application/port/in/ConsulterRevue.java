package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.LigneRevue;
import fr.nextbanking.coeurbancaire.domain.LigneTraitee;
import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.List;
import java.util.Set;

/** Port entrant : file de revue et historique des virements traités, réservés à l'administrateur. */
public interface ConsulterRevue {

    /**
     * Virements suspendus, heure butoir la plus proche d'abord.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement rôle insuffisant
     */
    List<LigneRevue> fileDeRevue(Set<Role> roles);

    /**
     * Les 50 derniers virements libérés ou rejetés, du plus récent au plus ancien.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement rôle insuffisant
     */
    List<LigneTraitee> historique(Set<Role> roles);
}
