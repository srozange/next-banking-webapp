package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.RafraichirComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.SituationPowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.util.List;

/**
 * Rafraîchissement des comptes (FR-028-08, FR-028-09, FR-012-10, BUG-043, BUG-089). Comptes et transactions sont lus
 * avant toute écriture ; le compte sélectionné est conservé s'il existe encore, sinon le premier est retenu.
 */
public class RafraichirComptesExternesCasUsage implements RafraichirComptesExternes {

    private final Powens powens;
    private final DepotEtatPowens etat;
    private final DepotComptesExternes comptes;
    private final ConsulterEtatPowens consulter;

    public RafraichirComptesExternesCasUsage(Powens powens, DepotEtatPowens etat, DepotComptesExternes comptes,
            ConsulterEtatPowens consulter) {
        this.powens = powens;
        this.etat = etat;
        this.comptes = comptes;
        this.consulter = consulter;
    }

    @Override
    public SituationPowens rafraichir(String sub) {
        String jeton = etat.lire(sub).map(EtatPowens::jeton).orElse(null);
        if (jeton == null || jeton.isBlank()) {
            throw new PowensNonConnecte();
        }
        List<CompteExterne> lus = powens.comptes(jeton);
        if (lus.isEmpty()) {
            etat.enregistrerConnexion(sub, lus, null, List.of(), TraiterRetourPowensCasUsage.AUCUN_COMPTE);
            return consulter.consulter(sub);
        }
        Long precedent = comptes.compteSelectionne(sub).orElse(null);
        long selectionne = precedent != null && lus.stream().anyMatch(compte -> compte.id() == precedent)
                ? precedent
                : lus.get(0).id();
        List<TransactionExterne> transactions = powens.transactions(jeton, selectionne);
        etat.enregistrerConnexion(sub, lus, selectionne,
                transactions.subList(0, Math.min(TraiterRetourPowensCasUsage.TRANSACTIONS_MAX, transactions.size())),
                null);
        return consulter.consulter(sub);
    }
}
