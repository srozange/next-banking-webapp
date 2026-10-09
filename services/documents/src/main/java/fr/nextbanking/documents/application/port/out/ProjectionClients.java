package fr.nextbanking.documents.application.port.out;

import fr.nextbanking.documents.domain.ClientProjete;
import java.util.Optional;

/**
 * Port sortant : projection locale des clients, alimentée par {@code identite-clients.client-modifie.v1} ; documents
 * n'appelle jamais identite-clients.
 */
public interface ProjectionClients {

    /**
     * Enregistre le client dans la transaction courante si {@code versionAgregat} est strictement supérieure à celle de
     * la projection (ou si le client est inconnu) ; faux, sans effet, pour un message ancien.
     */
    boolean enregistrerSiPlusRecent(ClientProjete client);

    /** Nom complet du client, s'il est connu de la projection. */
    Optional<String> nomComplet(long clientId);
}
