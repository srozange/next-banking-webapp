package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.application.port.in.IssueDecision;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Libération d'un virement suspendu par l'administrateur (FR-016-19, FR-027-16, FR-027-17) : rôle et motif, heure
 * butoir qui fait foi, solde contrôlé à l'instant de la commande, remise dans la même transaction, alertes OPEN passées
 * à REVIEWED, audit, aucune notification.
 */
@Tag("FR-016-19")
@Tag("FR-027-16")
@Tag("FR-027-17")
@Tag("FR-023-01")
@Tag("FR-026-02")
class LiberationVirementCasUsageTest {

    private final MemoireCoeurBancaire memoire = RevueTestes.memoire();
    private LiberationVirementCasUsage cas;

    @BeforeEach
    void preparer() {
        RevueTestes.suspendu(memoire);
        memoire.maintenant = RevueTestes.BUTOIR.minusSeconds(1);
        cas = new LiberationVirementCasUsage(memoire, memoire, memoire, memoire, memoire, memoire, memoire);
    }

    private static CommandeDecision commande(String motif) {
        return new CommandeDecision(99, RevueTestes.ADMIN, 12, motif, RevueTestes.IP);
    }

    private ErreurVirement refus(CommandeDecision commande) {
        return catchThrowableOfType(ErreurVirement.class, () -> cas.liberer(commande));
    }

    private void inchange() {
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
        assertThat(memoire.decisionsRevue).isEmpty();
        assertThat(memoire.journal).doesNotContain("debiter", "crediter");
    }

