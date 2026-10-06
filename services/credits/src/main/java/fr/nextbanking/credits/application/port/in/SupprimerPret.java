package fr.nextbanking.credits.application.port.in;

/**
 * Port entrant : supprimer définitivement un prêt et ses échéances (FR-007-13), sans confirmation ni événement.
 */
public interface SupprimerPret {

    /**
     * @throws fr.nextbanking.credits.application.PretIntrouvable si le prêt n'existe pas
     * @throws fr.nextbanking.credits.application.SuppressionRefusee si le prêt appartient à un autre client
     * @throws fr.nextbanking.credits.application.SuppressionEnEchec sur toute autre panne
     */
    void supprimer(long clientId, long pretId);
}
