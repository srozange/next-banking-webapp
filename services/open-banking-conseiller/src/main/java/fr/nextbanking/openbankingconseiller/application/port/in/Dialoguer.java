package fr.nextbanking.openbankingconseiller.application.port.in;

import fr.nextbanking.openbankingconseiller.application.MessageVide;
import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import java.util.List;

/** Port entrant : poser une question à Sophie, sans état (l'appelant fournit l'historique). */
public interface Dialoguer {

    /**
     * @throws MessageVide             si le message est absent ou blanc
     * @throws ServiceIaIndisponible   si le service d'IA ne répond pas
     */
    String dialoguer(String message, List<Echange> historique);
}
