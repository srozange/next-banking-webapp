package fr.nextbanking.coeurbancaire.application.port.out;

/**
 * Port sortant : projection locale des clients (nom complet), alimentée par
 * {@code identite-clients.client-modifie.v1} ; le cœur bancaire n'appelle jamais identite-clients.
 */
public interface ProjectionClients {

    /**
     * Enregistre le client dans la transaction courante si {@code versionAgregat} est strictement supérieure à celle de
     * la projection (ou si le client est inconnu) ; faux, sans effet, pour un message ancien.
     */
    boolean enregistrerSiPlusRecent(long clientId, String nomComplet, long versionAgregat);
}
