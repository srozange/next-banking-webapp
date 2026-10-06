package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import java.util.UUID;

/**
 * Cas d'usage idempotent : dans une même transaction, l'événement est marqué traité puis le nom complet du client est
 * enregistré dans la projection locale, sauf si {@code versionAgregat} n'est pas plus récente que celle déjà reçue.
 */
public class RecevoirClientModifieCasUsage implements RecevoirClientModifie {

    private final ProjectionClients projection;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;
    private final Horloge horloge;

    public RecevoirClientModifieCasUsage(ProjectionClients projection, EvenementsTraites evenementsTraites,
            Transaction transaction, Horloge horloge) {
        this.projection = projection;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public Issue recevoir(UUID idEvenement, long clientId, String nomComplet, long versionAgregat) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement, horloge.instant())) {
                return Issue.DEJA_TRAITE;
            }
            return projection.enregistrerSiPlusRecent(clientId, nomComplet, versionAgregat)
                    ? Issue.ENREGISTRE : Issue.IGNORE_ANCIEN;
        });
    }
}
