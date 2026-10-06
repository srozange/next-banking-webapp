package fr.nextbanking.identiteclients.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Port sortant : état des clients publié par l'outbox ({@code outbox}) sur {@code identite-clients.client-modifie.v1}.
 */
public interface PublicationEtatClient {

    /**
     * Consigne l'état complet du client, relu après l'écriture, dans l'outbox, dans la transaction courante ;
     * rend l'identifiant de la ligne.
     */
    long consigner(long clientId, UUID idEvenement, Instant horodatage);

    /** Publie la ligne d'outbox après validation ; un échec la laisse au relais. */
    void publier(long idLigne);
}
