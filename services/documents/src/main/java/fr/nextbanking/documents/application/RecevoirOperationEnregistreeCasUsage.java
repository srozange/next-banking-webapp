package fr.nextbanking.documents.application;

import fr.nextbanking.documents.application.port.in.RecevoirOperationEnregistree;
import fr.nextbanking.documents.application.port.out.EvenementsTraites;
import fr.nextbanking.documents.application.port.out.ProjectionOperations;
import fr.nextbanking.documents.application.port.out.Transaction;
import fr.nextbanking.documents.domain.OperationProjetee;
import java.util.UUID;

/**
 * Cas d'usage idempotent : dans une même transaction, l'événement est marqué traité puis l'opération est enregistrée
 * dans la projection locale, sauf si son identifiant y est déjà.
 */
public class RecevoirOperationEnregistreeCasUsage implements RecevoirOperationEnregistree {

    private final ProjectionOperations projection;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;

    public RecevoirOperationEnregistreeCasUsage(ProjectionOperations projection, EvenementsTraites evenementsTraites,
            Transaction transaction) {
        this.projection = projection;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
    }

    @Override
    public Issue recevoir(UUID idEvenement, OperationProjetee operation) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement)) {
                return Issue.DEJA_TRAITE;
            }
            return projection.enregistrerSiAbsente(operation) ? Issue.ENREGISTRE : Issue.DEJA_PRESENTE;
        });
    }
}
