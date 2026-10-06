package fr.nextbanking.coeurbancaire.domain;

/** Statut d'une alerte de fraude ; toute alerte naît OPEN. */
public enum StatutAlerte {
    OPEN,
    CONFIRMED,
    FALSE_POSITIVE,
    REVIEWED
}
