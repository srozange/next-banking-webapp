package fr.nextbanking.notifications.domain;

/**
 * Nombre maximal de notifications conservées par utilisateur ({@code nb.notifications.plafond}, 20 par défaut),
 * validé au démarrage : zéro ou négatif est invalide.
 */
public record PlafondNotifications(int valeur) {

    public PlafondNotifications {
        if (valeur <= 0) {
            throw new IllegalArgumentException("La propriété nb.notifications.plafond doit être strictement positive.");
        }
    }
}
