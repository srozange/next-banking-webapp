package fr.nextbanking.openbankingconseiller.application;

/** Le client n'a pas de jeton Powens : aucune lecture possible, Powens n'est pas appelé. */
public class PowensNonConnecte extends RuntimeException {

    public PowensNonConnecte() {
        super("aucun jeton Powens", null, false, false);
    }
}
