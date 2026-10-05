package fr.nextbanking.coeurbancaire.domain;

/**
 * Témoin écrit par le cœur bancaire : objet-valeur dont le libellé est obligatoire.
 */
public record Temoin(String libelle) {

    public Temoin {
        if (libelle == null || libelle.isBlank()) {
            throw new IllegalArgumentException("Le libellé du témoin est obligatoire.");
        }
    }
}
