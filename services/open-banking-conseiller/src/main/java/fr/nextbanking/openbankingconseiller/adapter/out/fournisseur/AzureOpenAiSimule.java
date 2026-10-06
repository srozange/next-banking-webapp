package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
import fr.nextbanking.openbankingconseiller.application.port.out.ConversationIa;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import java.util.List;

/**
 * Adaptateur simulé du service d'IA, actif par défaut : réponse fixe, aucun appel réseau. Un refus d'authentification
 * (401 ou 403) peut être simulé par {@code nb.fournisseurs.azure-openai.refus-simule}.
 */
public class AzureOpenAiSimule implements ConversationIa {

    static final String REPONSE = "Réponse simulée de Sophie.";

    private final Integer refus;

    public AzureOpenAiSimule(Integer refus) {
        this.refus = refus;
    }

    @Override
    public String repondre(String consigne, List<Echange> historique, String message) {
        if (refus != null) {
            throw ServiceIaIndisponible.authentification(refus);
        }
        return REPONSE;
    }
}
