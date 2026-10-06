package fr.nextbanking.coeurbancaire.application.port.out;

/** Port sortant : bénéficiaires externes du client, en lecture seule (gestion : epic 4). */
public interface Beneficiaires {

    /** Vrai si le numéro est celui d'un bénéficiaire actif du client. */
    boolean estActif(long clientId, String numero);
}
