package fr.nextbanking.coeurbancaire.application;

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
import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.NotificationVirement;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import fr.nextbanking.coeurbancaire.domain.VirementATraiter;
import fr.nextbanking.coeurbancaire.domain.VirementDecide;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cas d'usage : virement immédiat. Une clé d'idempotence déjà servie rend le premier résultat. Sinon, la saisie est
 * contrôlée, puis une seule transaction verrouille les comptes, contrôle propriété et solde, évalue les signaux de
 * fraude (FEAT-027, avant tout débit), enregistre le virement et ses alertes, débite (mise à jour conditionnelle),
 * crédite, enregistre ses deux opérations, la ligne d'outbox de la décision (notification) et celle de la remise au
 * traitement asynchrone : un échec d'écriture annule tout. L'audit passe par l'outbox d'audit, dans des transactions indépendantes
 * (FR-026-04) : il est conservé même quand le virement est annulé (BUG-073).
 */
public class ExecuterVirementCasUsage implements ExecuterVirement {

    private static final Logger JOURNAL = Logger.getLogger(ExecuterVirementCasUsage.class.getName());
    static final String ISSUE_EXECUTE = "EXECUTE";
    static final String ISSUE_ECHEC_REMISE = "ECHEC_REMISE";

    private final Comptes comptes;
    private final Operations operations;
    private final DepotVirements virements;
    private final EvenementsSortants evenements;
    private final JournalAudit audit;
    private final Transaction transaction;
    private final Horloge horloge;
    private final SeuilsFraude seuils;
    private final ControleFraudeCasUsage controle;

    public ExecuterVirementCasUsage(Comptes comptes, Operations operations, DepotVirements virements,
            EvenementsSortants evenements, JournalAudit audit, Transaction transaction, Horloge horloge,
            SeuilsFraude seuils, ControleFraudeCasUsage controle) {
        this.comptes = comptes;
        this.operations = operations;
        this.virements = virements;
        this.evenements = evenements;
        this.audit = audit;
        this.transaction = transaction;
        this.horloge = horloge;
        this.seuils = seuils;
        this.controle = controle;
    }

    @Override
    public Resultat executer(Commande commande) {
        if (commande.cleIdempotence() != null) {
            var premier = virements.parCle(commande.clientId(), commande.cleIdempotence());
            if (premier.isPresent()) {
                return resultat(premier.get());
            }
        }
        BigDecimal montant = Virement.verifierSaisie(commande.compteSourceId(), commande.compteDestinataireId(),
                commande.montant(), commande.motif());
        Instant soumisLe = horloge.instant();
        long virementId = virements.attribuerIdentifiant();
        Ecriture ecriture;
        try {
            ecriture = transaction.executer(() -> ecrire(virementId, commande, montant, soumisLe));
        } catch (ErreurVirement refus) {
            if (refus.motif() == Cause.CLE_IDEMPOTENCE_DEJA_UTILISEE) {
                return resultat(virements.parCle(commande.clientId(), commande.cleIdempotence()).orElseThrow());
            }
            throw refus;
        } catch (RuntimeException echec) {
            auditer(commande, virementId, soumisLe, List.of(new Entree(ActionAudit.VIREMENT,
                    TextesVirement.detailVirement(montant.toPlainString(), commande.motif(), ISSUE_ECHEC_REMISE))));
            throw new ErreurVirement(Cause.REMISE_IMPOSSIBLE, echec);
        }
        evenements.publier(ecriture.ligneDecision());
        evenements.publier(ecriture.ligneRemise());
        String montantTexte = montant.toPlainString();
        auditer(commande, virementId, soumisLe, List.of(
                new Entree(ActionAudit.VIREMENT,
                        TextesVirement.detailVirement(montantTexte, commande.motif(), ISSUE_EXECUTE)),
                new Entree(ActionAudit.TRANSFER_EXECUTED,
                        TextesVirement.detailExecution(montantTexte, commande.motif()))));
        return resultat(ecriture.virement());
    }

    private Ecriture ecrire(long virementId, Commande commande, BigDecimal montant, Instant soumisLe) {
        long sourceId = commande.compteSourceId();
        long destinataireId = commande.compteDestinataireId();
        List<Compte> verrouilles = comptes.verrouiller(sourceId, destinataireId);
        Compte source = trouver(verrouilles, sourceId);
        Compte destinataire = trouver(verrouilles, destinataireId);
        Virement.Execution execution = Virement.executer(virementId, commande.clientId(), source, destinataire,
                montant, commande.motif(), seuils, soumisLe);
        Virement virement = execution.virement();
        Evaluation evaluation = controle.evaluer(commande.clientId(), montant, destinataire.numero(), soumisLe);
        virements.enregistrer(virement, commande.cleIdempotence());
        controle.enregistrerAlertes(evaluation, commande.clientId(), virementId, soumisLe);
        if (!comptes.debiter(sourceId, montant)) {
            throw new ErreurVirement(Cause.SOLDE_INSUFFISANT);
        }
        comptes.crediter(destinataireId, montant);
        operations.enregistrer(execution.debit());
        operations.enregistrer(execution.credit());
        // Une seule notification par virement, à la décision : jamais à la remise asynchrone.
        long decision = evenements.consigner(new VirementDecide(UUID.randomUUID(), virementId, commande.clientId(),
                virement.etat(), NotificationVirement.pourExecution(virement, destinataire.numero(), seuils),
                soumisLe));
        long remise = evenements.consigner(new VirementATraiter(UUID.randomUUID(), virementId, montant,
                commande.clientId(), destinataireId, virement.motif(), soumisLe));
        return new Ecriture(virement, decision, remise);
    }

    private static Compte trouver(List<Compte> comptes, long id) {
        return comptes.stream().filter(compte -> compte.id() == id).findFirst()
                .orElseThrow(() -> new ErreurVirement(Cause.COMPTE_INTROUVABLE));
    }

    /** Écrit les entrées d'audit dans une transaction indépendante, dans l'ordre, puis les publie. */
    private void auditer(Commande commande, long virementId, Instant horodatage, List<Entree> entrees) {
        try {
            List<Long> lignes = transaction.executerIndependamment(() -> entrees.stream()
                    .map(entree -> audit.consigner(new EntreeAudit(UUID.randomUUID(),
                            Long.toString(commande.clientId()), entree.action(), commande.adresseIp(), horodatage,
                            entree.detail(), virementId)))
                    .toList());
            lignes.forEach(audit::publier);
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Entrée d'audit du virement " + virementId + " non écrite", echec);
        }
    }

    private static Resultat resultat(Virement virement) {
        return new Resultat(virement.id(), virement.etat(), virement.statut(), virement.compteSourceId(),
                virement.compteDestinataireId(), virement.montant(), virement.motif());
    }

    private record Entree(ActionAudit action, String detail) {
    }

    private record Ecriture(Virement virement, long ligneDecision, long ligneRemise) {
    }
}
