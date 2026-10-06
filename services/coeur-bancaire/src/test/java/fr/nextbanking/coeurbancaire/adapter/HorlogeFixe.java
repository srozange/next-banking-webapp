package fr.nextbanking.coeurbancaire.adapter;

import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Horloge réglée des tests d'intégration : l'instant ne bouge que sur demande. Fuseau métier : Europe/Paris. */
public final class HorlogeFixe implements Horloge {

    /** 17/11/2026 à 10:00 Europe/Paris (parcours nominal de SC-003-04). */
    public static final Instant DIX_HEURES_PARIS = Instant.parse("2026-11-17T09:00:00Z");

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private volatile Instant instant = DIX_HEURES_PARIS;

    public void regler(Instant nouvelInstant) {
        this.instant = nouvelInstant;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public LocalDate aujourdhui() {
        return instant.atZone(PARIS).toLocalDate();
    }

    @Override
    public ZoneId fuseau() {
        return PARIS;
    }
}
