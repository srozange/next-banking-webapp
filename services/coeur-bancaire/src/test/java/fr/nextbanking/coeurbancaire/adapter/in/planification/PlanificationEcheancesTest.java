package fr.nextbanking.coeurbancaire.adapter.in.planification;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

/** Déclenchement des échéances : toutes les 5 minutes en {@code Europe/Paris}, désactivé en profil {@code rejeu}. */
@Tag("FR-021-01")
@Tag("NFR-BATCH-01")
class PlanificationEcheancesTest {

    @Test
    void le_traitement_est_planifie_toutes_les_5_minutes_en_europe_paris() throws Exception {
        Method methode = PlanificationEcheances.class.getMethod("executerLesEcheancesDues");

        Scheduled planification = methode.getAnnotation(Scheduled.class);

        assertThat(planification.cron()).isEqualTo("0 */5 * * * *");
        assertThat(planification.zone()).isEqualTo("Europe/Paris");
    }

    @Test
    void il_est_desactive_en_profil_rejeu() {
        assertThat(PlanificationEcheances.class.getAnnotation(Profile.class).value()).containsExactly("!rejeu");
    }

    @Test
    void chaque_declenchement_lance_l_execution_des_echeances_dues() {
        AtomicInteger appels = new AtomicInteger();
        PlanificationEcheances planification = new PlanificationEcheances(() -> appels.incrementAndGet());

        planification.executerLesEcheancesDues();
        planification.executerLesEcheancesDues();

        assertThat(appels).hasValue(2);
    }
}
