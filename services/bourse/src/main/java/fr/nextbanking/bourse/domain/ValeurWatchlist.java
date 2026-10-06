package fr.nextbanking.bourse.domain;

/**
 * Valeur enregistrée dans la watchlist commune (FR-009-10).
 */
public record ValeurWatchlist(long id, String symbole, String nom, String marche, String devise) {
}
