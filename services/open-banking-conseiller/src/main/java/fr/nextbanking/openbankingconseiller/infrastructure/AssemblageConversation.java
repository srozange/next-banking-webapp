package fr.nextbanking.openbankingconseiller.infrastructure;

import fr.nextbanking.openbankingconseiller.application.DialoguerCasUsage;
import fr.nextbanking.openbankingconseiller.application.port.in.Dialoguer;
import fr.nextbanking.openbankingconseiller.application.port.out.ConversationIa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Assemble le cas d'usage de la conversation avec Sophie, écrit en Java pur. */
@Configuration
public class AssemblageConversation {

    @Bean
    Dialoguer dialoguer(ConversationIa conversation) {
        return new DialoguerCasUsage(conversation);
    }
}
