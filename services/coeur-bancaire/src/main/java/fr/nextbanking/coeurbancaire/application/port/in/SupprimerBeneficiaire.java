package fr.nextbanking.coeurbancaire.application.port.in;

/** Port entrant : suppression logique d'un bénéficiaire du client authentifié. */
public interface SupprimerBeneficiaire {

    /**
     * Désactive le bénéficiaire du client (la ligne est conservée).
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire absent, déjà désactivé (INTROUVABLE) ou d'un
     *         autre client (ETRANGER, BUG-047)
     */
    void supprimer(long clientId, long beneficiaireId);
}
