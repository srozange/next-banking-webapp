package fr.nextbanking.coeurbancaire.application.port.in;

/**
 * Port entrant : écrire un témoin.
 */
public interface EcrireTemoin {

    /** Écrit le témoin et rend son identifiant. */
    long ecrire(String libelle);
}
