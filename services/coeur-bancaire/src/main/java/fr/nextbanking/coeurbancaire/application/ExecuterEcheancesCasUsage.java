package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.EcritureAudit.Entree;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterEcheances;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch;
import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch.Statut;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.ControleFraude.Decision;
import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import fr.nextbanking.coeurbancaire.domain.DemandeVirement;
import fr.nextbanking.coeurbancaire.domain.EcheanceNonExecutee;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.NotificationVirement;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.Soumission;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import fr.nextbanking.coeurbancaire.domain.Virement.Execution;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cas d'usage planifié : exécute les échéances dues des virements programmés (FEAT-021). Une échéance par virement et
 * par passage, chacune dans sa transaction : numéro du bénéficiaire, verrou des deux comptes, prise conditionnelle de
 * l'échéance (une prise perdue est sans effet), solde, contrôle de fraude puis débit, crédit et deux opérations. Un
 * échec isole l'échéance (annulée entièrement) sans arrêter les autres. Une échéance exécutée ne notifie pas ; une
 * échéance dont le destinataire est introuvable (évalué avant le solde) est refusée et désactivée, une échéance au
 * solde insuffisant reste due (transaction annulée) : chacune notifie le client une seule fois (événement par
 * l'outbox, seconde transaction pour le solde insuffisant). Chaque passage écrit une ligne {@code RECURRING_TRANSFERS}
 * au journal d'exécution.
 */
public class ExecuterEcheancesCasUsage implements ExecuterEcheances {

    private static final Logger JOURNAL = Logger.getLogger(ExecuterEcheancesCasUsage.class.getName());
    static final String JOB = "RECURRING_TRANSFERS";
    static final String ISSUE_EXECUTE = "EXECUTE";
    private static final String SEPARATEUR_IDENTIFIANTS = "; ";

    private final VirementsProgrammes echeances;
    private final Comptes comptes;
    private final Operations operations;
    private final DepotVirements virements;
    private final EcritureAudit audit;
    private final JournalBatch journalBatch;
    private final Transaction transaction;
    private final EvenementsSortants evenements;
    private final Horloge horloge;
    private final SeuilsFraude seuils;
    private final ControleFraudeCasUsage controle;

    public ExecuterEcheancesCasUsage(VirementsProgrammes echeances, Comptes comptes, Operations operations,
            DepotVirements virements, JournalAudit audit, JournalBatch journalBatch, Transaction transaction,
            EvenementsSortants evenements, Horloge horloge, SeuilsFraude seuils, ControleFraudeCasUsage controle) {
        this.echeances = echeances;
        this.comptes = comptes;
        this.operations = operations;
        this.virements = virements;
        this.audit = new EcritureAudit(audit, transaction);
        this.journalBatch = journalBatch;
        this.transaction = transaction;
        this.evenements = evenements;
        this.horloge = horloge;
        this.seuils = seuils;
        this.controle = controle;
    }

    @Override
    public int executerLesDues() {
        Instant debut = horloge.instant();
        JOURNAL.info(TextesVirement.detailsVerification());
        List<VirementProgramme> dues;
        try {
            dues = echeances.dues(debut);
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Lecture des virements programmés dus en erreur", echec);
            journaliser(debut, 0, Statut.FAILED, TextesVirement.detailsErreur(echec.getMessage()));
            return 0;
        }
        int executees = 0;
        List<String> echecs = new ArrayList<>();
        for (VirementProgramme due : dues) {
            try {
                if (executer(due)) {
                    executees++;
                }
            } catch (ErreurVirement refus) {
                if (refus.motif() == Cause.SOLDE_INSUFFISANT) {
                    JOURNAL.warning(TextesVirement.journalSoldeInsuffisant(due.id()));
                    notifierSoldeInsuffisant(due, echecs);
                } else {
                    echouer(due, echecs, refus);
                }
            } catch (NonExecutee suspendue) {
                JOURNAL.warning(TextesVirement.journalEchec(due.id()));
            } catch (RuntimeException echec) {
                echouer(due, echecs, echec);
            }
        }
        if (echecs.isEmpty()) {
            journaliser(debut, executees, Statut.SUCCESS, TextesVirement.detailsVerification());
        } else {
            journaliser(debut, executees, executees == 0 ? Statut.FAILED : Statut.PARTIAL,
                    String.join(SEPARATEUR_IDENTIFIANTS, echecs));
        }
        return executees;
    }

    private static void echouer(VirementProgramme due, List<String> echecs, RuntimeException echec) {
        String texte = TextesVirement.journalEchec(due.id());
        JOURNAL.log(Level.WARNING, texte, echec);
        echecs.add(texte);
    }

    /** Exécute l'échéance dans sa transaction ; faux si elle n'est pas exécutée (destinataire introuvable, prise perdue). */
    private boolean executer(VirementProgramme due) {
        Instant executeLe = horloge.instant();
        Resultat resultat = transaction.executer(() -> ecrire(due, executeLe));
        if (resultat.ligne() != null) {
            evenements.publier(resultat.ligne());
        }
        if (resultat.execution() == null) {
            return false;
        }
        Virement virement = resultat.execution().virement();
        String montant = virement.montant().texte();
        String utilisateur = Long.toString(due.clientId());
        audit.ecrire(virement.id(), executeLe, List.of(
                new Entree(utilisateur, null, ActionAudit.VIREMENT,
                        TextesVirement.detailVirement(montant, virement.motif().texte(), ISSUE_EXECUTE)),
                new Entree(utilisateur, null, ActionAudit.TRANSFER_EXECUTED,
                        TextesVirement.detailExecution(montant, virement.motif().texte()))));
        JOURNAL.info(TextesVirement.journalExecute(due.id(), montant));
        return true;
    }

    private Resultat ecrire(VirementProgramme due, Instant executeLe) {
        Optional<Compte> beneficiaire = comptes.parNumero(due.numeroBeneficiaire());
        if (beneficiaire.isEmpty()) {
            JOURNAL.warning(TextesVirement.journalEchec(due.id()));
            return refuser(due);
        }
        long sourceId = due.compteSourceId();
        long destinataireId = beneficiaire.get().id();
        List<Compte> verrouilles = comptes.verrouiller(sourceId, destinataireId);
        Compte source = trouver(verrouilles, sourceId);
        Compte destinataire = trouver(verrouilles, destinataireId);
        Instant prochaine = due.frequence().suivante(due.prochaineExecution(), horloge.fuseau());
        if (!echeances.prendreEcheance(due.id(), due.prochaineExecution(), prochaine, executeLe)) {
            return Resultat.AUCUN;
        }
        Virement.verifierEcheance(due.clientId(), source, due.montant());
        Evaluation evaluation = controle.evaluer(due.clientId(), due.montant().valeur(), destinataire.numero(),
                executeLe);
        Decision decision = evaluation.decider(due.montant().valeur(), seuils.seuilFraude());
        if (decision.etat() == EtatVirement.SUSPENDU) {
            throw new NonExecutee();
        }
        long virementId = virements.attribuerIdentifiant();
        Soumission soumission = new Soumission(virementId, due.clientId(),
                new DemandeVirement(new CompteId(sourceId), new CompteId(destinataireId), due.montant(),
                        Motif.depuis(due.motif())),
                executeLe);
        Execution execution = Virement.executerEcheance(soumission, source, destinataire,
                due.libelleBeneficiaire(), seuils);
        virements.enregistrer(execution.virement(), null);
        controle.enregistrerAlertes(evaluation, due.clientId(), virementId, executeLe);
        if (!comptes.debiter(new CompteId(sourceId), due.montant())) {
            throw new ErreurVirement(Cause.SOLDE_INSUFFISANT);
        }
        comptes.crediter(new CompteId(destinataireId), due.montant());
        operations.enregistrer(execution.debit());
        operations.enregistrer(execution.credit());
        return new Resultat(execution, null);
    }

    /**
     * Refus « compte introuvable », avant tout contrôle du solde : désactive le virement programmé (mise à jour
     * conditionnelle, une désactivation perdue est sans effet), marque l'échéance et consigne l'événement dans la
     * même transaction. Aucune écriture de compte, d'opération ni de virement.
     */
    private Resultat refuser(VirementProgramme due) {
        if (!echeances.desactiver(due.id(), due.prochaineExecution())) {
            return Resultat.AUCUN;
        }
        Long ligne = consignerNotification(due,
                NotificationVirement.pourEcheanceCompteIntrouvable(due.clientId(), due.libelleBeneficiaire()));
        return new Resultat(null, ligne);
    }

    /**
     * Solde insuffisant : la transaction d'exécution est annulée (échéance intacte, toujours due) ; la notification est
     * écrite dans une seconde transaction, puis publiée après validation. Un échec de cette écriture est compté en
     * échec du passage.
     */
    private void notifierSoldeInsuffisant(VirementProgramme due, List<String> echecs) {
        try {
            Long ligne = transaction.executer(() -> consignerNotification(due,
                    NotificationVirement.pourEcheanceSoldeInsuffisant(due.clientId(), due.libelleBeneficiaire())));
            if (ligne != null) {
                evenements.publier(ligne);
            }
        } catch (RuntimeException echec) {
            echouer(due, echecs, echec);
        }
    }

    /** Marque l'échéance notifiée et consigne l'événement ; rend la ligne d'outbox, ou nul si déjà notifiée. */
    private Long consignerNotification(VirementProgramme due, NotificationVirement notification) {
        Instant maintenant = horloge.instant();
        if (!echeances.marquerNotifiee(due.id(), due.prochaineExecution(), maintenant)) {
            return null;
        }
        return evenements.consigner(new EcheanceNonExecutee(UUID.randomUUID(), due.id(), notification, maintenant));
    }

    private static Compte trouver(List<Compte> comptes, long id) {
        return comptes.stream().filter(compte -> compte.id() == id).findFirst()
                .orElseThrow(() -> new ErreurVirement(Cause.COMPTE_INTROUVABLE));
    }

    private void journaliser(Instant debut, int executees, Statut statut, String details) {
        try {
            journalBatch.enregistrer(JOB, debut, horloge.instant(), executees, statut, details);
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Ligne du journal d'exécution " + JOB + " non écrite", echec);
        }
    }

    /** Issue de l'écriture d'une échéance : virement exécuté, et ligne d'outbox à publier après validation. */
    private record Resultat(Execution execution, Long ligne) {
        static final Resultat AUCUN = new Resultat(null, null);
    }

    /** Échéance non exécutée (suspendue par le contrôle de fraude) : annule la transaction, sans échec compté. */
    private static final class NonExecutee extends RuntimeException {
        private static final long serialVersionUID = 1L;

        NonExecutee() {
            super(null, null, false, false);
        }
    }
}
