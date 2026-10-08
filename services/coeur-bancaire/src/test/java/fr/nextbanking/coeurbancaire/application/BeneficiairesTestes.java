package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.Iban;

/** Bénéficiaires de test du cas d'usage. */
final class BeneficiairesTestes {

    static final String IBAN = "FR3530004000000123456789010";

    private BeneficiairesTestes() {
    }

    static BeneficiaireExterne beneficiaire(long id, long clientId, String libelle) {
        return new BeneficiaireExterne(id, clientId, libelle, "Titulaire " + id, new Iban(IBAN), null, null);
    }
}
