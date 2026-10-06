package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Événement interne : un virement exécuté est remis au traitement asynchrone, avec identifiant, montant, émetteur,
 * compte destinataire et motif (FR-023-01). Topic {@code coeur-bancaire.virement-a-traiter.v1}.
 */
public record VirementATraiter(UUID idEvenement, long virementId, BigDecimal montant, long emetteurId,
        long compteDestinataireId, String motif, Instant horodatage) {

    public VirementATraiter {
        if (idEvenement == null || montant == null || horodatage == null) {
            throw new IllegalArgumentException("L'identifiant, le montant et l'horodatage sont obligatoires.");
        }
    }
}
