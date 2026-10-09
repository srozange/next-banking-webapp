package fr.nextbanking.identiteclients.domain;

/**
 * Client du portefeuille d'un conseiller, en lecture : identifiant numérique, nom complet, identifiant de connexion,
 * adresse électronique, téléphone (absent possible) et état actif.
 */
public record ClientDuPortefeuille(long clientId, String nomComplet, String login, String email, String telephone,
        boolean actif) {
}
