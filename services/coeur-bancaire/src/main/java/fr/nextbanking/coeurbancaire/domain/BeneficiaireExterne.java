package fr.nextbanking.coeurbancaire.domain;

/**
 * Bénéficiaire externe actif d'un client. {@code bic} et {@code banque} sont nuls quand ils ne sont pas renseignés.
 * La zone SEPA est calculée depuis le code pays de l'IBAN (RG-013-010), jamais lue.
 */
public record BeneficiaireExterne(long id, long clientId, String libelle, String titulaire, Iban iban, String bic,
        String banque) {

    /** Vrai si le bénéficiaire est celui du client. */
    public boolean appartientA(long client) {
        return clientId == client;
    }
}
