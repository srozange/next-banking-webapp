package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.AlerteAdministree;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import java.util.List;
import java.util.Set;

/** Port entrant : liste et revue des alertes de fraude, réservées à l'administrateur (FR-016-14 à 16). */
public interface RevoirAlertesFraude {

    /**
     * Toutes les alertes, tous statuts, la plus récente d'abord.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement rôle insuffisant
     */
    List<AlerteAdministree> toutes(Set<Role> roles);

    /**
     * Passe une alerte encore OPEN à {@code CONFIRMED} ou {@code FALSE_POSITIVE}, sans aucun effet sur le virement
     * lié. Un identifiant nul ou une alerte déjà traitée ne change rien. Rend vrai si l'alerte a changé.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement rôle insuffisant
     * @throws IllegalArgumentException issue autre que {@code CONFIRMED} ou {@code FALSE_POSITIVE}
     */
    boolean revoir(Long alerteId, StatutAlerte issue, long acteurId, Set<Role> roles);
}
