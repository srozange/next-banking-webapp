package fr.nextbanking.credits.application;

/**
 * La ligne d'outbox de la demande n'a pas pu être écrite : la transaction est annulée, aucun prêt EN_ATTENTE ne reste.
 */
public class TransmissionEnEchec extends RuntimeException {

    public TransmissionEnEchec(Throwable cause) {
        super("Transmission de la demande de prêt en échec", cause);
    }
}
