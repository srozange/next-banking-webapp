package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterTableauDeBord;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import java.util.Comparator;
import java.util.List;

/**
 * Cas d'usage : tableau de bord du client authentifié. Les opérations affichées sont celles du compte COURANT, choisi
 * par son type et non par son rang (BUG-005) ; le tri par date décroissante et la limite de 5 sont décidés ici
 * (FR-002-06). Le tri est stable : l'ordre du port départage les opérations de même date. La date du jour vient du
 * port Horloge.
 */
public class ConsulterTableauDeBordCasUsage implements ConsulterTableauDeBord {

    static final int NOMBRE_OPERATIONS = 5;

    private final Comptes comptes;
    private final Operations operations;
    private final Horloge horloge;

    public ConsulterTableauDeBordCasUsage(Comptes comptes, Operations operations, Horloge horloge) {
        this.comptes = comptes;
        this.operations = operations;
        this.horloge = horloge;
    }

    @Override
    public TableauDeBord tableauDeBord(long clientId) {
        List<Compte> duClient = comptes.duClient(clientId);
        Montant total = duClient.stream().map(Compte::solde).reduce(Montant.ZERO, Montant::ajouter);
        List<Operation> recentes = duClient.stream().filter(c -> c.type() == TypeCompte.COURANT).findFirst()
                .map(c -> operations.duCompte(c.id()).stream()
                        .sorted(Comparator.comparing(Operation::date).reversed())
                        .limit(NOMBRE_OPERATIONS)
                        .toList())
                .orElse(List.of());
        return new TableauDeBord(duClient, total, recentes, horloge.aujourdhui());
    }
}
