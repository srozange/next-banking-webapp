package fr.nextbanking.openbankingconseiller.domain;

/**
 * État Powens d'un client (clé : {@code sub} du jeton d'accès). Le jeton permanent reste dans le service : il n'est
 * jamais rendu par l'API.
 *
 * @param jeton     jeton permanent Powens, absent tant qu'aucune connexion n'a été lancée
 * @param connecte  vrai une fois les comptes lus après le retour de Powens
 * @param message   dernier message à montrer à l'utilisateur, absent sinon
 */
public record EtatPowens(String jeton, boolean connecte, String message) {

    /** Client qui n'a jamais lancé de connexion. */
    public static EtatPowens nonConnecte() {
        return new EtatPowens(null, false, null);
    }
}
