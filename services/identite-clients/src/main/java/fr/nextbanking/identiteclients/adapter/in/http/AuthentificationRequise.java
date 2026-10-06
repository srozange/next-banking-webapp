package fr.nextbanking.identiteclients.adapter.in.http;

/**
 * Jeton valide qui ne désigne aucun client : traité comme une absence d'authentification (401).
 */
class AuthentificationRequise extends RuntimeException {

    AuthentificationRequise() {
        super("Jeton sans client correspondant");
    }
}
