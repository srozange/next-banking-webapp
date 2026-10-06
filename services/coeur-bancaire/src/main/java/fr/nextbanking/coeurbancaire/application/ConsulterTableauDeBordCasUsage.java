package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterTableauDeBord;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Cas d'usage : tableau de bord du client authentifié. Les opérations affichées sont celles du compte COURANT, choisi
 * par son type et non par son rang (BUG-005) ; le tri par date décroissante et la limite de 5 sont décidés ici
 * (FR-002-06). Le tri est stable : l'ordre du port départage les opérations de même date.
 */
public class ConsulterTableauDeBordCasUsage implements ConsulterTableauDeBord {

    static final int NOMBRE_OPERATIONS = 5;

    private final Comptes comptes;
    private final Operations operations;

    public ConsulterTableauDeBordCasUsage(Comptes comptes, Operations operations) {
        this.comptes = comptes;
        this.operations = operations;
    }

    @Override
    public TableauDeBord tableauDeBord(long clientId) {
        List<Compte> duClient = comptes.duClient(clientId);
        BigDecimal total = duClient.stream().map(Compte::solde).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        List<Operation> recentes = duClient.stream().filter(c -> c.type() == TypeCompte.COURANT).findFirst()
                .map(c -> operations.duCompte(c.id()).stream()
                        .sorted(Comparator.comparing(Operation::date).reversed())
                        .limit(NOMBRE_OPERATIONS)
                        .toList())
                .orElse(List.of());
        return new TableauDeBord(duClient, total, recentes);
    }
}
