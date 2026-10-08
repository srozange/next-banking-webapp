package fr.nextbanking.coeurbancaire.application.port.in;

/** Port entrant : exécution des échéances dues des virements programmés (FR-021-01 à FR-021-10). */
public interface ExecuterEcheances {

    /** Exécute au plus une échéance par virement actif et dû ; rend le nombre d'échéances exécutées. */
    int executerLesDues();
}
