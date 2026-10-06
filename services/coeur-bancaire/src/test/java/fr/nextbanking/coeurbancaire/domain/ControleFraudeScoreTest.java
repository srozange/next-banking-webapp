package fr.nextbanking.coeurbancaire.domain;

import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.DIX_HEURES;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.FRANCE;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.evaluer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Score cumulé et alertes : à 75 aucune alerte, à 80 une alerte OPEN par signal levé. */
@Tag("FR-003-25")
@Tag("FR-027-03")
class ControleFraudeScoreTest {

    private static final Instant CREE_LE = Instant.parse("2026-11-17T09:00:00Z");

    @Test
    void a_75_aucune_alerte_n_est_a_ecrire() {
        // 40 (montant 5000) + 35 (non bénéficiaire) = 75.
        Evaluation evaluation = evaluer("5000.50", FRANCE, false, new HistoriqueClient(0, null, 0), DIX_HEURES);

        assertThat(evaluation.score()).isEqualTo(75);
        assertThat(evaluation.alerter()).isFalse();
        assertThat(evaluation.alertes(7, 12, CREE_LE)).isEmpty();
    }

    @Test
    void a_80_une_alerte_open_par_signal_leve_avec_son_score_et_sa_description() {
        Evaluation evaluation = evaluer("5000.00", FRANCE, false, new HistoriqueClient(0, null, 0), DIX_HEURES);

        // 5000 est un multiple de 1000 : LARGE_AMOUNT 40 + ROUND_AMOUNT 20 + NEW_BENEFICIARY 35 = 95.
        assertThat(evaluation.score()).isEqualTo(95);
        assertThat(evaluation.alerter()).isTrue();
        assertThat(evaluation.alertes(7, 12, CREE_LE)).extracting(AlerteFraude::regle, AlerteFraude::score,
                AlerteFraude::statut, AlerteFraude::clientId, AlerteFraude::virementId, AlerteFraude::creeLe)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TypeSignal.LARGE_AMOUNT, 40, StatutAlerte.OPEN, 7L, 12L,
                                CREE_LE),
                        org.assertj.core.groups.Tuple.tuple(TypeSignal.NEW_BENEFICIARY, 35, StatutAlerte.OPEN, 7L,
                                12L, CREE_LE),
                        org.assertj.core.groups.Tuple.tuple(TypeSignal.ROUND_AMOUNT, 20, StatutAlerte.OPEN, 7L, 12L,
                                CREE_LE));
    }

    @Test
    void exactement_80_declenche_les_alertes() {
        // 45 (heure inhabituelle) + 35 (non bénéficiaire) = 80.
        Evaluation evaluation = evaluer("100.00", FRANCE, false, new HistoriqueClient(0, null, 0),
                Instant.parse("2026-11-17T22:30:00Z"));

        assertThat(evaluation.score()).isEqualTo(80);
        assertThat(evaluation.alertes(7, 12, CREE_LE)).extracting(AlerteFraude::description).containsExactly(
                "Target IBAN FR7630006000011234567890189 is not registered as a trusted beneficiary",
                "Transaction initiated at hour 23 which is outside normal operating hours (06:00-23:00)");
    }

    @Test
    void les_huit_signaux_sont_evalues_dans_l_ordre_de_la_liste() {
        Evaluation evaluation = evaluer("15000.00", new Iban("DE89370400440532013000"), false,
                new HistoriqueClient(9, Instant.parse("2026-11-17T22:29:59Z"), 5), Instant.parse("2026-11-17T22:30:00Z"));

        assertThat(evaluation.signaux()).extracting(Signal::type).containsExactly(TypeSignal.LARGE_AMOUNT,
                TypeSignal.HIGH_FREQUENCY, TypeSignal.NEW_BENEFICIARY, TypeSignal.UNUSUAL_HOURS,
                TypeSignal.ROUND_AMOUNT, TypeSignal.FOREIGN_IBAN, TypeSignal.RAPID_SUCCESSION,
                TypeSignal.VELOCITY_CHECK);
        assertThat(evaluation.score()).isEqualTo(80 + 50 + 35 + 45 + 20 + 30 + 55 + 65);
    }

    @Test
    void les_modeles_refusent_les_valeurs_absentes() {
        assertThatThrownBy(() -> new Signal(null, 1, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Signal(TypeSignal.ROUND_AMOUNT, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlerteFraude(1, 1, null, 1, "x", StatutAlerte.OPEN, CREE_LE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlerteFraude(1, 1, TypeSignal.ROUND_AMOUNT, 1, null, StatutAlerte.OPEN, CREE_LE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlerteFraude(1, 1, TypeSignal.ROUND_AMOUNT, 1, "x", null, CREE_LE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlerteFraude(1, 1, TypeSignal.ROUND_AMOUNT, 1, "x", StatutAlerte.OPEN, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Evaluation(List.of(), 0, false).signaux()).isEmpty();
    }
}
