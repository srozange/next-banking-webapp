package fr.nextbanking.openbankingconseiller.application;

/** Le message à envoyer à Sophie est absent ou blanc. */
public class MessageVide extends RuntimeException {

    public MessageVide() {
        super("message vide", null, false, false);
    }
}
