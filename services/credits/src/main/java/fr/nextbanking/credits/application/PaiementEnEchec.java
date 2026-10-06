package fr.nextbanking.credits.application;

/**
 * Le paiement d'une échéance a échoué de façon inattendue ; la cause technique n'est jamais montrée à l'utilisateur.
 */
public class PaiementEnEchec extends RuntimeException {

    public PaiementEnEchec(Throwable cause) {
        super("Paiement de l'échéance en échec", cause);
    }
}
