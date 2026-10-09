package fr.nextbanking.documents.application.port.out;

import fr.nextbanking.documents.domain.CompteProjete;
import java.util.Optional;

/**
 * Port sortant : projection locale des comptes, alimentée par {@code coeur-bancaire.compte-modifie.v1} ; documents
 * n'appelle jamais coeur-bancaire et ne modifie jamais un solde.
 */
public interface ProjectionComptes {

    /**
     * Enregistre le compte dans la transaction courante si {@code versionAgregat} est strictement supérieure à celle de
     * la projection (ou si le compte est inconnu) ; faux, sans effet, pour un message ancien.
     */
    boolean enregistrerSiPlusRecent(CompteProjete compte);

    /** Compte d'identifiant donné, s'il est connu de la projection. */
    Optional<CompteProjete> trouver(long compteId);
}
