package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.EcritureAudit.Entree;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.DecisionRevue;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.time.Instant;
import java.util.List;

/**
 * Rejet d'un virement suspendu à l'heure butoir : REJETÉ sans débit ni notification, audit TRANSFER_AUTO_REJECT par
 * {@code SYSTEM}. La transition conditionnelle tranche entre rejets concurrents : un seul gagne, un seul audit.
 */
final class RejetAutomatique {

    private final DepotVirements virements;
    private final Transaction transaction;
    private final Horloge horloge;
    private final EcritureAudit audit;

    RejetAutomatique(DepotVirements virements, Transaction transaction, Horloge horloge, JournalAudit audit) {
        this.virements = virements;
        this.transaction = transaction;
        this.horloge = horloge;
        this.audit = new EcritureAudit(audit, transaction);
    }

    /** Vrai si ce rejet a eu lieu ; faux si le virement n'était plus suspendu. */
    boolean rejeter(Virement virement) {
        Instant maintenant = horloge.instant();
        boolean rejete = transaction.executer(() -> virements.transiter(virement.id(), EtatVirement.SUSPENDU,
                EtatVirement.REJETE,
                new DecisionRevue(IssueRevue.REJETE_HEURE_BUTOIR, DecisionRevue.SYSTEME, null, maintenant)));
        if (rejete) {
            audit.ecrire(virement.id(), maintenant, List.of(new Entree(DecisionRevue.SYSTEME, null,
                    ActionAudit.TRANSFER_AUTO_REJECT, TextesVirement.detailRejetAutomatique(virement.id()))));
        }
        return rejete;
    }
}
