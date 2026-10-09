package fr.nextbanking.documents.domain;

import java.math.BigDecimal;

/**
 * État d'un compte tel que reçu sur {@code coeur-bancaire.compte-modifie.v1} : projection locale, jamais un appel à
 * coeur-bancaire, qui reste seul à modifier un solde. Le solde est de ce seul compte, à 2 décimales.
 */
public record CompteProjete(long compteId, long clientId, String numero, TypeCompte type, String iban, String bic,
        BigDecimal solde, long versionAgregat) {

    public CompteProjete {
        if (numero == null || numero.isBlank() || type == null || iban == null || bic == null || solde == null) {
            throw new IllegalArgumentException("Le numéro, le type, l'IBAN, le BIC et le solde du compte sont obligatoires.");
        }
    }

    /** Vrai si le compte appartient au client. */
    public boolean appartientA(long client) {
        return clientId == client;
    }
}
