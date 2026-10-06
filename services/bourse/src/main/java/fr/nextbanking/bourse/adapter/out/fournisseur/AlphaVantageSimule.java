package fr.nextbanking.bourse.adapter.out.fournisseur;

import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.application.port.out.Horloge;
import fr.nextbanking.bourse.domain.PointCotation;
import fr.nextbanking.bourse.domain.TypeSerie;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptateur simulé du fournisseur de cotations, actif par défaut : séries fixes pour les 9 valeurs de référence,
 * datées depuis l'Horloge. Série journalière : 100 jours ouvrés finissant au dernier jour ouvré au plus tard
 * aujourd'hui ; série hebdomadaire : 270 semaines finissant au dernier vendredi. Valeurs déterministes (fonction du
 * symbole et de la date) à 4 décimales ; un symbole inconnu donne une liste vide. Aucun appel réseau.
 */
public class AlphaVantageSimule implements Cotations {

    static final int JOURS = 100;
    static final int SEMAINES = 270;

    /** Cours de base en dix-millièmes, et graine de chaque symbole de référence. */
    private static final Map<String, long[]> REFERENCE = Map.of(
            "AI.PA", new long[] {1_750_000L, 1},
            "BNP.PA", new long[] {620_000L, 2},
            "MC.PA", new long[] {6_800_000L, 3},
            "OR.PA", new long[] {4_100_000L, 4},
            "SAN.PA", new long[] {910_000L, 5},
            "TTE.PA", new long[] {580_000L, 6},
            "AAPL", new long[] {2_300_000L, 7},
            "GOOGL", new long[] {1_700_000L, 8},
            "MSFT", new long[] {4_300_000L, 9});

    private final Horloge horloge;

    public AlphaVantageSimule(Horloge horloge) {
        this.horloge = horloge;
    }

    @Override
    public List<PointCotation> serie(String symbole, TypeSerie type) {
        long[] reference = REFERENCE.get(symbole);
        if (reference == null) {
            return List.of();
        }
        List<LocalDate> dates = type == TypeSerie.JOURNALIERE ? joursOuvres() : vendredis();
        return dates.stream().map(date -> point(date, reference[0], reference[1])).toList();
    }

    private List<LocalDate> joursOuvres() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate jour = horloge.aujourdhui();
        while (dates.size() < JOURS) {
            if (jour.getDayOfWeek() != DayOfWeek.SATURDAY && jour.getDayOfWeek() != DayOfWeek.SUNDAY) {
                dates.addFirst(jour);
            }
            jour = jour.minusDays(1);
        }
        return dates;
    }

    private List<LocalDate> vendredis() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate vendredi = horloge.aujourdhui().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        for (int semaine = 0; semaine < SEMAINES; semaine++) {
            dates.addFirst(vendredi.minusWeeks(semaine));
        }
        return dates;
    }

    private static PointCotation point(LocalDate date, long base, long graine) {
        long jour = date.toEpochDay();
        long cloture = base + base * (Math.floorMod(jour * 37 + graine * 101, 201) - 100) / 1000;
        long ouverture = base + base * (Math.floorMod(jour * 53 + graine * 59, 201) - 100) / 1000;
        long haut = Math.max(ouverture, cloture) + base * (Math.floorMod(jour + graine, 7) + 1) / 1000;
        long bas = Math.min(ouverture, cloture) - base * (Math.floorMod(jour * 3 + graine, 7) + 1) / 1000;
        long volume = 1_000_000L + Math.floorMod(jour * 7919 + graine * 104_729, 500_000);
        return new PointCotation(date, prix(ouverture), prix(haut), prix(bas), prix(cloture), volume);
    }

    private static BigDecimal prix(long dixMilliemes) {
        return BigDecimal.valueOf(dixMilliemes, 4);
    }
}
