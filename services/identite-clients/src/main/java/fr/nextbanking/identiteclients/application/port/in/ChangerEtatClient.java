package fr.nextbanking.identiteclients.application.port.in;

import fr.nextbanking.identiteclients.domain.Role;
import java.util.Set;

/**
 * Port entrant : désactiver ou réactiver un client, réservé au rôle ADMIN (contrôlé ici, pas seulement par
 * l'adaptateur). Refus de rôle : {@code AccesRefuse} ; client inconnu : {@code ClientIntrouvable} ; dans les deux
 * cas, rien n'est modifié ni consigné.
 */
public interface ChangerEtatClient {

    /** Désactive le client ; consigne ADMIN_DEACTIVATE (adresse IP déjà résolue par l'adaptateur entrant). */
    void desactiver(long acteurId, Set<Role> rolesActeur, long clientId, String adresseIp);

    /** Réactive le client ; consigne ADMIN_ACTIVATE. */
    void activer(long acteurId, Set<Role> rolesActeur, long clientId, String adresseIp);
}
