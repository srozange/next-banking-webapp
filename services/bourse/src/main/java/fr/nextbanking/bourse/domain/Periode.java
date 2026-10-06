package fr.nextbanking.bourse.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Période d'historique (FR-009-08, FR-029-01, FR-029-02) : 1W et 1M lisent la série journalière, 1Y et 5Y la série
 * hebdomadaire ; la date limite se calcule depuis la date du jour. Une période absente ou inconnue vaut 1M
 * (RG-029-003 : date limite d'un mois, série journalière comme le legacy).
 */
public enum Periode {
    SEMAINE("1W", "1 semaine (données journalières)", TypeSerie.JOURNALIERE, jour -> jour.minusWeeks(1)),
    MOIS("1M", "1 mois (données journalières)", TypeSerie.JOURNALIERE, jour -> jour.minusMonths(1)),
    AN("1Y", "1 an (données hebdomadaires)", TypeSerie.HEBDOMADAIRE, jour -> jour.minusYears(1)),
    CINQ_ANS("5Y", "5 ans (données hebdomadaires)", TypeSerie.HEBDOMADAIRE, jour -> jour.minusYears(5));

    private final String code;
    private final String libelle;
    private final TypeSerie serie;
    private final UnaryOperator<LocalDate> recul;

    Periode(String code, String libelle, TypeSerie serie, UnaryOperator<LocalDate> recul) {
        this.code = code;
        this.libelle = libelle;
        this.serie = serie;
        this.recul = recul;
    }

    /** Période désignée par son code (casse exacte) ; absente ou inconnue → {@link #MOIS}. */
    public static Periode depuis(String code) {
        for (Periode periode : values()) {
            if (periode.code.equals(code)) {
                return periode;
            }
        }
        return MOIS;
    }

    public String code() {
        return code;
    }

    /** Libellé du badge du graphique (MSG-029-070 à 073). */
    public String libelle() {
        return libelle;
    }

    public TypeSerie serie() {
        return serie;
    }

    /** Date limite : aujourd'hui moins une semaine, un mois, un an ou cinq ans. */
    public LocalDate dateLimite(LocalDate aujourdhui) {
        return recul.apply(aujourdhui);
    }

    /** Points datés au plus tôt de la date limite, triés par date croissante. */
    public List<PointCotation> retenir(List<PointCotation> serie, LocalDate aujourdhui) {
        LocalDate limite = dateLimite(aujourdhui);
        return serie.stream()
                .filter(point -> !point.date().isBefore(limite))
                .sorted(Comparator.comparing(PointCotation::date))
                .toList();
    }
}
