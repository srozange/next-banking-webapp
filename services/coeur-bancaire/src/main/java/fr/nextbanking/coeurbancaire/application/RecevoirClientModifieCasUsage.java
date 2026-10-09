package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.ClientProjete;
import java.util.UUID;

/**
 * Cas d'usage idempotent : dans une même transaction, l'événement est marqué traité puis l'état du client (nom complet,
 * conseiller affecté, suppression) est enregistré dans la projection locale, sauf si {@code versionAgregat} n'est pas plus récente que celle déjà reçue.
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
    public Issue recevoir(UUID idEvenement, ClientProjete client) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement, horloge.instant())) {
                return Issue.DEJA_TRAITE;
            }
            return projection.enregistrerSiPlusRecent(client)
                    ? Issue.ENREGISTRE : Issue.IGNORE_ANCIEN;
        });
    }
}
