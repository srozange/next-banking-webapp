package fr.nextbanking.credits.application;

/**
 * La décision de l'administrateur sur un prêt n'a pas pu être menée à terme : le prêt est resté EN_ATTENTE.
 */
public class DecisionAdministrateurEnEchec extends RuntimeException {

    public DecisionAdministrateurEnEchec(Throwable cause) {
        super("Décision de l'administrateur en échec", cause);
    }
}
