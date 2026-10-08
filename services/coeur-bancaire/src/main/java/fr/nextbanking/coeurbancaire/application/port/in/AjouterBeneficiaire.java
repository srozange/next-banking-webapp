package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;

/** Port entrant : ajout d'un bénéficiaire externe actif au client authentifié. */
public interface AjouterBeneficiaire {

    /**
     * Valide, normalise et enregistre le bénéficiaire du client.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurSaisieBeneficiaire saisie refusée : rien n'est enregistré
     */
    BeneficiaireExterne ajouter(long clientId, String libelle, String titulaire, String iban, String bic,
            String banque);
}
