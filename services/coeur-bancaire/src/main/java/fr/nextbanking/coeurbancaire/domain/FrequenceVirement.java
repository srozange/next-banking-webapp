package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Fréquence d'un virement programmé, dans l'ordre proposé à l'écran (FR-004-08). Les noms sont ceux de l'énumération
 * du legacy et les valeurs échangées avec le front.
 */
public enum FrequenceVirement {
    HEBDOMADAIRE,
    MENSUEL,
    BIMESTRIEL,
    TRIMESTRIEL,
    SEMESTRIEL,
    ANNUEL;

    /**
     * Échéance qui suit l'échéance prévue (jamais l'heure réelle d'exécution : pas de dérive) : sept jours, un, deux,
     * trois ou six mois, ou un an, calculés dans le fuseau donné, l'heure locale étant conservée à travers les
     * changements d'heure (FR-021-08, BUG-069).
     */
    public Instant suivante(Instant echeance, ZoneId fuseau) {
        var locale = echeance.atZone(fuseau);
        var suivante = switch (this) {
            case HEBDOMADAIRE -> locale.plusDays(7);
            case MENSUEL -> locale.plusMonths(1);
            case BIMESTRIEL -> locale.plusMonths(2);
            case TRIMESTRIEL -> locale.plusMonths(3);
            case SEMESTRIEL -> locale.plusMonths(6);
            case ANNUEL -> locale.plusYears(1);
        };
        return suivante.toInstant();
    }
}
