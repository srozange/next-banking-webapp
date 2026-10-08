package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Échéance suivante d'un virement programmé : calculée depuis l'échéance prévue, en heure de Paris, sans dérive. */
@Tag("FR-021-08")
@Tag("FR-004-14")
@Tag("BUG-069")
class FrequenceVirementTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Instant PREVUE = Instant.parse("2026-01-15T07:00:00Z");

    @Test
    void les_six_frequences_avancent_d_une_periode() {
        assertThat(FrequenceVirement.HEBDOMADAIRE.suivante(PREVUE, PARIS)).isEqualTo(Instant.parse("2026-01-22T07:00:00Z"));
        assertThat(FrequenceVirement.MENSUEL.suivante(PREVUE, PARIS)).isEqualTo(Instant.parse("2026-02-15T07:00:00Z"));
        assertThat(FrequenceVirement.BIMESTRIEL.suivante(PREVUE, PARIS)).isEqualTo(Instant.parse("2026-03-15T07:00:00Z"));
        assertThat(FrequenceVirement.TRIMESTRIEL.suivante(PREVUE, PARIS)).isEqualTo(Instant.parse("2026-04-15T06:00:00Z"));
        assertThat(FrequenceVirement.SEMESTRIEL.suivante(PREVUE, PARIS)).isEqualTo(Instant.parse("2026-07-15T06:00:00Z"));
        assertThat(FrequenceVirement.ANNUEL.suivante(PREVUE, PARIS)).isEqualTo(Instant.parse("2027-01-15T07:00:00Z"));
    }

    @Test
    void douze_mensualites_restent_le_15_a_8_heures_de_paris() {
        Instant echeance = Instant.parse("2026-01-15T07:00:00Z");
        for (int i = 0; i < 12; i++) {
            echeance = FrequenceVirement.MENSUEL.suivante(echeance, PARIS);
            var locale = echeance.atZone(PARIS);
            assertThat(locale.getDayOfMonth()).isEqualTo(15);
            assertThat(locale.getHour()).isEqualTo(8);
            assertThat(locale.getMinute()).isZero();
        }
    }

    @Test
    void l_heure_locale_est_conservee_a_travers_le_changement_d_heure() {
        Instant avantEte = Instant.parse("2026-03-20T07:00:00Z");

        assertThat(FrequenceVirement.MENSUEL.suivante(avantEte, PARIS)).isEqualTo(Instant.parse("2026-04-20T06:00:00Z"));
        assertThat(FrequenceVirement.HEBDOMADAIRE.suivante(Instant.parse("2026-03-26T07:00:00Z"), PARIS))
                .isEqualTo(Instant.parse("2026-04-02T06:00:00Z"));
    }
}
