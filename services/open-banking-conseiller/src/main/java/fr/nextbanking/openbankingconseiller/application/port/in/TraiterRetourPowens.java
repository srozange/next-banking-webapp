package fr.nextbanking.openbankingconseiller.application.port.in;

import java.util.Map;

/** Cas d'usage : traiter le retour de la page web de Powens, sans jeton d'accès (FEAT-014). */
public interface TraiterRetourPowens {

    /**
     * Traite les paramètres reçus (dans l'ordre reçu, {@code state} compris). Seul un {@code state} valide a un effet ;
     * sinon rien ne change et Powens n'est pas appelé. Ne lève jamais d'erreur de panne Powens : elle devient un message.
     */
    void traiter(Map<String, String> parametres);
}
