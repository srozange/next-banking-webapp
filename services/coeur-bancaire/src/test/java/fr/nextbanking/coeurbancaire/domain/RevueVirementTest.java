package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ControleFraude.Decision;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Décision de suspension, heure butoir, virement refusé, libéré puis débité, textes et modèles de la revue. */
@Tag("FR-027-04")
@Tag("FR-027-16")
@Tag("FR-027-17")
class RevueVirementTest {

    private static final Instant SOUMIS = Instant.parse("2026-11-05T09:15:00Z");
    private static final SeuilsFraude SEUILS =
            new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000"));
    private static final Compte SOURCE = new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00"));
    private static final Compte DESTINATAIRE = new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("100.00"));
    private static Soumission soumission(long id, String montant, String motif, Instant soumisLe) {
        return new Soumission(id, 7, DemandeVirement.lire(1L, 2L, montant, motif), soumisLe);
    }

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Test
    void un_score_strictement_superieur_a_75_suspend_avec_le_score_et_les_signaux() {
        Decision a75 = ControleFraudeOutils.evaluer("100.00", ControleFraudeOutils.FRANCE, false,
                new HistoriqueClient(0, null, 0), ControleFraudeOutils.DIX_HEURES)
                .decider(new BigDecimal("100.00"), new BigDecimal("10000"));
        assertThat(a75).isEqualTo(new Decision(EtatVirement.EXECUTE, null));

        Decision a130 = ControleFraudeOutils.evaluer("5000.50", ControleFraudeOutils.FRANCE, false,
                new HistoriqueClient(0, SOUMIS, 0), SOUMIS.plusSeconds(1))
                .decider(new BigDecimal("5000.50"), new BigDecimal("10000"));
        assertThat(a130.etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(a130.motifSuspension()).isEqualTo(
                "Score cumulé 130 - signaux : LARGE_AMOUNT, NEW_BENEFICIARY, RAPID_SUCCESSION");
    }

    @Test
    void un_score_de_75_exactement_n_est_pas_suspendu() {
        // 5000.50 : LARGE_AMOUNT 40 + NEW_BENEFICIARY 35 = 75.
        Decision decision = ControleFraudeOutils.evaluer("5000.50", ControleFraudeOutils.FRANCE, false,
                ControleFraudeOutils.VIDE, ControleFraudeOutils.DIX_HEURES)
                .decider(new BigDecimal("5000.50"), new BigDecimal("10000"));

        assertThat(decision.etat()).isEqualTo(EtatVirement.EXECUTE);
    }

    @Test
    void un_montant_au_moins_egal_au_seuil_de_fraude_suspend_quel_que_soit_le_score() {
        Decision decision = ControleFraudeOutils.evaluerMontant("10000.00")
                .decider(new BigDecimal("10000.00"), new BigDecimal("10000"));
        Decision dessous = ControleFraudeOutils.evaluerMontant("9999.99")
                .decider(new BigDecimal("9999.99"), new BigDecimal("10000"));

        assertThat(decision).isEqualTo(new Decision(EtatVirement.SUSPENDU,
                "Montant supérieur ou égal au seuil anti-fraude (10000 EUR)"));
        assertThat(dessous.etat()).isEqualTo(EtatVirement.EXECUTE);
    }

    @Test
    void le_virement_suspendu_a_une_heure_butoir_a_24_heures_et_le_virement_refuse_aucune() {
        Virement suspendu = Virement.suspendre(soumission(12, "10000.00", "loyer", SOUMIS), SEUILS, "motif");
        Virement refuse = Virement.refuser(soumission(13, "50.00", " ", SOUMIS), SEUILS);

        assertThat(suspendu.etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(suspendu.heureButoir()).isEqualTo(SOUMIS.plus(Duration.ofHours(24)))
                .isEqualTo(Instant.parse("2026-11-06T09:15:00Z"));
        assertThat(suspendu.motifSuspension()).isEqualTo("motif");
        assertThat(suspendu.statut()).isEqualTo(StatutAffichage.PENDING);
        assertThat(refuse.etat()).isEqualTo(EtatVirement.REFUSE_ERREUR_CONTROLE);
        assertThat(refuse.heureButoir()).isNull();
        assertThat(refuse.motif()).isEqualTo(Motif.AUCUN);
        assertThat(refuse.statut()).isEqualTo(StatutAffichage.SUCCESS);
    }

    @Test
    @Tag("FR-027-17")
    void l_heure_butoir_fait_foi_a_l_instant_exact_et_apres() {
        Virement suspendu = Virement.suspendre(soumission(12, "10000.00", null, SOUMIS), SEUILS, "motif");
        Instant butoir = suspendu.heureButoir();

        assertThat(suspendu.heureButoirAtteinte(butoir.minusSeconds(1))).isFalse();
        assertThat(suspendu.heureButoirAtteinte(butoir)).isTrue();
        assertThat(suspendu.heureButoirAtteinte(butoir.plusSeconds(1))).isTrue();
        Virement execute = Virement.executer(soumission(14, "100.00", null, SOUMIS), SOURCE, DESTINATAIRE, SEUILS).virement();
        assertThat(execute.heureButoirAtteinte(butoir.plusSeconds(1))).isFalse();
    }

    @Test
    void un_virement_libere_est_execute_avec_ses_deux_operations_datees_du_debit_et_au_statut_de_la_soumission() {
        Virement libere = new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("10000.00"), Motif.lire("loyer"), EtatVirement.LIBERE,
                StatutAffichage.PENDING, SOUMIS, SOUMIS.plusSeconds(86400), "motif");
        Instant debit = SOUMIS.plusSeconds(3600);

        Virement.Execution execution = libere.executerApresLiberation(SOURCE, DESTINATAIRE, debit);

        assertThat(execution.virement().etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(execution.debit().date()).isEqualTo(debit);
        assertThat(execution.debit().soldeApres().texte()).isEqualTo("10000.00");
        assertThat(execution.debit().description()).isEqualTo("[TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002 - loyer");
        assertThat(execution.credit().soldeApres().texte()).isEqualTo("10100.00");
        assertThat(execution.credit().date()).isEqualTo(debit);
    }

    @Test
    void les_controles_de_propriete_et_de_solde_precedent_le_controle_de_fraude() {
        assertThatThrownBy(() -> Virement.verifierComptes(8, SOURCE, DESTINATAIRE, Montant.lire("1.00")))
                .isInstanceOf(ErreurVirement.class).hasMessage("Accès refusé");
        assertThatThrownBy(() -> Virement.verifierComptes(7, SOURCE, DESTINATAIRE, Montant.lire("20000.01")))
                .hasMessage("Solde insuffisant sur le compte source");
        Virement.verifierComptes(7, SOURCE, DESTINATAIRE, Montant.lire("20000.00"));
    }

    @Test
    @Tag("FR-003-16")
    void l_avis_de_suspension_est_le_texte_exact_sans_fraude_score_ni_signal() {
        Virement suspendu = Virement.suspendre(soumission(12, "10000.00", null, Instant.parse("2026-11-05T09:15:00Z")), SEUILS, "Score cumulé 130 - signaux : LARGE_AMOUNT");

        NotificationVirement notification = NotificationVirement.pourSuspension(suspendu, "FR76-0001-0002", PARIS);

        assertThat(notification.type()).isEqualTo(TypeNotification.FRAUDE);
        assertThat(notification.clientId()).isEqualTo(7);
        assertThat(notification.titre()).isNull();
        assertThat(notification.message()).isEqualTo("Votre virement de 10000.00 EUR vers le compte FR76-0001-0002 "
                + "est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant le 06/11/2026 "
                + "à 10:15. Sans décision à cette heure, il sera rejeté sans aucun débit.");
        assertThat(notification.message()).doesNotContainIgnoringCase("fraude").doesNotContainIgnoringCase("score")
                .doesNotContain("LARGE_AMOUNT");
    }

    @Test
    void les_textes_de_la_revue_et_les_details_d_audit_suivent_les_sources() {
        assertThat(TextesVirement.formaterDate(SOUMIS, PARIS)).isEqualTo("05/11/2026");
        assertThat(TextesVirement.formaterHeure(SOUMIS, PARIS)).isEqualTo("10:15");
        assertThat(TextesVirement.motifSuspensionScore(80, List.of("A", "B"))).isEqualTo("Score cumulé 80 - signaux : A, B");
        assertThat(TextesVirement.detailLiberation(12, "vérifié par téléphone", "EXECUTE")).isEqualTo(
                "Liberation virement id=12 - Motif: vérifié par téléphone - Issue: EXECUTE");
        assertThat(TextesVirement.detailRejet(12, "client injoignable"))
                .isEqualTo("Rejet virement id=12 - Motif: client injoignable");
        assertThat(TextesVirement.detailRejetAutomatique(12)).isEqualTo("Rejet automatique virement id=12");
        assertThat(TextesVirement.detailRejetSoldeAuDebit(12))
                .isEqualTo("Rejet virement id=12 - Motif: solde insuffisant au debit");
        assertThat(TextesVirement.LIBERATION_REFUSEE_SOLDE)
                .isEqualTo("Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit.");
        assertThat(TextesVirement.HEURE_BUTOIR_DEPASSEE).isEqualTo("Heure butoir dépassée : le virement est rejeté.");
        assertThat(TextesVirement.MOTIF_DECISION_OBLIGATOIRE).isEqualTo("Veuillez indiquer un motif");
        assertThat(TextesVirement.detailsEcheance(2)).contains("2");
        assertThat(TextesVirement.detailsEcheanceEchec(1, 2)).contains("1").contains("2");
    }

    @Test
    void les_modeles_de_la_revue_exigent_leurs_champs() {
        assertThatThrownBy(() -> new DecisionRevue(null, "1", null, SOUMIS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecisionRevue(IssueRevue.LIBERE, null, null, SOUMIS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecisionRevue(IssueRevue.LIBERE, " ", null, SOUMIS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecisionRevue(IssueRevue.LIBERE, "1", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new DecisionRevue(IssueRevue.REJETE_HEURE_BUTOIR, DecisionRevue.SYSTEME, null, SOUMIS).decideur())
                .isEqualTo("SYSTEM");
    }

    @Test
    void le_delai_d_acces_a_l_historique_est_strictement_positif_et_valide_au_demarrage() {
        for (Duration invalide : new Duration[] {null, Duration.ZERO, Duration.ofMillis(-1)}) {
            assertThatThrownBy(() -> new DelaiAccesHistorique(invalide))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nb.fraude.acces-historique.delai");
        }
        assertThat(new DelaiAccesHistorique(Duration.ofSeconds(2)).millisecondes()).isEqualTo(2000);
        assertThat(new DelaiAccesHistorique(Duration.ofNanos(1)).millisecondes()).isEqualTo(1);
    }

    @Test
    void le_role_est_lu_par_son_nom_exact_et_une_erreur_de_controle_garde_sa_cause() {
        assertThat(Role.depuis("ADMIN")).contains(Role.ADMIN);
        assertThat(Role.depuis("CLIENT")).contains(Role.CLIENT);
        assertThat(Role.depuis("admin")).isEmpty();
        assertThat(Role.depuis(null)).isEmpty();
        IllegalStateException cause = new IllegalStateException("x");
        assertThat(new ControleEnErreur(cause)).hasCause(cause);
    }
}
