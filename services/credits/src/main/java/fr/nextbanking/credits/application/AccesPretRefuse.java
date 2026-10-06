package fr.nextbanking.credits.application;

/**
 * Le prêt demandé appartient à un autre client.
 */
public class AccesPretRefuse extends RuntimeException {

    public AccesPretRefuse(long pretId) {
        super("Accès refusé au prêt " + pretId);
    }
}
