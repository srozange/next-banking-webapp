package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.VerifierIban;
import fr.nextbanking.coeurbancaire.domain.VerificationIban;

/** Cas d'usage : vérification d'un IBAN. Règle MOD-97 et zone SEPA portées par le domaine ; rien n'est journalisé. */
public class VerifierIbanCasUsage implements VerifierIban {

    @Override
    public VerificationIban verifier(String iban) {
        return VerificationIban.de(iban);
    }
}
