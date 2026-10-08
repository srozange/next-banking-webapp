package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Resultat;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.TypeNotification;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Suspension avant tout débit (FR-027-04, FR-023-04) : score strictement supérieur à 75, ou montant au moins égal au
 * seuil de fraude. Horloge injectée : soumission le 5 novembre à 10:15 Paris.
 */
@Tag("FR-027-04")
@Tag("FR-027-02")
@Tag("FR-003-09")
@Tag("FR-003-13")
@Tag("FR-003-15")
@Tag("FR-003-16")
@Tag("FR-023-04")
@Tag("FR-026-02")
@Tag("BUG-076")
class ControleFraudeSuspensionTest {

    private final MemoireCoeurBancaire memoire = RevueTestes.memoire();
    private ExecuterVirementCasUsage cas;

    @BeforeEach
    void preparer() {
        memoire.maintenant = RevueTestes.SOUMIS;
        cas = ExecuterVirementCasUsageTest.assembler(memoire);
    }

    private Resultat soumettre(String montant) {
        return cas.executer(new Commande(7, 1L, 2L, montant, "loyer", null, RevueTestes.IP));
    }

    private void aucuneEcritureFinanciere() {
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("20000.00");
        assertThat(memoire.comptes.get(2L).solde().texte()).isEqualTo("100.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.journal).doesNotContain("debiter", "crediter");
    }

    @Test
    void un_virement_de_10000_euros_est_suspendu_quel_que_soit_son_score_avec_un_butoir_a_24_heures() {
        memoire.beneficiairesActifs.put(7L, Set.of("FR76-0001-0002"));

        Resultat resultat = soumettre("10000.00");

        assertThat(resultat.etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(resultat.heureButoir()).isEqualTo(RevueTestes.BUTOIR);
        aucuneEcritureFinanciere();
        assertThat(memoire.virements.get(12L).motifSuspension())
                .isEqualTo("Montant supérieur ou égal au seuil anti-fraude (10000 EUR)");
        assertThat(memoire.alertes).isEmpty();
    }

    @Test
    void un_score_de_76_ou_plus_suspend_avec_une_alerte_open_par_signal_et_le_motif_en_clair() {
        memoire.comptes.put(1L, memoire.comptes.get(1L));
        soumettre("5000.50");

        Resultat resultat = soumettre("5000.50");

        assertThat(resultat.etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.alertes).hasSize(3).allSatisfy(a -> assertThat(a.virementId()).isEqualTo(13));
        assertThat(memoire.virements.get(13L).motifSuspension()).startsWith("Score cumulé 130 - signaux : ");
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("14999.50");
    }

    @Test
    void la_notification_unique_est_l_avis_de_suspension_sans_fraude_score_ni_signal() {
        soumettre("10000.00");

        assertThat(memoire.decisions).singleElement().satisfies(decision -> {
            assertThat(decision.etat()).isEqualTo(EtatVirement.SUSPENDU);
            assertThat(decision.notification().type()).isEqualTo(TypeNotification.FRAUDE);
            assertThat(decision.notification().message()).isEqualTo(
                    "Votre virement de 10000.00 EUR vers le compte FR76-0001-0002 est en cours de vérification. "
                            + "Rien n'a été débité. Vous aurez notre réponse avant le 06/11/2026 à 10:15. "
                            + "Sans décision à cette heure, il sera rejeté sans aucun débit.");
            assertThat(decision.notification().message()).doesNotContainIgnoringCase("fraude")
                    .doesNotContainIgnoringCase("score");
        });
        assertThat(memoire.decisionsPubliees).hasSize(1);
        assertThat(memoire.publies).isEmpty();
    }

    @Test
    @Tag("FR-026-03")
    void l_audit_contient_la_seule_entree_virement_issue_suspendu() {
        soumettre("10000.00");

        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.VIREMENT);
            assertThat(entree.detail()).isEqualTo("Montant: 10000.00 - Motif: loyer - Issue: SUSPENDU");
            assertThat(entree.utilisateur()).isEqualTo("7");
            assertThat(entree.adresseIp()).isEqualTo(RevueTestes.IP);
        });
    }

    @Test
    void un_virement_de_score_75_et_de_9999_99_euros_est_execute_avec_une_alerte_et_deux_entrees_d_audit() {
        Resultat resultat = soumettre("9999.99");

        assertThat(resultat.etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(resultat.heureButoir()).isNull();
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("10000.01");
        assertThat(memoire.decisions.getFirst().notification().type()).isEqualTo(TypeNotification.ALERTE);
        assertThat(memoire.outboxAudit).extracting(e -> e.action(), e -> e.detail()).containsExactly(
                org.assertj.core.groups.Tuple.tuple(ActionAudit.VIREMENT,
                        "Montant: 9999.99 - Motif: loyer - Issue: EXECUTE"),
                org.assertj.core.groups.Tuple.tuple(ActionAudit.TRANSFER_EXECUTED,
                        "Montant: 9999.99 - Motif: loyer"));
    }

    @Test
    void la_remise_du_virement_suspendu_n_existe_pas_et_une_ecriture_de_decision_impossible_l_annule() {
        memoire.outboxRefusee = true;

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> soumettre("10000.00"))
                .hasMessage("Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard");

        assertThat(memoire.virements).isEmpty();
        aucuneEcritureFinanciere();
    }

    @Test
    void la_meme_cle_d_idempotence_rend_le_virement_suspendu_et_son_butoir() {
        Resultat premier = cas.executer(new Commande(7, 1L, 2L, "10000.00", "loyer", "cle-s", RevueTestes.IP));

        Resultat second = cas.executer(new Commande(7, 1L, 2L, "10000.00", "loyer", "cle-s", RevueTestes.IP));

        assertThat(second).isEqualTo(premier);
        assertThat(second.etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(second.heureButoir()).isEqualTo(RevueTestes.BUTOIR);
        assertThat(memoire.virements).hasSize(1);
        assertThat(memoire.decisions).hasSize(1);
        assertThat(new BigDecimal("10000.00")).isEqualByComparingTo(second.montant());
    }
}
