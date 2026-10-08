package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;

/** Port entrant : création d'un virement programmé par le client authentifié. */
public interface CreerVirementProgramme {

    /**
     * Crée le virement après validation de la saisie, du compte source puis du compte destinataire.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme si un contrôle échoue
     */
    LigneVirementProgramme creer(long clientId, Long compteSourceId, String numeroBeneficiaire,
            String libelleBeneficiaire, String montant, String frequence, String motif, String datePremiereExecution);
}
