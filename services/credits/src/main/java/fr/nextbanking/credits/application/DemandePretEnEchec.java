package fr.nextbanking.credits.application;

/**
 * L'enregistrement d'une demande valide a échoué (hors outbox) ; la cause technique n'est jamais montrée à
 * l'utilisateur.
 */
public class DemandePretEnEchec extends RuntimeException {

    public DemandePretEnEchec(Throwable cause) {
        super("Enregistrement de la demande de prêt en échec", cause);
    }
}
