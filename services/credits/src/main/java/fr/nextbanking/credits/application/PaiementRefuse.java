package fr.nextbanking.credits.application;

/**
 * L'échéance à payer appartient à un prêt d'un autre client ; rien n'est modifié.
 */
public class PaiementRefuse extends RuntimeException {

    public PaiementRefuse(long echeanceId) {
        super("Paiement refusé pour l'échéance " + echeanceId);
    }
}
