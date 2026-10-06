package fr.nextbanking.coeurbancaire.application.port.in;

/** Port entrant : rejet automatique des virements suspendus dont l'heure butoir est atteinte (FR-027-17). */
public interface RejeterEcheances {

    /** Rejette chaque virement échu, sans débit ni notification ; rend le nombre de virements rejetés. */
    int rejeterLesEchus();
}
