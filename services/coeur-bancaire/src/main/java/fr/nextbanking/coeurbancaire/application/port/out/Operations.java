package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Operation;
import java.util.List;

/**
 * Port sortant : opérations au relevé.
 */
public interface Operations {

    /** Enregistre l'opération. */
    void enregistrer(Operation operation);

    /** Toutes les opérations du compte, sans limite, de la plus récente à la plus ancienne (date puis identifiant). */
    List<Operation> duCompte(long compteId);
}
