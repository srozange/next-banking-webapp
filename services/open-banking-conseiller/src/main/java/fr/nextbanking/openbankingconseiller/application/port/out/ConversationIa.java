package fr.nextbanking.openbankingconseiller.application.port.out;

import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import java.util.List;

/** Port sortant : le service d'IA qui répond à la conversation avec Sophie. */
public interface ConversationIa {

    /**
     * @param consigne   consigne système
     * @param historique échanges précédents, du plus ancien au plus récent (vide au premier message)
     * @param message    message de l'utilisateur
     * @return la réponse de Sophie
     * @throws ServiceIaIndisponible si le service ne peut pas répondre
     */
    String repondre(String consigne, List<Echange> historique, String message);
}
