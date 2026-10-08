package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import fr.nextbanking.coeurbancaire.domain.Virement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Rejet automatique à l'échéance (FR-027-17, NFR-BATCH-01, NFR-BATCH-02) : virement soumis le 5 à 10:15 ; traitement le
 * 6 à 10:14:59 (rien), 10:15:00 (rejet) et 10:15:01. Sans débit ni notification ; audit TRANSFER_AUTO_REJECT par SYSTEM ;
 * une ligne SUSPENDED_TRANSFERS_EXPIRY seulement si un virement est rejeté ou si le traitement échoue.
 */
@Tag("FR-027-17")
@Tag("FR-027-16")
@Tag("FR-026-02")
class EcheanceRevueCasUsageTest {

    private final MemoireCoeurBancaire memoire = RevueTestes.memoire();
    private EcheanceRevueCasUsage cas;

    @BeforeEach
    void preparer() {
        RevueTestes.suspendu(memoire);
        cas = new EcheanceRevueCasUsage(memoire, memoire, memoire, memoire, memoire);
    }

    @Test
    void a_23_h_59_min_59_s_le_virement_est_encore_suspendu_et_rien_n_est_journalise() {
        memoire.maintenant = RevueTestes.BUTOIR.minusSeconds(1);

        assertThat(cas.rejeterLesEchus()).isZero();

        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.lignesBatch).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void a_l_heure_butoir_exacte_le_virement_est_rejete_sans_debit_ni_notification_avec_son_audit() {
        memoire.maintenant = RevueTestes.BUTOIR;

        assertThat(cas.rejeterLesEchus()).isEqualTo(1);

        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.REJETE);
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("20000.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.decisionsRevue.get(12L).issue()).isEqualTo(IssueRevue.REJETE_HEURE_BUTOIR);
        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.TRANSFER_AUTO_REJECT);
            assertThat(entree.detail()).isEqualTo("Rejet automatique virement id=12");
            assertThat(entree.utilisateur()).isEqualTo("SYSTEM");
            assertThat(entree.adresseIp()).isNull();
        });
        assertThat(memoire.lignesBatch).singleElement().asString()
                .startsWith("SUSPENDED_TRANSFERS_EXPIRY|1|SUCCESS|");
        assertThat(memoire.alertes).extracting(a -> a.statut()).containsExactly(StatutAlerte.OPEN,
                StatutAlerte.OPEN, StatutAlerte.CONFIRMED);
    }

    @Test
    void une_seconde_plus_tard_le_virement_est_rejete_une_seule_fois() {
        memoire.maintenant = RevueTestes.BUTOIR.plusSeconds(1);

        assertThat(cas.rejeterLesEchus()).isEqualTo(1);
        assertThat(cas.rejeterLesEchus()).isZero();

        assertThat(memoire.outboxAudit).hasSize(1);
        assertThat(memoire.lignesBatch).hasSize(1);
    }

    @Test
    void les_virements_echus_sont_rejetes_du_plus_ancien_au_plus_recent_et_les_autres_etats_sont_ignores() {
        memoire.virements.put(13L, new Virement(13, 7, new CompteId(1), new CompteId(2), Montant.lire("50.00"), Motif.AUCUN, EtatVirement.SUSPENDU,
                StatutAffichage.SUCCESS, RevueTestes.SOUMIS.minusSeconds(60), RevueTestes.BUTOIR.minusSeconds(60),
                "motif"));
        memoire.virements.put(14L, new Virement(14, 7, new CompteId(1), new CompteId(2), Montant.lire("50.00"), Motif.AUCUN, EtatVirement.EXECUTE,
                StatutAffichage.SUCCESS, RevueTestes.SOUMIS.minusSeconds(60), null, null));
        memoire.maintenant = RevueTestes.BUTOIR;

        assertThat(cas.rejeterLesEchus()).isEqualTo(2);

        assertThat(memoire.outboxAudit).extracting(e -> e.virementId()).containsExactly(13L, 12L);
        assertThat(memoire.virements.get(14L).etat()).isEqualTo(EtatVirement.EXECUTE);
    }

    @Test
    void une_transition_perdue_contre_une_libération_ne_rejette_ni_n_audite() {
        memoire.maintenant = RevueTestes.BUTOIR;
        memoire.transitionsPerdues.add(12L);

        assertThat(cas.rejeterLesEchus()).isZero();

        assertThat(memoire.outboxAudit).isEmpty();
        assertThat(memoire.lignesBatch).isEmpty();
    }

    @Test
    void un_echec_de_rejet_est_journalise_failed_puis_partial_quand_un_autre_virement_est_rejete() {
        memoire.maintenant = RevueTestes.BUTOIR;
        memoire.transitionEnErreur = true;

        assertThat(cas.rejeterLesEchus()).isZero();
        assertThat(memoire.lignesBatch).singleElement().asString().startsWith("SUSPENDED_TRANSFERS_EXPIRY|0|FAILED|");

        memoire.lignesBatch.clear();
        memoire.transitionEnErreur = false;
        memoire.virements.put(13L, new Virement(13, 7, new CompteId(1), new CompteId(2), Montant.lire("50.00"), Motif.AUCUN, EtatVirement.SUSPENDU,
                StatutAffichage.SUCCESS, RevueTestes.SOUMIS.minusSeconds(60), RevueTestes.BUTOIR.minusSeconds(60),
                "motif"));
        MemoireCoeurBancaire partiel = new MemoireCoeurBancaire() {
            @Override
            public boolean transiter(long id, EtatVirement de, EtatVirement vers,
                    fr.nextbanking.coeurbancaire.domain.DecisionRevue decision) {
                if (id == 12) {
                    throw new IllegalStateException("panne");
                }
                return super.transiter(id, de, vers, decision);
            }
        };
        partiel.comptes.putAll(memoire.comptes);
        partiel.virements.putAll(memoire.virements);
        partiel.maintenant = RevueTestes.BUTOIR;
        EcheanceRevueCasUsage cas2 = new EcheanceRevueCasUsage(partiel, partiel, partiel, partiel, partiel);

        assertThat(cas2.rejeterLesEchus()).isEqualTo(1);
        assertThat(partiel.lignesBatch).singleElement().asString().startsWith("SUSPENDED_TRANSFERS_EXPIRY|1|PARTIAL|");
    }

    @Test
    void une_lecture_en_erreur_est_journalisee_failed_et_un_journal_en_panne_ne_fait_pas_echouer_le_traitement() {
        memoire.lectureEcheancesEnErreur = true;

        assertThat(cas.rejeterLesEchus()).isZero();
        assertThat(memoire.lignesBatch).singleElement().asString().startsWith("SUSPENDED_TRANSFERS_EXPIRY|0|FAILED|");

        memoire.batchRefuse = true;
        assertThat(cas.rejeterLesEchus()).isZero();
    }
}
