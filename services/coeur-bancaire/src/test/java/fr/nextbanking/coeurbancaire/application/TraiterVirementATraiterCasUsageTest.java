package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter.Issue;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-023-01")
class TraiterVirementATraiterCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final TraiterVirementATraiterCasUsage cas =
            new TraiterVirementATraiterCasUsage(memoire, memoire, memoire, memoire, memoire, memoire, memoire, memoire);

    @Test
    @Tag("FR-023-03")
    void un_virement_deja_execute_n_est_pas_debite_a_nouveau_et_n_emet_rien() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("2030.00")));
        memoire.virements.put(12L, new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("100.00"), Motif.lire("loyer"),
                EtatVirement.EXECUTE, StatutAffichage.SUCCESS, MemoireCoeurBancaire.MAINTENANT, null, null));

        Issue issue = cas.traiter(UUID.randomUUID(), 12);

        assertThat(issue).isEqualTo(Issue.DEJA_EXECUTE);
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("2030.00");
        assertThat(memoire.journal).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void un_message_livre_deux_fois_n_a_qu_un_effet() {
        UUID id = UUID.randomUUID();

        assertThat(cas.traiter(id, 12)).isEqualTo(Issue.VIREMENT_INCONNU);
        assertThat(cas.traiter(id, 12)).isEqualTo(Issue.DEJA_TRAITE);
    }

    @Test
    void refuse_un_identifiant_d_evenement_absent() {
        assertThatThrownBy(() -> cas.traiter(null, 12)).isInstanceOf(IllegalArgumentException.class);
    }

    private Virement libere(String montant) {
        MemoireCoeurBancaire m = memoire;
        m.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00")));
        m.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("100.00")));
        Virement virement = new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire(montant), Motif.lire("loyer"), EtatVirement.LIBERE,
                StatutAffichage.PENDING, RevueTestes.SOUMIS, RevueTestes.BUTOIR, "motif");
        m.virements.put(12L, virement);
        m.maintenant = RevueTestes.BUTOIR.minusSeconds(60);
        return virement;
    }

    @Test
    @Tag("FR-027-16")
    @Tag("FR-026-03")
    void un_virement_libere_est_debite_credite_inscrit_au_releve_et_passe_a_execute_sans_notification() {
        libere("12000.00");

        Issue issue = cas.traiter(UUID.randomUUID(), 12);

        assertThat(issue).isEqualTo(Issue.EXECUTE_APRES_LIBERATION);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("8000.00");
        assertThat(memoire.comptes.get(2L).solde().texte()).isEqualTo("12100.00");
        assertThat(memoire.operations).extracting(o -> o.type().name(), o -> o.description(), o -> o.date())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("DEBIT",
                                "[EN ATTENTE] Virement vers FR76-0001-0002 - loyer", memoire.maintenant),
                        org.assertj.core.groups.Tuple.tuple("CREDIT",
                                "[EN ATTENTE] Virement depuis FR76-0001-0001 - loyer", memoire.maintenant));
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action().name()).isEqualTo("TRANSFER_EXECUTED");
            assertThat(entree.detail()).isEqualTo("Montant: 12000.00 - Motif: loyer");
            assertThat(entree.utilisateur()).isEqualTo("7");
            assertThat(entree.adresseIp()).isNull();
        });
    }

    @Test
    @Tag("FR-027-16")
    @Tag("FR-026-03")
    void un_virement_libere_dont_le_solde_est_devenu_insuffisant_est_rejete_sans_debit_ni_credit_ni_notification() {
        libere("12000.00");
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("11999.99")));

        Issue issue = cas.traiter(UUID.randomUUID(), 12);

        assertThat(issue).isEqualTo(Issue.REJETE_SOLDE_INSUFFISANT);
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.REJETE);
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("11999.99");
        assertThat(memoire.comptes.get(2L).solde().texte()).isEqualTo("100.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.decisionsRevue.get(12L).issue())
                .isEqualTo(fr.nextbanking.coeurbancaire.domain.IssueRevue.REJETE_SOLDE_AU_DEBIT);
        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action().name()).isEqualTo("TRANSFER_REJECT_INSUFFICIENT_FUNDS");
            assertThat(entree.detail()).isEqualTo("Rejet virement id=12 - Motif: solde insuffisant au debit");
            assertThat(entree.utilisateur()).isEqualTo("SYSTEM");
            assertThat(entree.adresseIp()).isNull();
        });
    }

    @Test
    void un_virement_qui_n_est_ni_libere_ni_execute_est_ignore_sans_effet() {
        libere("12000.00");
        memoire.virements.put(12L, new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("12000.00"), Motif.AUCUN, EtatVirement.SUSPENDU,
                StatutAffichage.PENDING, RevueTestes.SOUMIS, RevueTestes.BUTOIR, "motif"));

        assertThat(cas.traiter(UUID.randomUUID(), 12)).isEqualTo(Issue.ETAT_INATTENDU);

        assertThat(memoire.journal).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void une_transition_perdue_annule_le_debit_et_le_marquage_pour_que_le_message_soit_rejoue() {
        libere("12000.00");
        memoire.transitionsPerdues.add(12L);
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> cas.traiter(id, 12)).isInstanceOf(fr.nextbanking.coeurbancaire.domain.ErreurVirement.class);

        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("20000.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.traites).doesNotContain(id);
        memoire.transitionsPerdues.clear();
        assertThat(cas.traiter(id, 12)).isEqualTo(Issue.EXECUTE_APRES_LIBERATION);
    }

    @Test
    void une_transition_perdue_au_rejet_pour_solde_annule_aussi() {
        libere("12000.00");
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("1.00")));
        memoire.transitionsPerdues.add(12L);

        assertThatThrownBy(() -> cas.traiter(UUID.randomUUID(), 12))
                .isInstanceOf(fr.nextbanking.coeurbancaire.domain.ErreurVirement.class);
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void un_compte_disparu_est_signale_comme_introuvable() {
        libere("12000.00");
        memoire.comptes.remove(1L);

        assertThatThrownBy(() -> cas.traiter(UUID.randomUUID(), 12))
                .isInstanceOf(fr.nextbanking.coeurbancaire.domain.ErreurVirement.class);
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00")));
        memoire.comptes.remove(2L);
        assertThatThrownBy(() -> cas.traiter(UUID.randomUUID(), 12))
                .isInstanceOf(fr.nextbanking.coeurbancaire.domain.ErreurVirement.class);
    }
}
