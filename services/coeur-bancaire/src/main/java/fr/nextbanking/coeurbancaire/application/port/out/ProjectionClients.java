package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.ClientProjete;
import java.util.List;

/**
 * Port sortant : projection locale des clients (nom complet, conseiller affecté, suppression), alimentée par
 * {@code identite-clients.client-modifie.v1} ; le cœur bancaire n'appelle jamais identite-clients.
 */
public interface ProjectionClients {

    /**
     * Enregistre le client dans la transaction courante si {@code versionAgregat} est strictement supérieure à celle de
     * la projection (ou si le client est inconnu) ; faux, sans effet, pour un message ancien. Une version supérieure
     * écrase l'affectation, y compris vers aucun conseiller.
     */
    boolean enregistrerSiPlusRecent(ClientProjete client);

    /** Identifiants des clients non supprimés affectés au conseiller, triés par ordre croissant. */
    List<Long> clientsDuConseiller(long conseillerId);
}
