package fr.nextbanking.credits.application;

/**
 * Le calcul d'une simulation valide a échoué ; la cause technique n'est jamais montrée à l'utilisateur.
 */
public class SimulationEnEchec extends RuntimeException {

    public SimulationEnEchec(Throwable cause) {
        super("Calcul de simulation en échec", cause);
    }
}
