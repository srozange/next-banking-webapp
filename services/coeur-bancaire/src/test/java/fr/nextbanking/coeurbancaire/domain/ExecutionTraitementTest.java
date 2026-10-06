package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-011-09")
class ExecutionTraitementTest {

    private static final Instant DEBUT = Instant.parse("2026-10-05T01:00:00Z");

    @Test
    void la_duree_est_fin_moins_debut_en_millisecondes() {
        ExecutionTraitement execution = new ExecutionTraitement(1, "job", DEBUT, DEBUT.plusMillis(1500), 3,
                StatutTraitement.SUCCESS, null);

        assertThat(execution.dureeMs()).isEqualTo(1500);
    }

    @Test
    void la_duree_est_nulle_si_le_debut_ou_la_fin_manque() {
        assertThat(new ExecutionTraitement(1, "job", null, DEBUT, null, null, null).dureeMs()).isZero();
        assertThat(new ExecutionTraitement(1, "job", DEBUT, null, null, null, null).dureeMs()).isZero();
        assertThat(new ExecutionTraitement(1, "job", null, null, null, null, null).dureeMs()).isZero();
    }

    @Test
    void les_trois_statuts_du_legacy_existent() {
        assertThat(StatutTraitement.values()).containsExactly(StatutTraitement.SUCCESS, StatutTraitement.FAILED,
                StatutTraitement.PARTIAL);
    }
}
