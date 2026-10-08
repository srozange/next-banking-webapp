package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.VerificationIban;

/** Port entrant : vérification d'un IBAN saisi (MOD-97, code pays, zone SEPA), sans rien enregistrer. */
public interface VerifierIban {

    VerificationIban verifier(String iban);
}
