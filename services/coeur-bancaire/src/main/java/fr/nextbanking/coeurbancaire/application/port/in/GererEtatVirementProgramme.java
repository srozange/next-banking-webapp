package fr.nextbanking.coeurbancaire.application.port.in;

/** Port entrant : suspension, réactivation et suppression d'un virement programmé du client authentifié. */
public interface GererEtatVirementProgramme {

    /**
     * Passe le virement actif du client à inactif.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme INTROUVABLE : d'autrui (BUG-013),
     *         inexistant ou déjà inactif
     */
    void suspendre(long clientId, long virementId);

    /**
     * Passe le virement inactif du client à actif ; la prochaine exécution n'est pas recalculée.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme INTROUVABLE : d'autrui, inexistant ou
     *         déjà actif
     */
    void reactiver(long clientId, long virementId);

    /**
     * Supprime définitivement le virement du client, quel que soit son état.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme INTROUVABLE : d'autrui ou inexistant
     */
    void supprimer(long clientId, long virementId);
}
