package fr.nextbanking.coeurbancaire.adapter.in.planification;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Déclenchement du rejet à l'échéance : {@code @Scheduled} toutes les minutes en zone {@code Europe/Paris}, désactivé
 * en profil {@code rejeu}.
 */
@Tag("FR-027-17")
@Tag("NFR-BATCH-01")
class PlanificationRejetEcheanceTest {

    @Test
    void le_traitement_est_planifie_toutes_les_minutes_en_europe_paris() throws Exception {
        Method methode = PlanificationRejetEcheance.class.getMethod("rejeterLesVirementsEchus");

        Scheduled planification = methode.getAnnotation(Scheduled.class);

        assertThat(planification.cron()).isEqualTo("0 * * * * *");
        assertThat(planification.zone()).isEqualTo("Europe/Paris");
    }

    @Test
    void il_est_desactive_en_profil_rejeu() {
        assertThat(PlanificationRejetEcheance.class.getAnnotation(Profile.class).value()).containsExactly("!rejeu");
    }

    @Test
    void chaque_declenchement_lance_le_rejet_des_virements_echus() {
        AtomicInteger appels = new AtomicInteger();
        AtomicInteger rejetes = new AtomicInteger(0);
        PlanificationRejetEcheance planification = new PlanificationRejetEcheance(() -> {
            appels.incrementAndGet();
            return rejetes.get();
        });

        planification.rejeterLesVirementsEchus();
        rejetes.set(2);
        planification.rejeterLesVirementsEchus();

        assertThat(appels).hasValue(2);
    }
}
