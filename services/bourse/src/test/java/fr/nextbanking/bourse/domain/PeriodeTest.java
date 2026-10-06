package fr.nextbanking.bourse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PeriodeTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);

    @Test
    @Tag("FR-009-08")
    @Tag("FR-029-01")
    void chaque_code_designe_sa_serie_et_sa_date_limite() {
        assertThat(Periode.depuis("1W")).isEqualTo(Periode.SEMAINE);
        assertThat(Periode.depuis("1M")).isEqualTo(Periode.MOIS);
        assertThat(Periode.depuis("1Y")).isEqualTo(Periode.AN);
        assertThat(Periode.depuis("5Y")).isEqualTo(Periode.CINQ_ANS);

        assertThat(Periode.SEMAINE.serie()).isEqualTo(TypeSerie.JOURNALIERE);
        assertThat(Periode.MOIS.serie()).isEqualTo(TypeSerie.JOURNALIERE);
        assertThat(Periode.AN.serie()).isEqualTo(TypeSerie.HEBDOMADAIRE);
        assertThat(Periode.CINQ_ANS.serie()).isEqualTo(TypeSerie.HEBDOMADAIRE);

        assertThat(Periode.SEMAINE.dateLimite(AUJOURDHUI)).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(Periode.MOIS.dateLimite(AUJOURDHUI)).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(Periode.AN.dateLimite(AUJOURDHUI)).isEqualTo(LocalDate.of(2025, 11, 17));
        assertThat(Periode.CINQ_ANS.dateLimite(AUJOURDHUI)).isEqualTo(LocalDate.of(2021, 11, 17));

        assertThat(Periode.SEMAINE.code()).isEqualTo("1W");
        assertThat(Periode.CINQ_ANS.code()).isEqualTo("5Y");
    }

    @Test
    @Tag("FR-029-01")
    void une_periode_absente_ou_inconnue_vaut_un_mois_en_serie_journaliere() {
        for (String code : new String[] {null, "", "XX", "1m", "6M"}) {
            assertThat(Periode.depuis(code)).as(code).isEqualTo(Periode.MOIS);
        }
    }

    @Test
    @Tag("FR-029-02")
    void exclut_les_points_strictement_anterieurs_a_la_limite_et_trie_par_date_croissante() {
        PointCotation limite = point(LocalDate.of(2026, 10, 17));
        PointCotation avant = point(LocalDate.of(2026, 10, 16));
        PointCotation recent = point(LocalDate.of(2026, 11, 17));
        PointCotation milieu = point(LocalDate.of(2026, 11, 2));

        assertThat(Periode.MOIS.retenir(List.of(recent, avant, limite, milieu), AUJOURDHUI))
                .containsExactly(limite, milieu, recent);
        assertThat(Periode.MOIS.retenir(List.of(), AUJOURDHUI)).isEmpty();
    }

    @Test
    void l_historique_garde_une_copie_des_points() {
        java.util.ArrayList<PointCotation> points = new java.util.ArrayList<>(List.of(point(AUJOURDHUI)));
        HistoriqueCours historique = new HistoriqueCours("AI.PA", Periode.MOIS, points);
        points.clear();

        assertThat(historique.points()).hasSize(1);
        assertThat(new SymboleManquant()).hasMessage("Symbole manquant");
    }

    private static PointCotation point(LocalDate date) {
        return new PointCotation(date, BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, BigDecimal.TWO, 1L);
    }
}
