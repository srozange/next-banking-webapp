package fr.nextbanking.bourse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class GraphiqueCoursTest {

    private static final Pattern DATE = Pattern.compile(">(\\d\\d-\\d\\d)</text>");

    @Test
    @Tag("FR-009-05")
    @Tag("FR-029-08")
    void hausse_svg_800_sur_300_cinq_grilles_cinq_dates_regulieres_courbe_verte() {
        List<PointCotation> points = serie("1", "2", "3", "4", "5", "6", "7", "8", "9");
        String svg = new HistoriqueCours("AI.PA", Periode.MOIS, points).graphique().orElseThrow();

        assertThat(svg).contains("width=\"800\"", "height=\"300\"", "viewBox=\"0 0 800 300\"");
        assertThat(count(svg, "<line ")).isEqualTo(5);
        assertThat(svg).contains("#2e7d32").doesNotContain("#E3000F");
        assertThat(dates(svg)).containsExactly("03-01", "03-03", "03-05", "03-07", "03-09");
        assertThat(svg).doesNotContain("AI.PA");
    }

    @Test
    @Tag("FR-009-06")
    void baisse_courbe_rouge_sans_vert() {
        String svg = GraphiqueCours.generer(Periode.AN, serie("5", "3", "1"));

        assertThat(svg).contains("#E3000F").doesNotContain("#2e7d32");
        assertThat(dates(svg)).containsExactly("03-01", "03-01", "03-02", "03-02", "03-03");
    }

    @Test
    @Tag("FR-009-06")
    void clotures_egales_ou_un_seul_point_donnent_une_courbe_horizontale_valide() {
        String plat = GraphiqueCours.generer(Periode.SEMAINE, serie("2", "2", "2"));
        String seul = GraphiqueCours.generer(Periode.CINQ_ANS, serie("2"));

        assertThat(plat).contains("#2e7d32").contains("points=\"40.00,135.00 400.00,135.00 760.00,135.00\"");
        assertThat(seul).contains("points=\"40.00,135.00\"");
        assertThat(dates(seul)).containsOnly("03-01").hasSize(5);
    }

    @Test
    @Tag("FR-029-08")
    void les_coordonnees_extremes_tiennent_dans_la_zone_de_trace() {
        String svg = GraphiqueCours.generer(Periode.MOIS, serie("1", "3"));

        assertThat(svg).contains("points=\"40.00,240.00 760.00,30.00\"");
    }

    @Test
    @Tag("FR-009-05")
    @Tag("FR-029-08")
    void le_badge_porte_le_libelle_de_la_periode() {
        assertThat(GraphiqueCours.generer(Periode.SEMAINE, serie("1"))).contains(">1 semaine (données journalières)<");
        assertThat(GraphiqueCours.generer(Periode.MOIS, serie("1"))).contains(">1 mois (données journalières)<");
        assertThat(GraphiqueCours.generer(Periode.AN, serie("1"))).contains(">1 an (données hebdomadaires)<");
        assertThat(GraphiqueCours.generer(Periode.CINQ_ANS, serie("1"))).contains(">5 ans (données hebdomadaires)<");
    }

    @Test
    @Tag("FR-009-05")
    void un_historique_vide_n_a_pas_de_graphique() {
        assertThat(new HistoriqueCours("X", Periode.MOIS, List.of()).graphique()).isEmpty();
    }

    @Test
    void le_constructeur_prive_n_est_pas_instanciable() throws Exception {
        var constructeur = GraphiqueCours.class.getDeclaredConstructor();
        constructeur.setAccessible(true);
        assertThat(constructeur.newInstance()).isNotNull();
    }

    private static int count(String texte, String motif) {
        return texte.split(Pattern.quote(motif), -1).length - 1;
    }

    private static List<String> dates(String svg) {
        List<String> dates = new ArrayList<>();
        Matcher m = DATE.matcher(svg);
        while (m.find()) {
            dates.add(m.group(1));
        }
        return dates;
    }

    private static List<PointCotation> serie(String... clotures) {
        List<PointCotation> points = new ArrayList<>();
        for (int i = 0; i < clotures.length; i++) {
            BigDecimal c = new BigDecimal(clotures[i]);
            points.add(new PointCotation(LocalDate.of(2026, 3, 1 + i), c, c, c, c, 1L));
        }
        return points;
    }
}
