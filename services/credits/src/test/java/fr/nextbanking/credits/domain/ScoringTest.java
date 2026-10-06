package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ScoringTest {

    static Pret pret(TypePret type, String montant, int duree) {
        return new Pret(42L, 5L, type, new BigDecimal(montant), new BigDecimal("3.5000"), duree,
                new BigDecimal("293.02"), new BigDecimal("10548.72"), new BigDecimal("548.72"), "Voiture",
                StatutPret.EN_ATTENTE, LocalDate.of(2026, 10, 1), LocalDate.of(2029, 10, 1));
    }

    @ParameterizedTest
    @EnumSource(TypePret.class)
    @Tag("FR-024-04")
    void le_montant_egal_au_plafond_est_approuve(TypePret type) {
        assertThat(Scoring.decider(pret(type, type.plafond().toPlainString() + ".00", 36)))
                .isEqualTo(DecisionScoring.APPROUVE);
    }

    @ParameterizedTest
    @EnumSource(TypePret.class)
    @Tag("FR-024-04")
    void le_plafond_plus_un_centime_est_refuse(TypePret type) {
        assertThat(Scoring.decider(pret(type, type.plafond().add(new BigDecimal("0.01")).toPlainString(), 36)))
                .isEqualTo(DecisionScoring.REFUSE);
    }

    @Test
    @Tag("FR-024-04")
    void les_plafonds_sont_ceux_du_legacy() {
        assertThat(TypePret.CONSOMMATION.plafond()).isEqualByComparingTo("25000");
        assertThat(TypePret.AUTO.plafond()).isEqualByComparingTo("40000");
        assertThat(TypePret.TRAVAUX.plafond()).isEqualByComparingTo("80000");
        assertThat(TypePret.ETUDIANT.plafond()).isEqualByComparingTo("20000");
        assertThat(TypePret.IMMOBILIER.plafond()).isEqualByComparingTo("600000");
    }

    @Test
    @Tag("FR-024-03")
    void une_duree_de_301_mois_est_refusee_meme_pour_un_petit_montant_et_300_est_approuvee() {
        assertThat(Scoring.decider(pret(TypePret.CONSOMMATION, "100.00", 301))).isEqualTo(DecisionScoring.REFUSE);
        assertThat(Scoring.decider(pret(TypePret.CONSOMMATION, "100.00", 300))).isEqualTo(DecisionScoring.APPROUVE);
    }

    @Test
    @Tag("FR-024-05")
    @Tag("BUG-078")
    void un_pret_sans_type_est_refuse() {
        assertThat(Scoring.decider(pret(null, "10000.00", 60))).isEqualTo(DecisionScoring.REFUSE);
    }

    @Test
    @Tag("FR-024-03")
    void la_duree_est_evaluee_avant_le_type() {
        assertThat(Scoring.decider(pret(null, "10000.00", 301))).isEqualTo(DecisionScoring.REFUSE);
    }
}
