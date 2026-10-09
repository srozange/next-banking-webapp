package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.EcritureAudit.Entree;
import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage idempotent : consomme la remise d'un virement au traitement asynchrone. Dans une même transaction,
 * l'événement est marqué traité puis le virement est lu. Un virement exécuté est déjà débité : aucun effet. Un
 * virement LIBÉRÉ est débité par mise à jour conditionnelle ({@code solde >= montant}), crédité, inscrit au relevé
 * par ses deux opérations et passé à EXÉCUTÉ ; si le solde est devenu insuffisant, il passe à REJETÉ sans débit ni
 * crédit (audit TRANSFER_REJECT_INSUFFICIENT_FUNDS par {@code SYSTEM}). Aucune notification dans aucun cas. L'audit
 * est écrit après la transaction, par l'outbox d'audit.
 */
public class TraiterVirementATraiterCasUsage implements TraiterVirementATraiter {

    private final DepotVirements virements;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;
    private final Horloge horloge;
    private final Comptes comptes;
    private final Operations operations;
    private final EvenementsSortants evenements;
    private final EcritureAudit audit;
    private final EvenementsComptes evenementsComptes;

    public TraiterVirementATraiterCasUsage(DepotVirements virements, EvenementsTraites evenementsTraites,
            Transaction transaction, Horloge horloge, Comptes comptes, Operations operations,
            JournalAudit audit, EvenementsSortants evenements) {
        this.virements = virements;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
        this.horloge = horloge;
        this.comptes = comptes;
        this.operations = operations;
        this.audit = new EcritureAudit(audit, transaction);
        this.evenements = evenements;
        this.evenementsComptes = new EvenementsComptes(comptes, evenements);
    }

    @Override
    public Issue traiter(UUID idEvenement, long virementId) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        Instant maintenant = horloge.instant();
        Traitement traitement = transaction.executer(() -> traiterLeVirement(idEvenement, virementId, maintenant));
        traitement.lignesComptes().forEach(evenements::publier);
        if (traitement.audit() != null) {
            audit.ecrire(virementId, maintenant, List.of(traitement.audit()));
        }
        return traitement.issue();
    }

    private Traitement traiterLeVirement(UUID idEvenement, long virementId, Instant maintenant) {
        if (!evenementsTraites.marquerTraite(idEvenement, maintenant)) {
            return Traitement.sans(Issue.DEJA_TRAITE);
        }
        Optional<Virement> lu = virements.parId(virementId);
        if (lu.isEmpty()) {
            return Traitement.sans(Issue.VIREMENT_INCONNU);
        }
        Virement virement = lu.get();
        if (virement.etat() == EtatVirement.EXECUTE) {
            return Traitement.sans(Issue.DEJA_EXECUTE);
        }
        if (virement.etat() != EtatVirement.LIBERE) {
            return Traitement.sans(Issue.ETAT_INATTENDU);
        }
        List<Compte> verrouilles = comptes.verrouiller(virement.compteSourceId().valeur(),
                virement.compteDestinataireId().valeur());
        Compte source = trouver(verrouilles, virement.compteSourceId().valeur());
        Compte destinataire = trouver(verrouilles, virement.compteDestinataireId().valeur());
        if (!comptes.debiter(virement.compteSourceId(), virement.montant())) {
            transiter(virement, EtatVirement.REJETE,
                    new DecisionRevue(IssueRevue.REJETE_SOLDE_AU_DEBIT, DecisionRevue.SYSTEME, null, maintenant));
            return new Traitement(Issue.REJETE_SOLDE_INSUFFISANT, new Entree(DecisionRevue.SYSTEME, null,
                    ActionAudit.TRANSFER_REJECT_INSUFFICIENT_FUNDS, TextesVirement.detailRejetSoldeAuDebit(virementId)),
                    List.of());
        }
        comptes.crediter(virement.compteDestinataireId(), virement.montant());
        Virement.Execution execution = virement.executerApresLiberation(source, destinataire, maintenant);
        long idDebit = operations.enregistrer(execution.debit());
        long idCredit = operations.enregistrer(execution.credit());
        transiter(virement, EtatVirement.EXECUTE, null);
        // Un événement par compte modifié puis un par opération, dans la transaction du débit.
        List<Long> lignesComptes = new ArrayList<>(evenementsComptes.consigner(maintenant,
                virement.compteSourceId(), virement.compteDestinataireId()));
        lignesComptes.addAll(evenementsComptes.consignerOperations(maintenant, execution.debit(), idDebit,
                execution.credit(), idCredit));
        return new Traitement(Issue.EXECUTE_APRES_LIBERATION, new Entree(Long.toString(virement.clientId()), null,
                ActionAudit.TRANSFER_EXECUTED,
                TextesVirement.detailExecution(virement.montant().texte(), virement.motif().texte())),
                lignesComptes);
    }

    /** Un autre traitement a décidé entre-temps : la transaction est annulée, le message sera rejoué. */
    private void transiter(Virement virement, EtatVirement vers, DecisionRevue decision) {
        if (!virements.transiter(virement.id(), EtatVirement.LIBERE, vers, decision)) {
            throw new ErreurVirement(Cause.VIREMENT_NON_SUSPENDU);
        }
    }

    private static Compte trouver(List<Compte> comptes, long id) {
        return comptes.stream().filter(compte -> compte.id() == id).findFirst()
                .orElseThrow(() -> new ErreurVirement(Cause.COMPTE_INTROUVABLE));
    }

    /** Issue du traitement, entrée d'audit à écrire après validation et lignes d'outbox des comptes à publier. */
    private record Traitement(Issue issue, Entree audit, List<Long> lignesComptes) {

        static Traitement sans(Issue issue) {
            return new Traitement(issue, null, List.of());
        }
    }
}
