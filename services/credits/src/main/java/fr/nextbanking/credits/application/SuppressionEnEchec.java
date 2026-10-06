package fr.nextbanking.credits.application;

/**
 * La suppression d'un prêt a échoué de façon inattendue ; la cause technique n'est jamais montrée à l'utilisateur.
 */
public class SuppressionEnEchec extends RuntimeException {

    public SuppressionEnEchec(Throwable cause) {
        super("Suppression du prêt en échec", cause);
    }
}
