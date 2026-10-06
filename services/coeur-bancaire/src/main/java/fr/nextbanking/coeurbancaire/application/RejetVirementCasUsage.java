package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.EcritureAudit.Entree;
import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.application.port.in.IssueDecision;
import fr.nextbanking.coeurbancaire.application.port.in.RejeterVirement;
import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.DecisionRevue;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.time.Instant;
import java.util.List;

/**
 * Cas d'usage : l'administrateur rejette un virement suspendu, sans débit ni notification. Avant l'heure butoir, la
 * transition conditionnelle vers REJETÉ et le passage des alertes OPEN à REVIEWED sont écrits ensemble, puis l'audit
 * ADMIN_TRANSFER_REJECT. À l'heure butoir ou après, le rejet de l'administrateur est sans objet : c'est le rejet de
 * l'échéance (A-016-6).
 */
public class RejetVirementCasUsage implements RejeterVirement {

    private final DepotVirements virements;
    private final AlertesFraude alertes;
    private final Transaction transaction;
    private final Horloge horloge;
    private final EcritureAudit audit;
    private final RejetAutomatique rejetAutomatique;

    public RejetVirementCasUsage(DepotVirements virements, AlertesFraude alertes, JournalAudit audit,
            Transaction transaction, Horloge horloge) {
        this.virements = virements;
        this.alertes = alertes;
        this.transaction = transaction;
        this.horloge = horloge;
        this.audit = new EcritureAudit(audit, transaction);
        this.rejetAutomatique = new RejetAutomatique(virements, transaction, horloge, audit);
    }

    @Override
    public IssueDecision rejeter(CommandeDecision commande) {
        String motif = DecisionAdministrateur.motifRetenu(commande);
        Virement virement = virements.parId(commande.virementId())
                .filter(trouve -> trouve.etat() == EtatVirement.SUSPENDU)
                .orElseThrow(() -> new ErreurVirement(Cause.VIREMENT_NON_SUSPENDU));
        Instant maintenant = horloge.instant();
        if (virement.heureButoirAtteinte(maintenant)) {
            if (!rejetAutomatique.rejeter(virement)) {
                throw new ErreurVirement(Cause.VIREMENT_NON_SUSPENDU);
            }
            return IssueDecision.REFUSEE_HEURE_BUTOIR;
        }
        String acteur = Long.toString(commande.acteurId());
        transaction.executer(() -> {
            if (!virements.transiter(virement.id(), EtatVirement.SUSPENDU, EtatVirement.REJETE,
                    new DecisionRevue(IssueRevue.REJETE, acteur, motif, maintenant))) {
                throw new ErreurVirement(Cause.VIREMENT_NON_SUSPENDU);
            }
            return alertes.passerEnRevue(virement.id(), acteur, maintenant);
        });
        audit.ecrire(virement.id(), maintenant, List.of(new Entree(acteur, commande.adresseIp(),
                ActionAudit.ADMIN_TRANSFER_REJECT, TextesVirement.detailRejet(virement.id(), motif))));
        return IssueDecision.ACCEPTEE;
    }
}
