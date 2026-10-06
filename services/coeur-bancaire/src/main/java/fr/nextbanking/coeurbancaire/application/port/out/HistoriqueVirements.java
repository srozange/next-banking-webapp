package fr.nextbanking.coeurbancaire.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Port sortant unique des signaux d'historique du contrôle de fraude. Le verrou consultatif sérialise le contrôle et
 * l'enregistrement d'un même client.
 */
public interface HistoriqueVirements {

    /** Prend le verrou consultatif du client, tenu jusqu'à la fin de la transaction courante. */
    void verrouillerClient(long clientId);

    /** Opérations au relevé du client (débits et crédits de tous ses comptes) depuis cet instant. */
    long operationsDepuis(long clientId, Instant debut);

    /** Instant de soumission du précédent virement enregistré du client. */
    Optional<Instant> dernierVirement(long clientId);

    /** Virements exécutés du client, de montant strictement supérieur au minimum, soumis depuis cet instant. */
    long virementsExecutesAuDessus(long clientId, BigDecimal montantMin, Instant depuis);
}
