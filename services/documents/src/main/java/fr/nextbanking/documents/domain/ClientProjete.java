package fr.nextbanking.documents.domain;

import java.time.LocalDate;

/**
 * État d'un client tel que reçu sur {@code identite-clients.client-modifie.v1} : projection locale, jamais un appel à
 * identite-clients.
 */
public record ClientProjete(long clientId, String login, String nomComplet, Role role, boolean actif,
        Long conseillerId, LocalDate dateCreation, boolean supprime, long versionAgregat) {
}
