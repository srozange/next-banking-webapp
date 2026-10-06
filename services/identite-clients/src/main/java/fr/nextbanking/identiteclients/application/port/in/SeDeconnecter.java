package fr.nextbanking.identiteclients.application.port.in;

/**
 * Port entrant : se déconnecter. Aucune liste de révocation : le jeton reste valide jusqu'à son expiration.
 */
public interface SeDeconnecter {

    /** Consigne l'entrée d'audit LOGOUT du client (adresse IP déjà résolue par l'adaptateur entrant). */
    void seDeconnecter(long clientId, String adresseIp);
}
