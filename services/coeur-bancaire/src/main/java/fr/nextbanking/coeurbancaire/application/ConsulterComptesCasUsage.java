package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterComptes;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.domain.Compte;
import java.util.List;

/**
 * Cas d'usage : comptes du client authentifié (propriété décidée par le service, NFR-SEC-03).
 */
public class ConsulterComptesCasUsage implements ConsulterComptes {

    private final Comptes comptes;

    public ConsulterComptesCasUsage(Comptes comptes) {
        this.comptes = comptes;
    }

    @Override
    public List<Compte> comptesDuClient(long clientId) {
        return comptes.duClient(clientId);
    }
}
