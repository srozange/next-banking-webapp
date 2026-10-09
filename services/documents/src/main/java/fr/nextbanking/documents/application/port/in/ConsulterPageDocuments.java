package fr.nextbanking.documents.application.port.in;

import fr.nextbanking.documents.domain.CompteProjete;
import java.time.LocalDate;
import java.util.List;

/** Port entrant : données de la page Documents du client connecté. */
public interface ConsulterPageDocuments {

    /** Date du jour (fuseau métier) et comptes du client, triés par identifiant. */
    record Page(LocalDate aujourdhui, List<CompteProjete> comptes) {
    }

    /** Les comptes du seul client désigné (sujet du jeton) dans la projection locale, et la date du jour de l'Horloge. */
    Page consulter(long clientId);
}
