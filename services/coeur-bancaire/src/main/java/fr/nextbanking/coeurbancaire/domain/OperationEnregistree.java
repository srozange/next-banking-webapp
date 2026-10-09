package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement d'une opération enregistrée au relevé d'un compte : une opération par événement, avec son identifiant
 * généré et la {@code versionAgregat} du compte relue après l'écriture. Topic
 * {@code coeur-bancaire.operation-enregistree.v1}, clé {@code compteId}.
 */
public record OperationEnregistree(UUID idEvenement, long operationId, Operation operation, long versionAgregat,
        Instant horodatage) {

    public OperationEnregistree {
        if (idEvenement == null || operation == null || horodatage == null) {
            throw new IllegalArgumentException("L'identifiant, l'opération et l'horodatage sont obligatoires.");
        }
        if (operationId < 1) {
            throw new IllegalArgumentException("L'identifiant de l'opération est un entier strictement positif.");
        }
        if (versionAgregat < 1) {
            throw new IllegalArgumentException("La version de l'agrégat est un entier strictement positif.");
        }
    }
}
