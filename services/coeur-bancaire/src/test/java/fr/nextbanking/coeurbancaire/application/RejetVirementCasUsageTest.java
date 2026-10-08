package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Motif;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.application.port.in.IssueDecision;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Rejet d'un virement suspendu par l'administrateur (FR-016-19, FR-027-16) : sans débit ni notification, alertes OPEN
 * passées à REVIEWED, audit ADMIN_TRANSFER_REJECT ; à l'heure butoir, le rejet est celui de l'échéance.
 */
@Tag("FR-016-19")
@Tag("FR-027-16")
@Tag("FR-026-02")
class RejetVirementCasUsageTest {

    private final MemoireCoeurBancaire memoire = RevueTestes.memoire();
    private RejetVirementCasUsage cas;

    @BeforeEach
    void preparer() {
        RevueTestes.suspendu(memoire);
        memoire.maintenant = RevueTestes.BUTOIR.minusSeconds(1);
        cas = new RejetVirementCasUsage(memoire, memoire, memoire, memoire, memoire);
    }

    private static CommandeDecision commande(String motif) {
        return new CommandeDecision(99, RevueTestes.ADMIN, 12, motif, RevueTestes.IP);
    }

    private ErreurVirement refus(CommandeDecision commande) {
        return catchThrowableOfType(ErreurVirement.class, () -> cas.rejeter(commande));
    }

    @Test
    void un_rejet_avant_le_butoir_passe_le_virement_a_rejete_sans_debit_ni_notification() {
        IssueDecision issue = cas.rejeter(commande("client injoignable"));

        assertThat(issue).isEqualTo(IssueDecision.ACCEPTEE);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.REJETE);
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("20000.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.decisionsRevue.get(12L).issue()).isEqualTo(IssueRevue.REJETE);
        assertThat(memoire.decisionsRevue.get(12L).decideur()).isEqualTo("99");
    }

    @Test
    void les_alertes_open_passent_a_reviewed() {
        cas.rejeter(commande("client injoignable"));

        assertThat(memoire.alertes).extracting(a -> a.statut()).containsExactly(StatutAlerte.REVIEWED,
                StatutAlerte.REVIEWED, StatutAlerte.CONFIRMED);
    }

    @Test
    void l_audit_est_admin_transfer_reject_avec_le_motif_l_identite_et_l_adresse_ip() {
        cas.rejeter(commande("client injoignable"));

        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_TRANSFER_REJECT);
            assertThat(entree.detail()).isEqualTo("Rejet virement id=12 - Motif: client injoignable");
            assertThat(entree.utilisateur()).isEqualTo("99");
            assertThat(entree.adresseIp()).isEqualTo(RevueTestes.IP);
        });
    }

    @Test
    void a_l_heure_butoir_le_rejet_est_sans_objet_c_est_le_rejet_de_l_echeance_par_le_systeme() {
        memoire.maintenant = RevueTestes.BUTOIR;

        IssueDecision issue = cas.rejeter(commande("client injoignable"));

        assertThat(issue).isEqualTo(IssueDecision.REFUSEE_HEURE_BUTOIR);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.REJETE);
        assertThat(memoire.decisionsRevue.get(12L).issue()).isEqualTo(IssueRevue.REJETE_HEURE_BUTOIR);
        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.TRANSFER_AUTO_REJECT);
            assertThat(entree.utilisateur()).isEqualTo("SYSTEM");
            assertThat(entree.adresseIp()).isNull();
        });
    }

    @Test
    void un_rejet_perdu_contre_une_autre_transition_n_ecrit_ni_changement_ni_audit() {
        memoire.transitionsPerdues.add(12L);
        assertThat(refus(commande("client injoignable")).motif()).isEqualTo(Cause.VIREMENT_NON_SUSPENDU);

        memoire.maintenant = RevueTestes.BUTOIR;
        assertThat(refus(commande("client injoignable")).motif()).isEqualTo(Cause.VIREMENT_NON_SUSPENDU);

        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.outboxAudit).isEmpty();
        assertThat(memoire.alertes).extracting(a -> a.statut()).containsExactly(StatutAlerte.OPEN,
                StatutAlerte.OPEN, StatutAlerte.CONFIRMED);
    }

    @Test
    void un_motif_absent_un_role_insuffisant_ou_un_virement_non_suspendu_sont_refuses_sans_effet() {
        assertThat(refus(commande(" ")).motif()).isEqualTo(Cause.MOTIF_DECISION_OBLIGATOIRE);
        assertThat(refus(new CommandeDecision(5, Set.of(Role.CLIENT), 12, "x", null)).motif())
                .isEqualTo(Cause.ROLE_INSUFFISANT);
        assertThat(refus(new CommandeDecision(5, Set.of(Role.CONSEILLER), 12, "x", null)).motif())
                .isEqualTo(Cause.ROLE_INSUFFISANT);
        assertThat(refus(new CommandeDecision(99, RevueTestes.ADMIN, 404, "x", null)).motif())
                .isEqualTo(Cause.VIREMENT_NON_SUSPENDU);

        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.outboxAudit).isEmpty();
        assertThat(memoire.decisionsRevue).isEmpty();
    }
}
