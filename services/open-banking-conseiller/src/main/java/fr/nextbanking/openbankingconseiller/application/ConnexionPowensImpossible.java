package fr.nextbanking.openbankingconseiller.application;

/** La connexion à une banque n'a pas pu être initialisée : aucune adresse n'est rendue. */
public class ConnexionPowensImpossible extends RuntimeException {

    public ConnexionPowensImpossible(String motif) {
        super(motif, null, false, false);
    }
}