    @Test
    void une_libération_a_une_seconde_du_butoir_passe_le_virement_a_libere_et_le_remet_sans_debit() {
        IssueDecision issue = cas.liberer(commande("  vérifié par téléphone "));

        assertThat(issue).isEqualTo(IssueDecision.ACCEPTEE);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.LIBERE);
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("20000.00");
        assertThat(memoire.comptes.get(2L).solde()).isEqualByComparingTo("100.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.outbox).singleElement().satisfies(remise -> {
            assertThat(remise.virementId()).isEqualTo(12);
            assertThat(remise.montant()).isEqualByComparingTo("12000.00");
            assertThat(remise.emetteurId()).isEqualTo(7);
            assertThat(remise.compteDestinataireId()).isEqualTo(2);
        });
        assertThat(memoire.publies).hasSize(1);
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.decisionsRevue.get(12L).issue()).isEqualTo(IssueRevue.LIBERE);
        assertThat(memoire.decisionsRevue.get(12L).decideur()).isEqualTo("99");
        assertThat(memoire.decisionsRevue.get(12L).motif()).isEqualTo("vérifié par téléphone");
    }

    @Test
    void les_alertes_open_passent_a_reviewed_et_la_confirmee_reste_confirmee() {
        cas.liberer(commande("vérifié par téléphone"));

        assertThat(memoire.alertes).extracting(a -> a.statut()).containsExactly(StatutAlerte.REVIEWED,
                StatutAlerte.REVIEWED, StatutAlerte.CONFIRMED);
    }

    @Test
    void l_audit_est_admin_transfer_release_issue_execute_avec_l_identite_et_l_adresse_ip() {
        cas.liberer(commande("vérifié par téléphone"));

        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_TRANSFER_RELEASE);
            assertThat(entree.detail())
                    .isEqualTo("Liberation virement id=12 - Motif: vérifié par téléphone - Issue: EXECUTE");
            assertThat(entree.utilisateur()).isEqualTo("99");
            assertThat(entree.adresseIp()).isEqualTo(RevueTestes.IP);
            assertThat(entree.virementId()).isEqualTo(12);
        });
    }

    @Test
    void un_solde_devenu_insuffisant_refuse_la_libération_et_rejette_le_virement_sans_debit() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("11999.99")));

        IssueDecision issue = cas.liberer(commande("vérifié"));

        assertThat(issue).isEqualTo(IssueDecision.REFUSEE_SOLDE_INSUFFISANT);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.REJETE);
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("11999.99");
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_TRANSFER_RELEASE);
            assertThat(entree.detail()).endsWith("Issue: REJETE");
        });
        assertThat(memoire.decisionsRevue.get(12L).issue()).isEqualTo(IssueRevue.REJETE);
    }

    @Test
    void un_solde_exactement_egal_au_montant_suffit() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("12000.00")));

        assertThat(cas.liberer(commande("vérifié"))).isEqualTo(IssueDecision.ACCEPTEE);
    }

    @Test
    void a_l_heure_butoir_exacte_la_libération_est_refusee_et_le_virement_rejete_sans_debit() {
        memoire.maintenant = RevueTestes.BUTOIR;

        IssueDecision issue = cas.liberer(commande("vérifié"));

        assertThat(issue).isEqualTo(IssueDecision.REFUSEE_HEURE_BUTOIR);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.REJETE);
        assertThat(memoire.decisionsRevue.get(12L).issue()).isEqualTo(IssueRevue.REJETE_HEURE_BUTOIR);
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.journal).doesNotContain("debiter", "crediter", "verrouiller");
        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_TRANSFER_RELEASE);
            assertThat(entree.detail()).endsWith("Issue: REJETE");
        });
    }

    @Test
    void une_seconde_apres_l_heure_butoir_la_libération_est_refusee_aussi() {
        memoire.maintenant = RevueTestes.BUTOIR.plusSeconds(1);

        assertThat(cas.liberer(commande("vérifié"))).isEqualTo(IssueDecision.REFUSEE_HEURE_BUTOIR);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.REJETE);
    }

    @Test
    void un_motif_vide_ou_d_espaces_est_refuse_et_le_virement_ne_change_pas() {
        for (String motif : new String[] {null, "", "   "}) {
            assertThat(refus(commande(motif)).motif()).isEqualTo(Cause.MOTIF_DECISION_OBLIGATOIRE);
        }
        assertThat(refus(commande(" ")).getMessage()).isEqualTo("Veuillez indiquer un motif");
        inchange();
    }

    @Test
    void un_client_ou_un_conseiller_est_refuse_meme_sans_motif_et_le_virement_ne_change_pas() {
        for (Role role : new Role[] {Role.CLIENT, Role.CONSEILLER}) {
            CommandeDecision commande = new CommandeDecision(7, Set.of(role), 12, "vérifié", RevueTestes.IP);
            assertThat(refus(commande).motif()).isEqualTo(Cause.ROLE_INSUFFISANT);
        }
        assertThat(refus(new CommandeDecision(7, Set.of(), 12, null, null)).motif())
                .isEqualTo(Cause.ROLE_INSUFFISANT);
        inchange();
    }

    @Test
    void un_virement_inconnu_ou_qui_n_est_plus_suspendu_n_est_pas_libérable() {
        assertThat(refus(new CommandeDecision(99, RevueTestes.ADMIN, 404, "vérifié", null)).motif())
                .isEqualTo(Cause.VIREMENT_NON_SUSPENDU);
        cas.liberer(commande("vérifié"));

        assertThat(refus(commande("encore")).motif()).isEqualTo(Cause.VIREMENT_NON_SUSPENDU);
        assertThat(memoire.outbox).hasSize(1);
    }

    @Test
    void une_transition_perdue_contre_un_autre_traitement_annule_tout_et_ne_double_pas_l_audit() {
        memoire.transitionsPerdues.add(12L);

        assertThat(refus(commande("vérifié")).motif()).isEqualTo(Cause.VIREMENT_NON_SUSPENDU);

        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
        assertThat(memoire.alertes).extracting(a -> a.statut()).containsExactly(StatutAlerte.OPEN,
                StatutAlerte.OPEN, StatutAlerte.CONFIRMED);
    }

    @Test
    void une_transition_perdue_a_l_heure_butoir_laisse_la_decision_a_l_autre_traitement() {
        memoire.maintenant = RevueTestes.BUTOIR;
        memoire.transitionsPerdues.add(12L);

        assertThat(refus(commande("vérifié")).motif()).isEqualTo(Cause.VIREMENT_NON_SUSPENDU);
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void une_remise_impossible_fait_echouer_la_libération_et_le_virement_reste_suspendu() {
        memoire.outboxRefusee = true;

        ErreurVirement erreur = refus(commande("vérifié"));

        assertThat(erreur.motif()).isEqualTo(Cause.REMISE_IMPOSSIBLE);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.decisionsRevue).isEmpty();
        assertThat(memoire.alertes).extracting(a -> a.statut()).containsExactly(StatutAlerte.OPEN,
                StatutAlerte.OPEN, StatutAlerte.CONFIRMED);
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void un_compte_source_disparu_est_signale_comme_introuvable() {
        memoire.comptes.remove(1L);

        assertThat(refus(commande("vérifié")).motif()).isEqualTo(Cause.COMPTE_INTROUVABLE);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
    }
}
