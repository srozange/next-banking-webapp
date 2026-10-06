package fr.nextbanking.coeurbancaire.domain;

/**
 * Le nombre d'exécutions demandé n'est pas strictement positif.
 */
public class LimiteInvalide extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LimiteInvalide(long limite) {
        super("La limite doit être strictement positive : " + limite);
    }
}
