package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Virement;
import java.util.Optional;

/**
 * Port sortant : virements enregistrés, avec leur clé d'idempotence.
 */
public interface DepotVirements {

    /** Réserve l'identifiant du prochain virement ; il n'est jamais repris, même si la transaction est annulée. */
    long attribuerIdentifiant();

    /**
     * Enregistre le virement avec sa clé d'idempotence (absente : aucune).
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement de cause {@code CLE_IDEMPOTENCE_DEJA_UTILISEE} si le
     *         client a déjà un virement sous cette clé
     */
    void enregistrer(Virement virement, String cleIdempotence);

    /** Virement du client enregistré sous cette clé d'idempotence. */
    Optional<Virement> parCle(long clientId, String cleIdempotence);

    /** Virement d'identifiant donné. */
    Optional<Virement> parId(long virementId);
}
