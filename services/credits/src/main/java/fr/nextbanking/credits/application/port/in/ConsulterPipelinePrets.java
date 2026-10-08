package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.PretAvecClient;
import fr.nextbanking.credits.domain.Role;
import java.util.List;
import java.util.Set;

/**
 * Port entrant : prêts vus par l'administrateur (FR-016-03, FR-016-07). Tout rôle autre qu'ADMIN est refusé.
 */
public interface ConsulterPipelinePrets {

    /** Prêts EN_ATTENTE avec le nom complet du client. */
    List<PretAvecClient> enAttente(Set<Role> roles);

    /** Tous les prêts, tous statuts, date de début décroissante. */
    List<PretAvecClient> historique(Set<Role> roles);
}
