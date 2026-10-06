package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.Echeance;

/**
 * Port entrant : marquer une échéance payée (FR-007-12), sans mouvement d'argent ni événement.
 */
public interface PayerEcheance {

    /**
     * Rend l'échéance à jour ; une échéance déjà payée reste inchangée, sans erreur.
     *
     * @throws fr.nextbanking.credits.application.EcheanceIntrouvable si l'échéance n'existe pas
     * @throws fr.nextbanking.credits.application.PaiementRefuse si le prêt appartient à un autre client
     * @throws fr.nextbanking.credits.application.PaiementEnEchec sur toute autre panne
     */
    Echeance payer(long clientId, long echeanceId);
}
