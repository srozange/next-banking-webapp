package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterReleve;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.ErreurReleve;
import fr.nextbanking.coeurbancaire.domain.ErreurReleve.Cause;
import fr.nextbanking.coeurbancaire.domain.Operation;
import java.util.Comparator;
import java.util.List;

/**
 * Cas d'usage : relevé d'un compte. La propriété du compte est décidée ici (NFR-SEC-03) et le tri par date
 * décroissante aussi (FR-005-05) ; le tri est stable, l'ordre du port départage les opérations de même date.
 */
public class ConsulterReleveCasUsage implements ConsulterReleve {

    private final Comptes comptes;
    private final Operations operations;

    public ConsulterReleveCasUsage(Comptes comptes, Operations operations) {
        this.comptes = comptes;
        this.operations = operations;
    }

    @Override
    public List<Operation> operations(long clientId, long compteId) {
        Compte compte = comptes.trouver(compteId).orElseThrow(() -> new ErreurReleve(Cause.COMPTE_INTROUVABLE));
        if (!compte.appartientA(clientId)) {
            throw new ErreurReleve(Cause.COMPTE_ETRANGER);
        }
        return operations.duCompte(compteId).stream()
                .sorted(Comparator.comparing(Operation::date).reversed())
                .toList();
    }
}
