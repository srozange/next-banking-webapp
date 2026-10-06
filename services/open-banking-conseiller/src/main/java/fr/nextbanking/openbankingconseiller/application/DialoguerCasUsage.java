package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.Dialoguer;
import fr.nextbanking.openbankingconseiller.application.port.out.ConversationIa;
import fr.nextbanking.openbankingconseiller.domain.ConsigneSophie;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import java.util.List;

/** Envoie le message au service d'IA avec la consigne de Sophie et l'historique fourni ; rien n'est conservé. */
public class DialoguerCasUsage implements Dialoguer {

    private final ConversationIa conversation;

    public DialoguerCasUsage(ConversationIa conversation) {
        this.conversation = conversation;
    }

    @Override
    public String dialoguer(String message, List<Echange> historique) {
        if (message == null || message.isBlank()) {
            throw new MessageVide();
        }
        List<Echange> echanges = historique == null ? List.of() : List.copyOf(historique);
        return conversation.repondre(ConsigneSophie.TEXTE, echanges, message);
    }
}
