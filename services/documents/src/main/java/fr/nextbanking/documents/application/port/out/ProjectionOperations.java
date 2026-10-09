package fr.nextbanking.documents.application.port.out;

import fr.nextbanking.documents.domain.OperationProjetee;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Port sortant : projection locale des opérations, alimentée par {@code coeur-bancaire.operation-enregistree.v1} ;
 * documents n'appelle jamais coeur-bancaire.
 */
public interface ProjectionOperations {

    /**
     * Enregistre l'opération dans la transaction courante ; faux, sans effet, si son identifiant est déjà connu.
     */
    boolean enregistrerSiAbsente(OperationProjetee operation);

    /** Opérations du compte dont l'instant est dans {@code [debut, fin[}, par instant croissant puis identifiant. */
    List<OperationProjetee> duCompte(long compteId, Instant debut, Instant fin);

    /**
     * Total des seuls crédits dont le libellé contient « interet » (comparaison du legacy, {@code LIKE '%interet%'}),
     * sur tous les comptes du client, dont l'instant est dans {@code [debut, fin[} ; zéro sans opération.
     */
    BigDecimal totalInteretsCredites(long clientId, Instant debut, Instant fin);
}
