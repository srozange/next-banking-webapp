package fr.nextbanking.bourse.adapter.out.horloge;

import fr.nextbanking.bourse.application.port.out.Horloge;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : horloge système dans le fuseau métier lu dans {@code nb.fuseau}.
 * Un fuseau absent ou invalide fait échouer le démarrage.
 */
@Component
public class HorlogeSysteme implements Horloge {

    private final Clock horloge;

    @Autowired
    public HorlogeSysteme(@Value("${nb.fuseau}") String fuseau) {
        this(Clock.system(lireFuseau(fuseau)));
    }

    HorlogeSysteme(Clock horloge) {
        this.horloge = horloge;
    }

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
        return horloge.getZone();
    }

    static ZoneId lireFuseau(String fuseau) {
        if (fuseau == null || fuseau.isBlank()) {
            throw new IllegalStateException("La propriété nb.fuseau est obligatoire.");
        }
        try {
            return ZoneId.of(fuseau);
        } catch (DateTimeException e) {
            throw new IllegalStateException("La propriété nb.fuseau n'est pas un fuseau valide : " + fuseau, e);
        }
    }
}
