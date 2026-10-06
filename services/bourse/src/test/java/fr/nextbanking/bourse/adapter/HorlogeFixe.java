package fr.nextbanking.bourse.adapter;

import fr.nextbanking.bourse.application.port.out.Horloge;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Horloge fixée au 2026-11-17 à 10 h (Europe/Paris), prioritaire sur l'horloge système. */
@TestConfiguration
public class HorlogeFixe {

    static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Bean
    @Primary
    Horloge horlogeFixe() {
        return new Horloge() {
            @Override
            public Instant instant() {
                return AUJOURDHUI.atTime(10, 0).atZone(PARIS).toInstant();
            }

            @Override
            public LocalDate aujourdhui() {
                return AUJOURDHUI;
            }

            @Override
            public ZoneId fuseau() {
                return PARIS;
            }
        };
    }
}
