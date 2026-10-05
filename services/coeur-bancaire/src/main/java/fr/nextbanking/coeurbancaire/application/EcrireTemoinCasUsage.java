package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.EcrireTemoin;
import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;
import java.util.UUID;

/**
 * Cas d'usage : valide le témoin, l'enregistre avec sa ligne d'outbox dans une même transaction,
 * puis publie l'événement après validation.
 */
public class EcrireTemoinCasUsage implements EcrireTemoin {

    private final DepotTemoins depot;
    private final EvenementsSortants evenements;
    private final Transaction transaction;
    private final Horloge horloge;

    public EcrireTemoinCasUsage(DepotTemoins depot, EvenementsSortants evenements, Transaction transaction,
            Horloge horloge) {
        this.depot = depot;
        this.evenements = evenements;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public long ecrire(String libelle) {
        Temoin temoin = new Temoin(libelle);
        Ecriture ecriture = transaction.executer(() -> {
            long temoinId = depot.enregistrer(temoin);
            long idLigne = evenements.consigner(new TemoinEcrit(UUID.randomUUID(), temoinId, horloge.instant()));
            return new Ecriture(temoinId, idLigne);
        });
        evenements.publier(ecriture.idLigne());
        return ecriture.temoinId();
    }

    private record Ecriture(long temoinId, long idLigne) {
    }
}
