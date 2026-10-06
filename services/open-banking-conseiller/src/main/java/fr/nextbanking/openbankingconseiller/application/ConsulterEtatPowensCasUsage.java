package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.SituationPowens;

/** Lecture de la situation Powens d'un client en base : aucun appel à Powens (FR-012-17, FR-012-10). */
public class ConsulterEtatPowensCasUsage implements ConsulterEtatPowens {

    private final DepotEtatPowens depot;
    private final DepotComptesExternes comptes;

    public ConsulterEtatPowensCasUsage(DepotEtatPowens depot, DepotComptesExternes comptes) {
        this.depot = depot;
        this.comptes = comptes;
    }

    @Override
    public SituationPowens consulter(String sub) {
        EtatPowens etat = depot.lire(sub).orElse(null);
        if (etat == null) {
            return SituationPowens.vide();
        }
        return new SituationPowens(etat, comptes.comptes(sub), comptes.compteSelectionne(sub).orElse(null),
                comptes.transactions(sub));
    }
}
