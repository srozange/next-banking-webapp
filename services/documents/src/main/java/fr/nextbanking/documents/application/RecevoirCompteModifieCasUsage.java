package fr.nextbanking.documents.application;

import fr.nextbanking.documents.application.port.in.RecevoirCompteModifie;
import fr.nextbanking.documents.application.port.out.EvenementsTraites;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.application.port.out.Transaction;
import fr.nextbanking.documents.domain.CompteProjete;
import java.util.UUID;

/**
 * Cas d'usage idempotent : dans une même transaction, l'événement est marqué traité puis le compte est enregistré dans
 * la projection locale, sauf si {@code versionAgregat} n'est pas strictement plus récente que celle déjà reçue.
 */
public class RecevoirCompteModifieCasUsage implements RecevoirCompteModifie {

    private final ProjectionComptes projection;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;

    public RecevoirCompteModifieCasUsage(ProjectionComptes projection, EvenementsTraites evenementsTraites,
            Transaction transaction) {
        this.projection = projection;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
    }

    @Override
    public Issue recevoir(UUID idEvenement, CompteProjete compte) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement)) {
                return Issue.DEJA_TRAITE;
            }
            return projection.enregistrerSiPlusRecent(compte) ? Issue.ENREGISTRE : Issue.IGNORE_ANCIEN;
        });
    }
}
