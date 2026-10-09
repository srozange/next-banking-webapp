package fr.nextbanking.credits.application.port.out;

import fr.nextbanking.credits.domain.ClientProjete;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Port sortant : projection locale des clients, alimentée par {@code identite-clients.client-modifie.v1} ; credits
 * n'appelle jamais identite-clients.
 */
public interface ProjectionClients {

    /**
     * Enregistre le client dans la transaction courante si {@code versionAgregat} est strictement supérieure à celle de
     * la projection (ou si le client est inconnu) ; faux, sans effet, pour un message ancien.
     */
    boolean enregistrerSiPlusRecent(ClientProjete client);

    /** Nom complet des clients connus parmi ceux demandés, par identifiant. */
    Map<Long, String> nomsComplets(Collection<Long> clientIds);

    /** Identifiants des clients non supprimés affectés au conseiller, triés par ordre croissant. */
    List<Long> clientsDuConseiller(long conseillerId);
}
