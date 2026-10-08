package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Operation;
import java.util.List;

/**
 * Port entrant : tableau de bord du client authentifié (comptes, solde total, dernières opérations du compte COURANT).
 */
public interface ConsulterTableauDeBord {

    /**
     * Vue d'ensemble du client.
     *
     * @param comptes comptes du client, par identifiant (vide sans compte propre)
     * @param soldeTotal somme des soldes de tous ses comptes, 2 décimales ({@code 0.00} sans compte)
     * @param dernieresOperations au plus 5 opérations du compte COURANT, de la plus récente à la plus ancienne ;
     *        vide sans compte COURANT
     */
    record TableauDeBord(List<Compte> comptes, Montant soldeTotal, List<Operation> dernieresOperations) {
    }

    TableauDeBord tableauDeBord(long clientId);
}
