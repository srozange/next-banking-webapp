package fr.nextbanking.bourse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.application.port.out.Horloge;
import fr.nextbanking.bourse.domain.HistoriqueCours;
import fr.nextbanking.bourse.domain.Periode;
import fr.nextbanking.bourse.domain.PointCotation;
import fr.nextbanking.bourse.domain.SymboleManquant;
import fr.nextbanking.bourse.domain.TypeSerie;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterHistoriqueTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);

    private final List<String> demandes = new ArrayList<>();
    private final List<PointCotation> serie = List.of(
            point(LocalDate.of(2026, 11, 17)), point(LocalDate.of(2021, 11, 16)), point(LocalDate.of(2026, 11, 10)),
            point(LocalDate.of(2026, 11, 9)), point(LocalDate.of(2026, 10, 17)), point(LocalDate.of(2025, 11, 17)));
    private final Cotations cotations = (symbole, type) -> {
        demandes.add(symbole + " " + type);
        return symbole.equals("INCONNU.XX") ? List.of() : serie;
    };
    private final ConsulterHistoriqueCasUsage casUsage = new ConsulterHistoriqueCasUsage(cotations, new Horloge() {
        @Override
        public Instant instant() {
            return Instant.parse("2026-11-17T09:00:00Z");
        }

        @Override
        public LocalDate aujourdhui() {
            return AUJOURDHUI;
        }

        @Override
        public ZoneId fuseau() {
            return ZoneId.of("Europe/Paris");
        }
    });

    @Test
    @Tag("FR-009-08")
    @Tag("FR-029-01")
    @Tag("FR-029-02")
    void demande_la_serie_de_la_periode_et_garde_les_points_depuis_la_date_limite() {
        assertThat(dates(casUsage.consulter("AI.PA", "1W"))).containsExactly("2026-11-10", "2026-11-17");
        assertThat(dates(casUsage.consulter("AI.PA", "1M")))
                .containsExactly("2026-10-17", "2026-11-09", "2026-11-10", "2026-11-17");
        assertThat(dates(casUsage.consulter("AI.PA", "1Y")))
                .containsExactly("2025-11-17", "2026-10-17", "2026-11-09", "2026-11-10", "2026-11-17");
        assertThat(dates(casUsage.consulter("AI.PA", "5Y"))).hasSize(5).doesNotContain("2021-11-16");

        assertThat(demandes).containsExactly("AI.PA JOURNALIERE", "AI.PA JOURNALIERE", "AI.PA HEBDOMADAIRE",
                "AI.PA HEBDOMADAIRE");
    }

    @Test
    @Tag("FR-029-01")
    void une_periode_absente_ou_inconnue_vaut_un_mois_journalier() {
        HistoriqueCours inconnue = casUsage.consulter("AI.PA", "XX");
        HistoriqueCours absente = casUsage.consulter("AI.PA", null);

        assertThat(inconnue.periode()).isEqualTo(Periode.MOIS);
        assertThat(absente.periode()).isEqualTo(Periode.MOIS);
        assertThat(dates(inconnue)).first().isEqualTo("2026-10-17");
        assertThat(demandes).containsExactly("AI.PA JOURNALIERE", "AI.PA JOURNALIERE");
    }

    @Test
    @Tag("FR-009-09")
    void un_symbole_sans_donnees_rend_une_liste_vide() {
        HistoriqueCours historique = casUsage.consulter("INCONNU.XX", "1M");

        assertThat(historique.symbole()).isEqualTo("INCONNU.XX");
        assertThat(historique.points()).isEmpty();
    }

    @Test
    void le_symbole_est_rogne_sans_changer_sa_casse() {
        assertThat(casUsage.consulter("  ai.pa ", "1M").symbole()).isEqualTo("ai.pa");
        assertThat(demandes).containsExactly("ai.pa JOURNALIERE");
    }

    @Test
    void un_symbole_absent_ou_blanc_est_refuse_sans_appeler_le_fournisseur() {
        assertThatThrownBy(() -> casUsage.consulter(null, "1M")).isInstanceOf(SymboleManquant.class);
        assertThatThrownBy(() -> casUsage.consulter("", "1M")).isInstanceOf(SymboleManquant.class);
        assertThatThrownBy(() -> casUsage.consulter("   ", "1M")).isInstanceOf(SymboleManquant.class);
        assertThat(demandes).isEmpty();
    }

    private static List<String> dates(HistoriqueCours historique) {
        return historique.points().stream().map(point -> point.date().toString()).toList();
    }

    private static PointCotation point(LocalDate date) {
        return new PointCotation(date, new BigDecimal("1.0000"), new BigDecimal("2.0000"), new BigDecimal("0.5000"),
                new BigDecimal("1.5000"), 10L);
    }
}
