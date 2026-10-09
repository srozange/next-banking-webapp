package fr.nextbanking.documents.adapter.out.horloge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Horloge réglée du profil {@code rejeu} (NFR-PAR-05, AR-44) : à la seconde de la date de départ, puis qui avance
 * comme l'horloge système injectée ; un décalage illisible empêche le démarrage.
 */
@Tag("NFR-PAR-05")
class HorlogeRegleeTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Instant DEPART = ZonedDateTime.of(LocalDateTime.of(2026, 11, 17, 10, 0, 0), PARIS).toInstant();

    /** Horloge système de test que le test fait avancer. */
    private static final class HorlogeMutable extends Clock {
        private Instant instant;

        private HorlogeMutable(Instant instant) {
            this.instant = instant;
        }

        void avancer(Duration duree) {
            instant = instant.plus(duree);
        }

        @Override
        public ZoneId getZone() {
            return PARIS;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Test
    void l_instant_vaut_la_date_de_depart_a_la_seconde_puis_avance_comme_l_horloge_systeme() {
        HorlogeMutable systeme = new HorlogeMutable(Instant.parse("2026-10-06T08:15:42Z"));
        String decalage = Duration.between(systeme.instant(), DEPART).toString();
        HorlogeReglee horloge = new HorlogeReglee(systeme, decalage);

        assertThat(horloge.instant()).isEqualTo(DEPART);
        assertThat(horloge.aujourdhui()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(horloge.fuseau()).isEqualTo(PARIS);
        systeme.avancer(Duration.ofSeconds(90));
        assertThat(horloge.instant()).isEqualTo(DEPART.plusSeconds(90));
    }

    @Test
    void la_date_suit_le_fuseau_metier_apres_minuit_a_paris() {
        HorlogeMutable systeme = new HorlogeMutable(Instant.parse("2026-10-06T08:15:42Z"));
        HorlogeReglee horloge = new HorlogeReglee(systeme, Duration.between(systeme.instant(),
                Instant.parse("2026-11-17T22:30:00Z")).toString());

        // 22h30 UTC = 23h30 à Paris le 17 novembre (UTC+1) ; une heure plus tard, déjà le 18.
        assertThat(horloge.aujourdhui()).isEqualTo(LocalDate.of(2026, 11, 17));
        systeme.avancer(Duration.ofHours(1));
        assertThat(horloge.aujourdhui()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    void un_decalage_negatif_est_lu() {
        HorlogeReglee horloge = new HorlogeReglee(Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), PARIS), "-PT1H");

        assertThat(horloge.instant()).isEqualTo(Instant.parse("2026-11-17T09:00:00Z"));
    }

    @Test
    void un_decalage_absent_ou_illisible_est_refuse() {
        for (String decalage : new String[] {null, "", "  "}) {
            assertThatThrownBy(() -> HorlogeReglee.lireDecalage(decalage))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("HORLOGE_DECALAGE est obligatoire");
        }
        for (String decalage : new String[] {"abc", "3600", "1h", "PT"}) {
            assertThatThrownBy(() -> HorlogeReglee.lireDecalage(decalage))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("durée ISO-8601");
        }
    }

    @Test
    void le_constructeur_de_spring_lit_le_fuseau_et_le_decalage() {
        HorlogeReglee horloge = new HorlogeReglee("Europe/Paris", "PT0S");

        assertThat(horloge.fuseau()).isEqualTo(PARIS);
        assertThat(horloge.instant()).isNotNull();
        assertThatThrownBy(() -> new HorlogeReglee("Mars/Olympus", "PT0S"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void en_profil_rejeu_un_decalage_illisible_empeche_le_demarrage_et_un_bon_decalage_active_l_horloge_reglee() {
        ApplicationContextRunner base = new ApplicationContextRunner()
                .withInitializer(contexte -> contexte.getEnvironment().addActiveProfile("rejeu"))
                .withConfiguration(UserConfigurations.of(HorlogeReglee.class, HorlogeSysteme.class))
                .withPropertyValues("nb.fuseau=Europe/Paris");

        base.withPropertyValues("HORLOGE_DECALAGE=abc").run(contexte -> assertThat(contexte).hasFailed());
        base.run(contexte -> assertThat(contexte).hasFailed());
        base.withPropertyValues("HORLOGE_DECALAGE=PT1H").run(contexte -> assertThat(contexte)
                .hasSingleBean(HorlogeReglee.class).doesNotHaveBean(HorlogeSysteme.class));
        new ApplicationContextRunner()
                .withConfiguration(UserConfigurations.of(HorlogeReglee.class, HorlogeSysteme.class))
                .withPropertyValues("nb.fuseau=Europe/Paris")
                .run(contexte -> assertThat(contexte).hasSingleBean(HorlogeSysteme.class)
                        .doesNotHaveBean(HorlogeReglee.class));
    }

    @Test
    void l_instant_ne_depend_pas_du_fuseau_du_processus() {
        HorlogeReglee horloge = new HorlogeReglee(Clock.fixed(Instant.parse("2026-11-17T00:00:00Z"),
                ZoneOffset.UTC), "PT0S");

        assertThat(horloge.instant()).isEqualTo(Instant.parse("2026-11-17T00:00:00Z"));
    }
}
