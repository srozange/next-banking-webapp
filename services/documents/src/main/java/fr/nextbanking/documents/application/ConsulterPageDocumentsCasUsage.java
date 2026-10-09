package fr.nextbanking.documents.application;

import fr.nextbanking.documents.application.port.in.ConsulterPageDocuments;
import fr.nextbanking.documents.application.port.out.Horloge;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;

/** Cas d'usage : comptes du client du jeton (projection locale) et {@code aujourdhui} de l'Horloge. */
public class ConsulterPageDocumentsCasUsage implements ConsulterPageDocuments {

    private final ProjectionComptes comptes;
    private final Horloge horloge;

    public ConsulterPageDocumentsCasUsage(ProjectionComptes comptes, Horloge horloge) {
        this.comptes = comptes;
        this.horloge = horloge;
    }

    @Override
    public Page consulter(long clientId) {
        return new Page(horloge.aujourdhui(), comptes.parClient(clientId));
    }
}
