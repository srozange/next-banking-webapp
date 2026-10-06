package fr.nextbanking.identiteclients.application.port.in;

/**
 * Port entrant : se connecter avec un identifiant et un mot de passe, depuis une adresse IP d'origine.
 */
public interface SeConnecter {

    /**
     * Vérifie les identifiants, consigne l'entrée d'audit de la tentative (avec l'adresse IP d'origine, déjà
     * résolue par l'adaptateur entrant) et rend le jeton émis.
     *
     * @throws fr.nextbanking.identiteclients.domain.ConnexionRefusee champ requis manquant ou identifiants incorrects
     */
    Connexion seConnecter(String identifiant, String motDePasse, String adresseIp);

    /** Connexion réussie : client connecté et jeton émis. Le jeton n'apparaît jamais dans {@link #toString()}. */
    record Connexion(long clientId, String jeton) {

        @Override
        public String toString() {
            return "Connexion[clientId=" + clientId + ", jeton=****]";
        }
    }
}
