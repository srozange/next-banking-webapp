package fr.nextbanking.openbankingconseiller.application.port.in;

import fr.nextbanking.openbankingconseiller.domain.EtatPowens;

/** Cas d'usage : lire l'état Powens d'un client, sans appeler Powens. */
public interface ConsulterEtatPowens {

    EtatPowens consulter(String sub);
}
