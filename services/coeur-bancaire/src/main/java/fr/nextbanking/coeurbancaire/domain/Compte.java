package fr.nextbanking.coeurbancaire.domain;


/**
 * Compte bancaire d'un client : identifiant numérique, propriétaire ({@code clientId}), numéro, type et solde.
 * Seul le cœur bancaire modifie un solde.
 */
public record Compte(long id, long clientId, String numero, TypeCompte type, Montant solde) {

    public Compte {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("Le numéro du compte est obligatoire.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Le type du compte est obligatoire.");
        }
        if (solde == null) {
            throw new IllegalArgumentException("Le solde du compte est obligatoire.");
        }
    }

    /** Vrai si le compte appartient au client. */
    public boolean appartientA(long client) {
        return clientId == client;
    }
}
