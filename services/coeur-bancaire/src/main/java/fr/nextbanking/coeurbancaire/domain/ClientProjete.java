package fr.nextbanking.coeurbancaire.domain;

/**
 * État d'un client tel que reçu sur {@code identite-clients.client-modifie.v1} et retenu par la projection locale :
 * nom complet, conseiller affecté (absent possible), suppression et version de l'agrégat. Jamais un appel à
 * identite-clients.
 */
public record ClientProjete(long clientId, String nomComplet, Long conseillerId, boolean supprime,
        long versionAgregat) {
}
