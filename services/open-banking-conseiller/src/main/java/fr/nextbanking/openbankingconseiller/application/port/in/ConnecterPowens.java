package fr.nextbanking.openbankingconseiller.application.port.in;

import fr.nextbanking.openbankingconseiller.application.ConnexionPowensImpossible;

/** Cas d'usage : lancer la connexion d'un client à une banque par Powens. */
public interface ConnecterPowens {

    /**
     * Crée l'utilisateur Powens, enregistre son jeton, puis rend l'adresse de la page web.
     *
     * @throws ConnexionPowensImpossible si Powens refuse la création, ne rend pas de jeton ou de code
     */
    String connecter(String sub);
}
