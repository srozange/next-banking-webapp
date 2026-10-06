package fr.nextbanking.bourse.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Port sortant : seule source du temps pour le domaine et l'application.
 */
public interface Horloge {

    /** Instant courant. */
    Instant instant();

    /** Date du jour dans le fuseau métier. */
    LocalDate aujourdhui();

    /** Fuseau métier (Europe/Paris). */
    ZoneId fuseau();
}
