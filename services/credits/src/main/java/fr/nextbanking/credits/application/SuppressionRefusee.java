package fr.nextbanking.credits.application;

/**
 * Le prêt à supprimer appartient à un autre client ; rien n'est modifié.
 */
public class SuppressionRefusee extends RuntimeException {

    public SuppressionRefusee(long pretId) {
        super("Suppression refusée pour le prêt " + pretId);
    }
}
