package fr.nextbanking.coeurbancaire.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.out.evenements.RelaisOutbox;
import fr.nextbanking.coeurbancaire.adapter.out.evenements.RelaisOutboxAudit;
import fr.nextbanking.coeurbancaire.adapter.out.horloge.HorlogeReglee;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Profil {@code rejeu} sur un contexte réel : l'horloge est réglée sur la date de départ puis avance, et aucun
 * traitement planifié métier n'est actif (seuls restent les relais de l'outbox et de l'outbox d'audit, AD-18, AD-21).
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@ActiveProfiles("rejeu")
@DirtiesContext
@Tag("NFR-PAR-05")
@Tag("NFR-BATCH-03")
class ProfilRejeuIT {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Instant DEPART = ZonedDateTime.of(LocalDateTime.of(2026, 11, 17, 10, 0, 0), PARIS).toInstant();
    private static final Instant LANCEMENT = Instant.now();

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Un seul décalage = date de référence moins instant réel.
        registre.add("HORLOGE_DECALAGE", () -> Duration.between(LANCEMENT, DEPART).toString());
    }

    @Autowired
    private Horloge horloge;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor planification;

    @Test
    void l_horloge_active_est_l_horloge_reglee_a_la_date_de_depart_puis_elle_avance() throws Exception {
        assertThat(horloge).isInstanceOf(HorlogeReglee.class);
        Instant lue = horloge.instant();

        // Réglée à 10:00:00 au démarrage du test, à la seconde près ; le contexte a mis quelques secondes à démarrer.
        assertThat(Duration.between(DEPART, lue)).isBetween(Duration.ZERO, Duration.ofMinutes(2));
        assertThat(horloge.aujourdhui()).isEqualTo(LocalDate.of(2026, 11, 17));
        Thread.sleep(1100);
        assertThat(horloge.instant()).isAfter(lue);
    }

    @Test
    void aucun_traitement_planifie_metier_n_est_actif_seuls_les_relais_restent() {
        List<String> taches = planification.getScheduledTasks().stream().map(ScheduledTask::toString).toList();

        assertThat(taches).isNotEmpty().allMatch(tache -> tache.matches(".*\\.Relais(Outbox|OutboxAudit)\\.relayer"));
        assertThat(taches).anyMatch(tache -> tache.endsWith(RelaisOutbox.class.getSimpleName() + ".relayer"))
                .anyMatch(tache -> tache.endsWith(RelaisOutboxAudit.class.getSimpleName() + ".relayer"));
    }
}
