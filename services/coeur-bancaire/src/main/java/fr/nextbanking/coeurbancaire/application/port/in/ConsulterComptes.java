package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.Compte;
import java.util.List;

/**
 * Port entrant : lister les comptes du client authentifié.
 */
public interface ConsulterComptes {

    /** Comptes du client, par identifiant ; vide pour un utilisateur sans compte propre (CONSEILLER, ADMIN). */
    List<Compte> comptesDuClient(long clientId);
}
