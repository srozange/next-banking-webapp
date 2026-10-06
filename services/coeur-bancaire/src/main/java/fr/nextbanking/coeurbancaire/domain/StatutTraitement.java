package fr.nextbanking.coeurbancaire.domain;

/**
 * Issue d'un traitement nocturne, valeurs du legacy (contrainte {@code CHECK} en base).
 */
public enum StatutTraitement {
    SUCCESS,
    FAILED,
    PARTIAL
}
