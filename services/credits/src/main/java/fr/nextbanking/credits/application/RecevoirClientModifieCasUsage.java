package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.RecevoirClientModifie;
import fr.nextbanking.credits.application.port.out.EvenementsTraites;
import fr.nextbanking.credits.application.port.out.ProjectionClients;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.ClientProjete;
import java.util.UUID;

/**
 * Cas d'usage idempotent : dans une même transaction, l'événement est marqué traité puis le client est enregistré dans
 * la projection locale, sauf si {@code versionAgregat} n'est pas strictement plus récente que celle déjà reçue.
 */
public class RecevoirClientModifieCasUsage implements RecevoirClientModifie {

    private final ProjectionClients projection;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;

    public RecevoirClientModifieCasUsage(ProjectionClients projection, EvenementsTraites evenementsTraites,
            Transaction transaction) {
        this.projection = projection;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
    }

    @Override
    public Issue recevoir(UUID idEvenement, ClientProjete client) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement)) {
                return Issue.DEJA_TRAITE;
            }
            return projection.enregistrerSiPlusRecent(client) ? Issue.ENREGISTRE : Issue.IGNORE_ANCIEN;
        });
    }
}
