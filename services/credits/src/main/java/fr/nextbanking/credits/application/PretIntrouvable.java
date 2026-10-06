package fr.nextbanking.credits.application;

/**
 * Aucun prêt ne porte l'identifiant demandé.
 */
public class PretIntrouvable extends RuntimeException {

    public PretIntrouvable(long pretId) {
        super("Prêt introuvable : " + pretId);
    }
}
