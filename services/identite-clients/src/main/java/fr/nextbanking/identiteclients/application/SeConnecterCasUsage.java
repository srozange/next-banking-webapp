package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.ConnexionRefusee;
import fr.nextbanking.identiteclients.domain.EntreeAudit;
import fr.nextbanking.identiteclients.domain.Identifiants;
import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage : valide les champs requis (refus sans audit), puis, dans une transaction, cherche le client, compare
 * toujours le mot de passe (même pour un identifiant inconnu) et consigne l'entrée d'audit LOGIN ou LOGIN_FAILED
 * dans une transaction indépendante, conservée même si la connexion est annulée. L'entrée est publiée après
 * validation ; un client inconnu, inactif ou un mauvais mot de passe sont ensuite refusés de façon indistincte.
 */
public class SeConnecterCasUsage implements SeConnecter {

    private final DepotClients depot;
    private final MotsDePasse motsDePasse;
    private final EmetteurJeton emetteur;
    private final JournalAudit journal;
    private final Transaction transaction;
    private final Horloge horloge;

    public SeConnecterCasUsage(DepotClients depot, MotsDePasse motsDePasse, EmetteurJeton emetteur,
            JournalAudit journal, Transaction transaction, Horloge horloge) {
        this.depot = depot;
        this.motsDePasse = motsDePasse;
        this.emetteur = emetteur;
        this.journal = journal;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public Connexion seConnecter(String identifiant, String motDePasse, String adresseIp) {
        Identifiants identifiants = new Identifiants(identifiant, motDePasse);
        Verification verification = transaction.executer(() -> {
            Optional<Client> trouve = depot.parIdentifiant(identifiants.identifiant());
            boolean correspond = motsDePasse.correspond(identifiants.motDePasse(),
                    trouve.map(Client::empreinteMotDePasse));
            Optional<Client> accepte = trouve.filter(c -> correspond && c.actif());
            EntreeAudit entree = accepte
                    .map(c -> EntreeAudit.connexionReussie(c.id(), adresseIp, UUID.randomUUID(), horloge.instant()))
                    .orElseGet(() -> EntreeAudit.tentativeEchouee(identifiants.identifiant(), adresseIp,
                            UUID.randomUUID(), horloge.instant()));
            long idLigne = transaction.executerIndependamment(() -> journal.consigner(entree));
            return new Verification(accepte, idLigne);
        });
        journal.publier(verification.idLigne());
        Client client = verification.client()
                .orElseThrow(() -> new ConnexionRefusee(ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS));
        return new Connexion(client.id(), emetteur.emettre(client));
    }

    private record Verification(Optional<Client> client, long idLigne) {
    }
}
