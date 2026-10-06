package fr.nextbanking.bourse.domain;

/**
 * Aucune valeur de la watchlist ne porte l'identifiant demandé.
 */
public class ValeurIntrouvable extends RuntimeException {

    private final long id;

    public ValeurIntrouvable(long id) {
        super("Valeur introuvable : " + id);
        this.id = id;
    }

    public long id() {
        return id;
    }
}
