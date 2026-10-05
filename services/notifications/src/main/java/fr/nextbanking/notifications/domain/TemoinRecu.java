package fr.nextbanking.notifications.domain;

/**
 * Témoin reçu du cœur bancaire, désigné par son identifiant numérique strictement positif.
 */
public record TemoinRecu(long temoinId) {

    public TemoinRecu {
        if (temoinId <= 0) {
            throw new IllegalArgumentException("L'identifiant du témoin doit être strictement positif.");
        }
    }
}
