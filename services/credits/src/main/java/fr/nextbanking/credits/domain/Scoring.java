package fr.nextbanking.credits.domain;

/**
 * Scoring crédit automatique (FR-024-03 à FR-024-05), dans l'ordre : durée supérieure à 300 mois, type absent,
 * plafond du type dépassé ; sinon approbation.
 */
public final class Scoring {

    static final int DUREE_MAXIMUM = 300;

    private Scoring() {
    }

    /** Décide du sort du prêt. */
    public static DecisionScoring decider(Pret pret) {
        if (pret.dureeMois() > DUREE_MAXIMUM) {
            return DecisionScoring.REFUSE;
        }
        if (pret.type() == null) {
            return DecisionScoring.REFUSE;
        }
        if (pret.montant().compareTo(pret.type().plafond()) > 0) {
            return DecisionScoring.REFUSE;
        }
        return DecisionScoring.APPROUVE;
    }
}
