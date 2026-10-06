package fr.nextbanking.coeurbancaire.application.port.in;

/** Port entrant : l'administrateur libère un virement suspendu (FR-016-19). */
public interface LibererVirement {

    /**
     * Libère le virement avant son heure butoir si le solde source le couvre : il passe à LIBÉRÉ et est remis au
     * traitement asynchrone. Sinon il passe à REJETÉ, sans débit.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement rôle insuffisant, motif absent, virement qui n'est
     *         pas suspendu, ou remise impossible (le virement reste SUSPENDU)
     */
    IssueDecision liberer(CommandeDecision commande);
}
