package fr.nextbanking.documents.adapter;

import fr.nextbanking.documents.application.port.out.Horloge;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Horloge de test fixe : 22h30 UTC le 9 octobre 2026, soit le 10 octobre à Paris (UTC+2). Les dates imprimées sur les
 * documents viennent de l'Horloge dans le fuseau métier, pas de la date UTC.
 */
@TestConfiguration
public class ConfigurationHorlogeFixe {

    public static final Instant INSTANT = Instant.parse("2026-10-09T22:30:00Z");
    public static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 10);
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Bean
    @Primary
    Horloge horlogeFixe() {
        Clock horloge = Clock.fixed(INSTANT, PARIS);
        return new Horloge() {
            @Override
            public Instant instant() {
                return horloge.instant();
            }

            @Override
            public LocalDate aujourdhui() {
                return LocalDate.now(horloge);
            }

            @Override
            public ZoneId fuseau() {
                return PARIS;
            }
        };
    }
}
