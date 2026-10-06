package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.ConnecterPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.application.port.out.Horloge;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import java.time.Duration;
import java.util.UUID;

/**
 * Lancement de la connexion (FR-028-01, FR-028-02, FR-012-05) : utilisateur anonyme, jeton permanent enregistré AVANT
 * de construire l'adresse, code temporaire, {@code state} signé à usage unique lié au client.
 */
public class ConnecterPowensCasUsage implements ConnecterPowens {

    static final Duration VALIDITE_STATE = Duration.ofMinutes(15);

    private final Powens powens;
    private final DepotEtatPowens depot;
    private final EmetteurState emetteur;
    private final Horloge horloge;

    public ConnecterPowensCasUsage(Powens powens, DepotEtatPowens depot, EmetteurState emetteur, Horloge horloge) {
        this.powens = powens;
        this.depot = depot;
        this.emetteur = emetteur;
        this.horloge = horloge;
    }

    @Override
    public String connecter(String sub) {
        try {
            UtilisateurPowens utilisateur = powens.creerUtilisateur();
            if (utilisateur.jeton() == null || utilisateur.jeton().isBlank()) {
                throw new ConnexionPowensImpossible("réponse sans jeton");
            }
            depot.enregistrerJeton(sub, utilisateur.jeton());
            String code = powens.codeTemporaire(utilisateur.jeton());
            if (code == null || code.isBlank()) {
                throw new ConnexionPowensImpossible("réponse sans code temporaire");
            }
            StatePowens state = new StatePowens(sub, UUID.randomUUID().toString().replace("-", ""),
                    horloge.instant().plus(VALIDITE_STATE));
            depot.enregistrerState(state);
            return powens.adresseConnexion(code, emetteur.emettre(state));
        } catch (PowensIndisponible indisponible) {
            throw new ConnexionPowensImpossible("Powens indisponible");
        }
    }
}
