package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Virement suspendu vu par l'administrateur. {@code nomClient} vient de la projection locale des clients ; il est
 * absent tant que le client n'a pas été reçu.
 */
public record LigneRevue(long virementId, long clientId, String nomClient, BigDecimal montant,
        String numeroDestinataire, String motifSuspension, Instant heureButoir) {
}
