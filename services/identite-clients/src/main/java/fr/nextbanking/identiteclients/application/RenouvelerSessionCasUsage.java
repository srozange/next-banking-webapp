package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.RenouvelerSession;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.domain.Client;
import java.util.Optional;

/**
 * Cas d'usage : émet un jeton neuf pour un client connu et actif ; un client inconnu ou désactivé n'en reçoit pas.
 */
public class RenouvelerSessionCasUsage implements RenouvelerSession {

    private final DepotClients depot;
    private final EmetteurJeton emetteur;

    public RenouvelerSessionCasUsage(DepotClients depot, EmetteurJeton emetteur) {
        this.depot = depot;
        this.emetteur = emetteur;
    }

    @Override
    public Optional<String> renouveler(long clientId) {
        return depot.parId(clientId).filter(Client::actif).map(emetteur::emettre);
    }
}
