package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.DeconnecterPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;

/** Déconnexion (FR-028-10) : jeton, comptes, sélection, transactions, connexion et message sont effacés. */
public class DeconnecterPowensCasUsage implements DeconnecterPowens {

    private final DepotEtatPowens depot;

    public DeconnecterPowensCasUsage(DepotEtatPowens depot) {
        this.depot = depot;
    }

    @Override
    public void deconnecter(String sub) {
        depot.remettreAZero(sub);
    }
}
