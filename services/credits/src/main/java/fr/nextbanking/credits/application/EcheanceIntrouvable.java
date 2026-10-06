package fr.nextbanking.credits.application;

/**
 * Aucune échéance ne porte l'identifiant demandé.
 */
public class EcheanceIntrouvable extends RuntimeException {

    public EcheanceIntrouvable(long echeanceId) {
        super("Échéance introuvable : " + echeanceId);
    }
}
