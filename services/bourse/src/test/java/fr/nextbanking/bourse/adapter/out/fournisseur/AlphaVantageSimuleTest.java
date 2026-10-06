package fr.nextbanking.bourse.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.bourse.application.port.out.Horloge;
import fr.nextbanking.bourse.domain.PointCotation;
import fr.nextbanking.bourse.domain.TypeSerie;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class AlphaVantageSimuleTest {

    private static final List<String> REFERENCE =
            List.of("AI.PA", "BNP.PA", "MC.PA", "OR.PA", "SAN.PA", "TTE.PA", "AAPL", "GOOGL", "MSFT");

    @Test
    @Tag("FR-009-08")
    @Tag("FR-029-01")
    void serie_journaliere_de_100_jours_ouvres_finissant_aujourd_hui() {
        List<PointCotation> serie = simule(LocalDate.of(2026, 11, 17)).serie("AI.PA", TypeSerie.JOURNALIERE);

        assertThat(serie).hasSize(100);
        assertThat(serie.getLast().date()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(serie).allSatisfy(point -> assertThat(point.date().getDayOfWeek())
                .isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        assertThat(serie).isSortedAccordingTo((a, b) -> a.date().compareTo(b.date()));
        assertThat(serie.getFirst().date()).isBefore(LocalDate.of(2026, 10, 17));
    }

    @Test
    void un_dimanche_la_serie_journaliere_finit_le_vendredi() {
        List<PointCotation> serie = simule(LocalDate.of(2026, 11, 15)).serie("MSFT", TypeSerie.JOURNALIERE);

        assertThat(serie.getLast().date()).isEqualTo(LocalDate.of(2026, 11, 13));
    }

    @Test
    @Tag("FR-029-01")
    void serie_hebdomadaire_de_270_semaines_finissant_au_dernier_vendredi() {
        List<PointCotation> serie = simule(LocalDate.of(2026, 11, 17)).serie("AAPL", TypeSerie.HEBDOMADAIRE);

        assertThat(serie).hasSize(270);
        assertThat(serie.getLast().date()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(serie).allSatisfy(point -> assertThat(point.date().getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY));
        assertThat(serie.getFirst().date()).isBefore(LocalDate.of(2021, 11, 17));
    }

    @Test
    void valeurs_deterministes_a_quatre_decimales_et_coherentes_pour_les_neuf_valeurs() {
        AlphaVantageSimule simule = simule(LocalDate.of(2026, 11, 17));

        for (String symbole : REFERENCE) {
            for (TypeSerie type : TypeSerie.values()) {
                List<PointCotation> serie = simule.serie(symbole, type);
                assertThat(serie).as(symbole).isNotEmpty().isEqualTo(simule.serie(symbole, type));
                assertThat(serie).allSatisfy(point -> {
                    assertThat(point.cloture().scale()).isEqualTo(4);
                    assertThat(point.haut()).isGreaterThanOrEqualTo(point.ouverture()).isGreaterThanOrEqualTo(point.bas());
                    assertThat(point.haut()).isGreaterThanOrEqualTo(point.cloture());
                    assertThat(point.bas()).isLessThanOrEqualTo(point.cloture()).isPositive();
                    assertThat(point.volume()).isPositive();
                });
            }
        }
    }

    @Test
    @Tag("FR-009-09")
    void un_symbole_inconnu_ou_d_une_autre_casse_rend_une_liste_vide() {
        AlphaVantageSimule simule = simule(LocalDate.of(2026, 11, 17));

        assertThat(simule.serie("INCONNU.XX", TypeSerie.JOURNALIERE)).isEmpty();
        assertThat(simule.serie("ai.pa", TypeSerie.HEBDOMADAIRE)).isEmpty();
    }

    static AlphaVantageSimule simule(LocalDate aujourdhui) {
        return new AlphaVantageSimule(horloge(aujourdhui));
    }

    static Horloge horloge(LocalDate aujourdhui) {
        ZoneId paris = ZoneId.of("Europe/Paris");
        return new Horloge() {
            @Override
            public Instant instant() {
                return aujourdhui.atTime(10, 0).atZone(paris).toInstant();
            }

            @Override
            public LocalDate aujourdhui() {
                return aujourdhui;
            }

            @Override
            public ZoneId fuseau() {
                return paris;
            }
        };
    }
}
