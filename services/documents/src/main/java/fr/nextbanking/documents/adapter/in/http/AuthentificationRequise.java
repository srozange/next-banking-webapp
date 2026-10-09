package fr.nextbanking.documents.adapter.in.http;

/**
 * Jeton valide dont le sujet n'est pas un identifiant de client : traité comme une absence d'authentification (401).
 */
class AuthentificationRequise extends RuntimeException {

    AuthentificationRequise() {
        super("Jeton sans identifiant de client");
    }
}
