package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.Role;
import java.util.Set;

/**
 * Port entrant : approbation ou refus manuel d'un prêt EN_ATTENTE par l'administrateur (FR-016-04, FR-016-05).
 */
public interface DecisionAdministrateurPret {

    /**
     * Approuve (EN_COURS avec échéancier) le prêt. Un prêt inconnu ou qui n'est plus EN_ATTENTE est ignoré, sans erreur,
     * échéancier ni audit.
     */
    void approuver(Set<Role> roles, String administrateur, long pretId, String adresseIp);

    /** Refuse le prêt (REFUSE), mêmes règles. */
    void refuser(Set<Role> roles, String administrateur, long pretId, String adresseIp);
}
