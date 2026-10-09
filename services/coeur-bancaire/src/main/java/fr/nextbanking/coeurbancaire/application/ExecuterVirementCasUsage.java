package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.EcritureAudit.Entree;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.ControleEnErreur;
import fr.nextbanking.coeurbancaire.domain.DemandeVirement;
import fr.nextbanking.coeurbancaire.domain.ControleFraude.Decision;
import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.NotificationVirement;
import fr.nextbanking.coeurbancaire.domain.Soumission;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import fr.nextbanking.coeurbancaire.domain.VirementATraiter;
import fr.nextbanking.coeurbancaire.domain.VirementDecide;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cas d'usage : virement immédiat. Une clé d'idempotence déjà servie rend le premier résultat. Sinon, la saisie est
 * contrôlée, puis une seule transaction verrouille les comptes, contrôle propriété et solde, évalue les signaux de
 * fraude (FEAT-027) et décide avant tout débit : EXÉCUTÉ (débit conditionnel, crédit, deux opérations, remise au
 * traitement asynchrone) ou SUSPENDU (ni débit, ni crédit, ni opération, ni remise ; heure butoir à 24 heures). La
 * décision et sa notification passent par l'outbox dans la même transaction : un échec d'écriture annule tout.
 * Quand le contrôle est en erreur, la transaction est annulée puis le virement est enregistré REFUSÉ_ERREUR_CONTRÔLE
 * dans une transaction à part, sans notification. L'audit passe par l'outbox d'audit, dans des transactions
 * indépendantes (FR-026-04) : il est conservé même quand le virement est annulé (BUG-073).
 */
public class ExecuterVirementCasUsage implements ExecuterVirement {

    private static final Logger JOURNAL = Logger.getLogger(ExecuterVirementCasUsage.class.getName());
    static final String ISSUE_EXECUTE = "EXECUTE";
    static final String ISSUE_SUSPENDU = "SUSPENDU";
    static final String ISSUE_REFUS_CONTROLE = "REFUSE_ERREUR_CONTROLE";
    static final String ISSUE_ECHEC_REMISE = "ECHEC_REMISE";

    private final Comptes comptes;
    private final Operations operations;
    private final DepotVirements virements;
    private final EvenementsSortants evenements;
    private final EcritureAudit audit;
    private final Transaction transaction;
    private final Horloge horloge;
    private final SeuilsFraude seuils;
    private final ControleFraudeCasUsage controle;
    private final EvenementsComptes evenementsComptes;

    public ExecuterVirementCasUsage(Comptes comptes, Operations operations, DepotVirements virements,
            EvenementsSortants evenements, JournalAudit audit, Transaction transaction, Horloge horloge,
            SeuilsFraude seuils, ControleFraudeCasUsage controle) {
        this.comptes = comptes;
        this.operations = operations;
        this.virements = virements;
        this.evenements = evenements;
        this.audit = new EcritureAudit(audit, transaction);
        this.transaction = transaction;
        this.horloge = horloge;
        this.seuils = seuils;
        this.controle = controle;
        this.evenementsComptes = new EvenementsComptes(comptes, evenements);
    }

    @Override
    public Resultat executer(Commande commande) {
        if (commande.cleIdempotence() != null) {
            var premier = virements.parCle(commande.clientId(), commande.cleIdempotence());
            if (premier.isPresent()) {
                return rejouer(premier.get());
            }
        }
        DemandeVirement demande = DemandeVirement.lire(commande.compteSourceId(), commande.compteDestinataireId(),
                commande.montant(), commande.motif());
        Instant soumisLe = horloge.instant();
        long virementId = virements.attribuerIdentifiant();
        Soumission soumission = new Soumission(virementId, commande.clientId(), demande, soumisLe);
        Ecriture ecriture;
        try {
            ecriture = transaction.executer(() -> ecrire(soumission, commande.cleIdempotence()));
        } catch (ErreurVirement refus) {
            if (refus.motif() == Cause.CLE_IDEMPOTENCE_DEJA_UTILISEE) {
                return rejouer(virements.parCle(commande.clientId(), commande.cleIdempotence()).orElseThrow());
            }
            throw refus;
        } catch (ControleEnErreur erreur) {
            refuser(soumission, commande);
            throw new ErreurVirement(Cause.CONTROLE_EN_ERREUR, erreur);
        } catch (RuntimeException echec) {
            auditer(commande, virementId, soumisLe, ActionAudit.VIREMENT,
                    TextesVirement.detailVirement(demande.montant().texte(), commande.motif(), ISSUE_ECHEC_REMISE));
            throw new ErreurVirement(Cause.REMISE_IMPOSSIBLE, echec);
        }
        evenements.publier(ecriture.ligneDecision());
        ecriture.lignesComptes().forEach(evenements::publier);
        String montantTexte = demande.montant().texte();
        if (ecriture.virement().etat() == EtatVirement.SUSPENDU) {
            auditer(commande, virementId, soumisLe, ActionAudit.VIREMENT,
                    TextesVirement.detailVirement(montantTexte, commande.motif(), ISSUE_SUSPENDU));
            return resultat(ecriture.virement());
        }
        evenements.publier(ecriture.ligneRemise());
        audit.ecrire(virementId, soumisLe, List.of(
                entree(commande, ActionAudit.VIREMENT,
                        TextesVirement.detailVirement(montantTexte, commande.motif(), ISSUE_EXECUTE)),
                entree(commande, ActionAudit.TRANSFER_EXECUTED,
                        TextesVirement.detailExecution(montantTexte, commande.motif()))));
        return resultat(ecriture.virement());
    }

