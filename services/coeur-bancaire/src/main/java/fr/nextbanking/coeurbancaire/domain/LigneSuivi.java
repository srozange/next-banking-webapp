package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** Virement passé en revue vu par son client : état, et heure butoir tant qu'il est suspendu. */
public record LigneSuivi(long virementId, Instant soumisLe, String numeroDestinataire, BigDecimal montant,
        EtatVirement etat, Instant heureButoir) {
}
