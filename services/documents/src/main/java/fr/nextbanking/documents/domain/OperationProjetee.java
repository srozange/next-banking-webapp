package fr.nextbanking.documents.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Opération telle que reçue sur {@code coeur-bancaire.operation-enregistree.v1} : projection locale, jamais un appel à
 * coeur-bancaire. Une opération est immuable : son identifiant est la clé d'idempotence. Le montant et le solde après
 * sont à 2 décimales.
 */
public record OperationProjetee(long operationId, long compteId, Instant instant, String libelle, TypeOperation type,
        BigDecimal montant, BigDecimal soldeApres, long versionAgregat) {

    public OperationProjetee {
        if (instant == null || libelle == null || type == null || montant == null || soldeApres == null) {
            throw new IllegalArgumentException(
                    "L'instant, le libellé, le type, le montant et le solde après de l'opération sont obligatoires.");
        }
    }
}
