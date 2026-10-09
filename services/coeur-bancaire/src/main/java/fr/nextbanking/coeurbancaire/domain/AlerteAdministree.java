package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;

/**
 * Alerte de fraude vue par l'administrateur, quel que soit son statut (BUG-056 corrigé). {@code regle} est le code
 * métier tel qu'enregistré (les signaux de virement et {@code MULTIPLE_FAILED_AUTH}). {@code nomClient} et
 * {@code nomRelecteur} viennent de la projection locale des clients, nuls si absents ; {@code revuPar} est
 * l'identifiant brut de l'auteur de la revue, nul tant que l'alerte n'a pas été revue.
 */
public record AlerteAdministree(long alerteId, long clientId, String nomClient, String regle, int score,
        StatutAlerte statut, String description, Instant creeLe, String revuPar, String nomRelecteur) {
}