    private Ecriture ecrire(Soumission soumission, String cleIdempotence) {
        DemandeVirement demande = soumission.demande();
        CompteId sourceId = demande.source();
        CompteId destinataireId = demande.destinataire();
        Montant montant = demande.montant();
        long virementId = soumission.id();
        long clientId = soumission.clientId();
        Instant soumisLe = soumission.soumisLe();
        List<Compte> verrouilles = comptes.verrouiller(sourceId.valeur(), destinataireId.valeur());
        Compte source = trouver(verrouilles, sourceId.valeur());
        Compte destinataire = trouver(verrouilles, destinataireId.valeur());
        Virement.verifierComptes(clientId, source, destinataire, montant);
        Evaluation evaluation = controle.evaluer(clientId, montant.valeur(), destinataire.numero(), soumisLe);
        Decision decision = evaluation.decider(montant.valeur(), seuils.seuilFraude());
        if (decision.etat() == EtatVirement.SUSPENDU) {
            Virement suspendu = Virement.suspendre(soumission, seuils, decision.motifSuspension());
            virements.enregistrer(suspendu, cleIdempotence);
            controle.enregistrerAlertes(evaluation, clientId, virementId, soumisLe);
            long ligne = evenements.consigner(new VirementDecide(UUID.randomUUID(), virementId,
                    clientId, suspendu.etat(),
                    NotificationVirement.pourSuspension(suspendu, destinataire.numero(), horloge.fuseau()),
                    soumisLe));
            return new Ecriture(suspendu, ligne, 0, List.of());
        }
        Virement.Execution execution = Virement.executer(soumission, source, destinataire, seuils);
        Virement virement = execution.virement();
        virements.enregistrer(virement, cleIdempotence);
        controle.enregistrerAlertes(evaluation, clientId, virementId, soumisLe);
        if (!comptes.debiter(sourceId, montant)) {
            throw new ErreurVirement(Cause.SOLDE_INSUFFISANT);
        }
        comptes.crediter(destinataireId, montant);
        long idDebit = operations.enregistrer(execution.debit());
        long idCredit = operations.enregistrer(execution.credit());
        // Un événement par compte modifié puis un par opération, dans la même transaction.
        List<Long> lignesComptes = new ArrayList<>(evenementsComptes.consigner(soumisLe, sourceId, destinataireId));
        lignesComptes.addAll(evenementsComptes.consignerOperations(soumisLe, execution.debit(), idDebit,
                execution.credit(), idCredit));
        // Une seule notification par virement, à la décision : jamais à la remise asynchrone.
        long decisionLigne = evenements.consigner(new VirementDecide(UUID.randomUUID(), virementId,
                clientId, virement.etat(),
                NotificationVirement.pourExecution(virement, destinataire.numero(), seuils), soumisLe));
        long remise = evenements.consigner(new VirementATraiter(UUID.randomUUID(), virementId, montant,
                clientId, destinataireId, virement.motif(), soumisLe));
        return new Ecriture(virement, decisionLigne, remise, lignesComptes);
    }

    /**
     * Le contrôle est en erreur et la transaction de soumission est annulée : le virement est enregistré refusé, sans
     * solde, opération, remise ni notification, avec la seule entrée d'audit « Issue: REFUSE_ERREUR_CONTROLE ».
     */
    private void refuser(Soumission soumission, Commande commande) {
        long virementId = soumission.id();
        try {
            Virement refuse = Virement.refuser(soumission, seuils);
            transaction.executer(() -> {
                virements.enregistrer(refuse, commande.cleIdempotence());
                return null;
            });
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Virement " + virementId + " refusé non enregistré", echec);
        }
        auditer(commande, virementId, soumission.soumisLe(), ActionAudit.VIREMENT,
                TextesVirement.detailVirement(soumission.demande().montant().texte(), commande.motif(),
                        ISSUE_REFUS_CONTROLE));
    }

    private static Compte trouver(List<Compte> comptes, long id) {
        return comptes.stream().filter(compte -> compte.id() == id).findFirst()
                .orElseThrow(() -> new ErreurVirement(Cause.COMPTE_INTROUVABLE));
    }

    private void auditer(Commande commande, long virementId, Instant horodatage, ActionAudit action, String detail) {
        audit.ecrire(virementId, horodatage, List.of(entree(commande, action, detail)));
    }

    private static Entree entree(Commande commande, ActionAudit action, String detail) {
        return new Entree(Long.toString(commande.clientId()), commande.adresseIp(), action, detail);
    }

    /** Premier résultat d'une clé d'idempotence : un virement refusé pour erreur de contrôle est refusé de nouveau. */
    private static Resultat rejouer(Virement virement) {
        if (virement.etat() == EtatVirement.REFUSE_ERREUR_CONTROLE) {
            throw new ErreurVirement(Cause.CONTROLE_EN_ERREUR);
        }
        return resultat(virement);
    }

    private static Resultat resultat(Virement virement) {
        return new Resultat(virement.id(), virement.etat(), virement.heureButoir(), virement.statut(),
                virement.compteSourceId().valeur(), virement.compteDestinataireId().valeur(), virement.montant().valeur(),
                virement.motif().texte());
    }

    /**
     * {@code ligneRemise} vaut 0 pour un virement suspendu : aucune remise ; {@code lignesComptes} est alors vide
     * (aucun compte modifié).
     */
    private record Ecriture(Virement virement, long ligneDecision, long ligneRemise, List<Long> lignesComptes) {
    }
}
