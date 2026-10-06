package fr.nextbanking.coeurbancaire.application.port.in;

/** Port entrant : l'administrateur rejette un virement suspendu (FR-016-19). */
public interface RejeterVirement {

    /**
     * Rejette le virement, sans débit. À l'heure butoir ou après, le rejet est celui de l'échéance.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement rôle insuffisant, motif absent, ou virement qui
     *         n'est pas suspendu
     */
    IssueDecision rejeter(CommandeDecision commande);
}
