package fr.nextbanking.openbankingconseiller.application.port.in;

import fr.nextbanking.openbankingconseiller.domain.SituationPowens;

/** Cas d'usage : lire la situation Powens d'un client en base, sans appeler Powens. */
public interface ConsulterEtatPowens {

    SituationPowens consulter(String sub);
}
