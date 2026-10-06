package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.ConnexionRefusee;
import fr.nextbanking.identiteclients.domain.Identifiants;
import java.util.Optional;

/**
 * Cas d'usage : valide les champs requis, cherche le client, compare toujours le mot de passe (même pour un
 * identifiant inconnu), puis refuse de façon indistincte un client inconnu, inactif ou un mauvais mot de passe.
 */
public class SeConnecterCasUsage implements SeConnecter {

    private final DepotClients depot;
    private final MotsDePasse motsDePasse;
    private final EmetteurJeton emetteur;

    public SeConnecterCasUsage(DepotClients depot, MotsDePasse motsDePasse, EmetteurJeton emetteur) {
        this.depot = depot;
        this.motsDePasse = motsDePasse;
        this.emetteur = emetteur;
    }

    @Override
    public Connexion seConnecter(String identifiant, String motDePasse) {
        Identifiants identifiants = new Identifiants(identifiant, motDePasse);
        Optional<Client> trouve = depot.parIdentifiant(identifiants.identifiant());
        boolean correspond = motsDePasse.correspond(identifiants.motDePasse(),
                trouve.map(Client::empreinteMotDePasse));
        Client client = trouve.filter(c -> correspond && c.actif())
                .orElseThrow(() -> new ConnexionRefusee(ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS));
        return new Connexion(client.id(), emetteur.emettre(client));
    }
}
