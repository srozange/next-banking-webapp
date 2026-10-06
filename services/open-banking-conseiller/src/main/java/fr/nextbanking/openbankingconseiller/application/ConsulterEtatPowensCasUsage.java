package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;

/** Lecture de l'état Powens d'un client en base : aucun appel à Powens (FR-012-17). */
public class ConsulterEtatPowensCasUsage implements ConsulterEtatPowens {

    private final DepotEtatPowens depot;

    public ConsulterEtatPowensCasUsage(DepotEtatPowens depot) {
        this.depot = depot;
    }

    @Override
    public EtatPowens consulter(String sub) {
        return depot.lire(sub).orElseGet(EtatPowens::nonConnecte);
    }
}
