package fr.nextbanking.coeurbancaire.domain;

/**
 * État d'un compte relu après une écriture : le compte et la {@code versionAgregat} qui l'accompagne, incrémentée à
 * chaque débit ou crédit.
 */
public record EtatCompte(Compte compte, long versionAgregat) {

    public EtatCompte {
        if (compte == null) {
            throw new IllegalArgumentException("Le compte est obligatoire.");
        }
        if (versionAgregat < 1) {
            throw new IllegalArgumentException("La version de l'agrégat est un entier strictement positif.");
        }
    }
}
