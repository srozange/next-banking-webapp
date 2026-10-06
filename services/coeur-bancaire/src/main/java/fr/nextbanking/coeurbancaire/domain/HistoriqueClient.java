package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;

/**
 * Valeurs d'historique lues avant l'écriture du virement en cours : opérations au relevé du jour courant (débits et
 * crédits), instant du précédent virement enregistré du client (absent : aucun), virements exécutés de plus de
 * {@code velocite-montant} dans la fenêtre de vélocité.
 */
public record HistoriqueClient(long operationsDuJour, Instant dernierVirement, long virementsImportants) {
}
