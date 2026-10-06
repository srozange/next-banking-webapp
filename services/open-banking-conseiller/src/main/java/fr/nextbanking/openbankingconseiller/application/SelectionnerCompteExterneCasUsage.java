package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.SelectionnerCompteExterne;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.SituationPowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.util.List;

/**
 * Sélection d'un compte externe (FR-028-07, FR-014-07, BUG-043). Le compte doit être au client, sinon Powens n'est pas
 * appelé. Les transactions sont lues avant toute écriture : en cas de panne, l'état reste inchangé.
 */
public class SelectionnerCompteExterneCasUsage implements SelectionnerCompteExterne {

    private final Powens powens;
    private final DepotEtatPowens etat;
    private final DepotComptesExternes comptes;
    private final ConsulterEtatPowens consulter;

    public SelectionnerCompteExterneCasUsage(Powens powens, DepotEtatPowens etat, DepotComptesExternes comptes,
            ConsulterEtatPowens consulter) {
        this.powens = powens;
        this.etat = etat;
        this.comptes = comptes;
        this.consulter = consulter;
    }

    @Override
    public SituationPowens selectionner(String sub, long compteId) {
        if (!comptes.appartient(sub, compteId)) {
            throw new CompteExterneIntrouvable(compteId);
        }
        String jeton = etat.lire(sub).map(EtatPowens::jeton).orElse(null);
        if (jeton == null || jeton.isBlank()) {
            throw new PowensNonConnecte();
        }
        List<TransactionExterne> transactions = powens.transactions(jeton, compteId);
        comptes.enregistrerSelection(sub, compteId,
                transactions.subList(0, Math.min(TraiterRetourPowensCasUsage.TRANSACTIONS_MAX, transactions.size())));
        return consulter.consulter(sub);
    }
}
