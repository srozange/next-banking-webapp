package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.EcritureAudit.Entree;
import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.application.port.in.IssueDecision;
import fr.nextbanking.coeurbancaire.application.port.in.LibererVirement;
import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.DecisionRevue;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import fr.nextbanking.coeurbancaire.domain.VirementATraiter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage : l'administrateur libère un virement suspendu. Rôle ADMIN et motif contrôlés d'abord. À l'heure butoir
 * ou après, la libération est refusée et le virement est rejeté (l'heure butoir fait foi). Sinon, dans une seule
 * transaction, le solde source est contrôlé à cet instant : insuffisant, le virement est rejeté sans débit ; suffisant,
 * il passe à LIBÉRÉ par transition conditionnelle, ses alertes OPEN passent à REVIEWED et il est remis sur
 * {@code coeur-bancaire.virement-a-traiter.v1} (outbox) : si la remise échoue, rien n'est écrit et le virement reste
 * SUSPENDU. Le débit est fait ensuite par le consommateur. Aucune notification.
 */
public class LiberationVirementCasUsage implements LibererVirement {

    static final String ISSUE_EXECUTE = "EXECUTE";
    static final String ISSUE_REJETE = "REJETE";

    private final Comptes comptes;
    private final DepotVirements virements;
    private final AlertesFraude alertes;
    private final EvenementsSortants evenements;
    private final Transaction transaction;
    private final Horloge horloge;
    private final EcritureAudit audit;

    public LiberationVirementCasUsage(Comptes comptes, DepotVirements virements, AlertesFraude alertes,
            EvenementsSortants evenements, JournalAudit audit, Transaction transaction, Horloge horloge) {
        this.comptes = comptes;
        this.virements = virements;
        this.alertes = alertes;
        this.evenements = evenements;
        this.transaction = transaction;
        this.horloge = horloge;
        this.audit = new EcritureAudit(audit, transaction);
    }

    @Override
    public IssueDecision liberer(CommandeDecision commande) {
        String motif = DecisionAdministrateur.motifRetenu(commande);
        Virement virement = virements.parId(commande.virementId())
                .filter(trouve -> trouve.etat() == EtatVirement.SUSPENDU)
                .orElseThrow(() -> new ErreurVirement(Cause.VIREMENT_NON_SUSPENDU));
        Instant maintenant = horloge.instant();
        Resultat resultat;
        try {
            resultat = transaction.executer(() -> decider(virement, commande, motif, maintenant));
        } catch (ErreurVirement refus) {
            throw refus;
        } catch (RuntimeException echec) {
            throw new ErreurVirement(Cause.REMISE_IMPOSSIBLE, echec);
        }
        if (resultat.issue() == IssueDecision.ACCEPTEE) {
            evenements.publier(resultat.ligneRemise());
        }
        String issueAudit = resultat.issue() == IssueDecision.ACCEPTEE ? ISSUE_EXECUTE : ISSUE_REJETE;
        audit.ecrire(virement.id(), maintenant, List.of(new Entree(Long.toString(commande.acteurId()),
                commande.adresseIp(), ActionAudit.ADMIN_TRANSFER_RELEASE,
                TextesVirement.detailLiberation(virement.id(), motif, issueAudit))));
        return resultat.issue();
    }

    private Resultat decider(Virement virement, CommandeDecision commande, String motif, Instant maintenant) {
        String acteur = Long.toString(commande.acteurId());
        if (virement.heureButoirAtteinte(maintenant)) {
            rejeter(virement, acteur, motif, IssueRevue.REJETE_HEURE_BUTOIR, maintenant);
            return new Resultat(IssueDecision.REFUSEE_HEURE_BUTOIR, 0);
        }
        List<Compte> verrouilles = comptes.verrouiller(virement.compteSourceId().valeur(),
                virement.compteDestinataireId().valeur());
        Compte source = verrouilles.stream().filter(compte -> compte.id() == virement.compteSourceId().valeur()).findFirst()
                .orElseThrow(() -> new ErreurVirement(Cause.COMPTE_INTROUVABLE));
        if (source.solde().compareTo(virement.montant()) < 0) {
            rejeter(virement, acteur, motif, IssueRevue.REJETE, maintenant);
            return new Resultat(IssueDecision.REFUSEE_SOLDE_INSUFFISANT, 0);
        }
        transiter(virement, EtatVirement.LIBERE, new DecisionRevue(IssueRevue.LIBERE, acteur, motif, maintenant));
        alertes.passerEnRevue(virement.id(), acteur, maintenant);
        long ligne = evenements.consigner(new VirementATraiter(UUID.randomUUID(), virement.id(), virement.montant(),
                virement.clientId(), virement.compteDestinataireId(), virement.motif(), maintenant));
        return new Resultat(IssueDecision.ACCEPTEE, ligne);
    }

    private void rejeter(Virement virement, String acteur, String motif, IssueRevue issue, Instant maintenant) {
        transiter(virement, EtatVirement.REJETE, new DecisionRevue(issue, acteur, motif, maintenant));
        alertes.passerEnRevue(virement.id(), acteur, maintenant);
    }

    /** Transition conditionnelle : si un autre traitement a déjà décidé, la libération est sans objet. */
    private void transiter(Virement virement, EtatVirement vers, DecisionRevue decision) {
        if (!virements.transiter(virement.id(), EtatVirement.SUSPENDU, vers, decision)) {
            throw new ErreurVirement(Cause.VIREMENT_NON_SUSPENDU);
        }
    }

    private record Resultat(IssueDecision issue, long ligneRemise) {
    }
}
