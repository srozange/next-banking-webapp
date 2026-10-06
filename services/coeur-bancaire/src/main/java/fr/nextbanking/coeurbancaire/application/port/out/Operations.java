package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Operation;

/**
 * Port sortant : opérations au relevé.
 */
public interface Operations {

    /** Enregistre l'opération. */
    void enregistrer(Operation operation);
}
