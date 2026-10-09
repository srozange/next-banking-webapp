package fr.nextbanking.openbankingconseiller.adapter.out.horloge;

import fr.nextbanking.openbankingconseiller.application.port.out.Horloge;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant du profil {@code rejeu} : l'horloge système décalée de {@code HORLOGE_DECALAGE}, durée ISO-8601
 * (décalage commun à tous les services = date de référence moins instant réel, calculé au démarrage de la pile).
 * L'horloge est réglée au démarrage puis avance comme l'horloge système (jamais figée). Un décalage absent ou illisible
 * fait échouer le démarrage.
 */
@Component
@Profile("rejeu")
public class HorlogeReglee implements Horloge {

    static final String PROPRIETE = "HORLOGE_DECALAGE";

    private final Clock horloge;

    @Autowired
    public HorlogeReglee(@Value("${nb.fuseau}") String fuseau, @Value("${" + PROPRIETE + ":}") String decalage) {
        this(Clock.system(HorlogeSysteme.lireFuseau(fuseau)), decalage);
    }

    HorlogeReglee(Clock systeme, String decalage) {
        this.horloge = Clock.offset(systeme, lireDecalage(decalage));
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

    static Duration lireDecalage(String decalage) {
        if (decalage == null || decalage.isBlank()) {
            throw new IllegalStateException("La variable " + PROPRIETE + " est obligatoire en profil rejeu.");
        }
        try {
            return Duration.parse(decalage.strip());
        } catch (DateTimeException e) {
            throw new IllegalStateException("La variable " + PROPRIETE
                    + " n'est pas une durée ISO-8601 lisible : " + decalage, e);
        }
    }
}
