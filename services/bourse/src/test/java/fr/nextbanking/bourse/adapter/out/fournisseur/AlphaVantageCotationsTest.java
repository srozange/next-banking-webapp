package fr.nextbanking.bourse.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.domain.PointCotation;
import fr.nextbanking.bourse.domain.TypeSerie;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur réel contre un bouchon local : requêtes émises, lecture des séries, échecs, délai, coupe-circuit, cache.
 */
@ExtendWith(OutputCaptureExtension.class)
class AlphaVantageCotationsTest {

    private static final String CLE = "cle-de-test";

    private final BouchonAlphaVantage bouchon = new BouchonAlphaVantage();

    @AfterEach
    void arreter() {
        bouchon.close();
    }

    @Test
    @Tag("FR-009-08")
    @Tag("FR-029-01")
    @Tag("FR-029-04")
    void demande_la_serie_journaliere_compacte_a_l_adresse_configuree_et_lit_ses_points() {
        var points = reel(5, Duration.ofSeconds(2)).serie("AI.PA", TypeSerie.JOURNALIERE);

        assertThat(bouchon.requetes()).singleElement().satisfies(requete -> assertThat(requete.split("&"))
                .containsExactlyInAnyOrder("function=TIME_SERIES_DAILY", "symbol=AI.PA", "outputsize=compact",
                        "apikey=" + CLE));
        assertThat(points).hasSize(3).contains(new PointCotation(LocalDate.of(2026, 11, 17),
                new BigDecimal("175.2000"), new BigDecimal("176.0500"), new BigDecimal("174.1000"),
                new BigDecimal("175.9000"), 1_234_567L));
    }

    @Test
    @Tag("FR-029-01")
    void demande_la_serie_hebdomadaire_sans_taille_et_encode_le_symbole() {
        bouchon.repondre(200, BouchonAlphaVantage.SERIE_HEBDOMADAIRE);

        var points = reel(5, Duration.ofSeconds(2)).serie("A&B C", TypeSerie.HEBDOMADAIRE);

        assertThat(points).extracting(PointCotation::date)
                .containsExactlyInAnyOrder(LocalDate.of(2026, 11, 13), LocalDate.of(2025, 11, 14));
        assertThat(bouchon.requetes().getFirst().split("&")).containsExactlyInAnyOrder(
                "function=TIME_SERIES_WEEKLY", "symbol=A%26B%20C", "apikey=" + CLE);
    }

    @Test
    @Tag("FR-009-09")
    @Tag("FR-029-03")
    void toute_reponse_en_echec_rend_une_liste_vide_et_est_journalisee_sans_la_cle(CapturedOutput sortie) {
        Map<String, String[]> echecs = Map.of(
                "Information", new String[] {"200", "{\"Information\":\"Thank you for using Alpha Vantage\"}"},
                "Note", new String[] {"200", "{\"Note\":\"call frequency\"}"},
                "Error Message", new String[] {"200", "{\"Error Message\":\"Invalid API call\"}"},
                "statut 500", new String[] {"500", "{}"},
                "statut 404", new String[] {"404", BouchonAlphaVantage.SERIE_JOURNALIERE},
                "statut 302", new String[] {"302", BouchonAlphaVantage.SERIE_JOURNALIERE},
                "pas du JSON", new String[] {"200", "<html>erreur</html>"},
                "tableau", new String[] {"200", "[]"},
                "série non objet", new String[] {"200", "{\"Time Series (Daily)\":[1]}"},
                "prix illisible", new String[] {"200", "{\"Time Series (Daily)\":{\"2026-11-17\":{\"1. open\":\"x\"}}}"});
        Cotations cotations = reel(100, Duration.ofSeconds(2));

        for (var echec : echecs.entrySet()) {
            bouchon.repondre(Integer.parseInt(echec.getValue()[0]), echec.getValue()[1]);
            assertThat(cotations.serie("AI.PA", TypeSerie.JOURNALIERE)).as(echec.getKey()).isEmpty();
        }
        bouchon.repondre(200, "{\"Time Series (Daily)\":{\"17/11/2026\":{\"1. open\":\"1\",\"2. high\":\"1\","
                + "\"3. low\":\"1\",\"4. close\":\"1\",\"5. volume\":\"1\"}}}");
        assertThat(cotations.serie("AI.PA", TypeSerie.JOURNALIERE)).as("date illisible").isEmpty();

        assertThat(bouchon.nombreDeRequetes()).isEqualTo(echecs.size() + 1);
        assertThat(sortie.getOut()).contains("WARN", "champ Information", "champ Note", "champ Error Message",
                "statut 500", "corps illisible").doesNotContain(CLE);
    }

    @Test
    @Tag("FR-029-03")
    void une_reponse_apres_le_delai_rend_une_liste_vide_dans_le_delai(CapturedOutput sortie) {
        bouchon.repondreApres(Duration.ofSeconds(3), BouchonAlphaVantage.SERIE_JOURNALIERE);
        Cotations cotations = reel(5, Duration.ofMillis(300));

        long debut = System.nanoTime();
        assertThat(cotations.serie("AI.PA", TypeSerie.JOURNALIERE)).isEmpty();

        assertThat(Duration.ofNanos(System.nanoTime() - debut)).isLessThan(Duration.ofSeconds(2));
        assertThat(sortie.getOut()).contains("ERROR", "délai dépassé").doesNotContain(CLE);
    }

    @Test
    @Tag("FR-029-03")
    void une_adresse_injoignable_rend_une_liste_vide(CapturedOutput sortie) {
        Cotations cotations = new AlphaVantageCotations(RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory()).build(), "http://localhost:1/query",
                Optional.of(CLE), ProprietesAlphaVantage.parDefaut());

        assertThat(cotations.serie("AI.PA", TypeSerie.JOURNALIERE)).isEmpty();
        assertThat(sortie.getOut()).contains("erreur d'appel").doesNotContain(CLE);
    }

    @Test
    @Tag("FR-029-03")
    void le_coupe_circuit_s_ouvre_apres_le_seuil_d_echecs_consecutifs(CapturedOutput sortie) {
        Cotations cotations = reel(3, Duration.ofSeconds(2));
        bouchon.repondre(500, "{}");
        cotations.serie("A", TypeSerie.JOURNALIERE);
        bouchon.repondre(200, "{\"Note\":\"limite\"}");
        cotations.serie("B", TypeSerie.JOURNALIERE);
        bouchon.repondre(200, "{\"Information\":\"limite\"}");
        cotations.serie("C", TypeSerie.HEBDOMADAIRE);
        assertThat(bouchon.nombreDeRequetes()).isEqualTo(3);

        bouchon.repondre(200, BouchonAlphaVantage.SERIE_JOURNALIERE);
        assertThat(cotations.serie("AI.PA", TypeSerie.JOURNALIERE)).isEmpty();
        assertThat(cotations.serie("MSFT", TypeSerie.HEBDOMADAIRE)).isEmpty();

        assertThat(bouchon.nombreDeRequetes()).isEqualTo(3);
        assertThat(sortie.getOut()).contains("coupe-circuit ouvert");
    }

    @Test
    @Tag("FR-029-03")
    void un_echec_isole_ne_compte_plus_apres_un_succes() {
        Cotations cotations = reel(2, Duration.ofSeconds(2));
        for (int tour = 0; tour < 3; tour++) {
            bouchon.repondre(500, "{}");
            cotations.serie("ECHEC" + tour, TypeSerie.JOURNALIERE);
            bouchon.repondre(200, BouchonAlphaVantage.SERIE_JOURNALIERE);
            assertThat(cotations.serie("SUCCES" + tour, TypeSerie.JOURNALIERE)).isNotEmpty();
        }
        assertThat(bouchon.nombreDeRequetes()).isEqualTo(6);
    }

    @Test
    @Tag("FR-029-03")
    void error_message_et_serie_vide_ne_sont_pas_des_echecs_du_coupe_circuit_ni_mis_en_cache() {
        Cotations cotations = reel(2, Duration.ofSeconds(2));
        bouchon.repondre(200, "{\"Error Message\":\"Invalid API call\"}");
        for (int tour = 0; tour < 3; tour++) {
            assertThat(cotations.serie("INCONNU.XX", TypeSerie.JOURNALIERE)).isEmpty();
        }
        bouchon.repondre(200, "{\"Time Series (Daily)\":{}}");
        for (int tour = 0; tour < 3; tour++) {
            assertThat(cotations.serie("VIDE", TypeSerie.JOURNALIERE)).isEmpty();
        }
        bouchon.repondre(200, "{\"Meta Data\":{}}");
        assertThat(cotations.serie("SANS.SERIE", TypeSerie.JOURNALIERE)).isEmpty();

        assertThat(bouchon.nombreDeRequetes()).isEqualTo(7);
    }

    @Test
    @Tag("FR-009-08")
    void une_serie_reussie_est_servie_par_le_cache_par_symbole_et_type_pendant_sa_duree() {
        AtomicLong nanos = new AtomicLong();
        Cotations cotations = new AlphaVantageCotations(RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory()).build(), bouchon.adresse(), Optional.of(CLE),
                new ProprietesAlphaVantage("reel", null, null, null, Duration.ofMinutes(10)), nanos::get);

        var premiere = cotations.serie("AI.PA", TypeSerie.JOURNALIERE);
        assertThat(cotations.serie("AI.PA", TypeSerie.JOURNALIERE)).isEqualTo(premiere);
        assertThat(bouchon.nombreDeRequetes()).isEqualTo(1);

        cotations.serie("BNP.PA", TypeSerie.JOURNALIERE);
        bouchon.repondre(200, BouchonAlphaVantage.SERIE_HEBDOMADAIRE);
        cotations.serie("AI.PA", TypeSerie.HEBDOMADAIRE);
        assertThat(bouchon.nombreDeRequetes()).isEqualTo(3);

        nanos.addAndGet(Duration.ofMinutes(11).toNanos());
        cotations.serie("AI.PA", TypeSerie.JOURNALIERE);
        assertThat(bouchon.nombreDeRequetes()).isEqualTo(4);
    }

    @Test
    @Tag("FR-029-05")
    @Tag("FR-009-14")
    @Tag("BUG-091")
    @Tag("BUG-034")
    void sans_cle_aucune_requete_n_est_emise(CapturedOutput sortie) {
        Cotations cotations = new ConfigurationAlphaVantage().cotations(
                new ProprietesAlphaVantage("reel", null, null, null, null),
                AlphaVantageSimuleTest.horloge(LocalDate.of(2026, 11, 17)),
                new ParametresDeploiement(nom -> nom.equals(ParametresDeploiement.ADRESSE) ? bouchon.adresse() : null,
                        nom -> null),
                new StaticListableBeanFactory().getBeanProvider(RestClient.Builder.class));

        assertThat(cotations.serie("AI.PA", TypeSerie.JOURNALIERE)).isEmpty();
        assertThat(bouchon.nombreDeRequetes()).isZero();
        assertThat(sortie.getOut()).contains("WARN", "clé absente");
    }

    private Cotations reel(int seuil, Duration delai) {
        return new ConfigurationAlphaVantage().cotations(
                new ProprietesAlphaVantage("reel", delai, seuil, Duration.ofMinutes(1), null),
                AlphaVantageSimuleTest.horloge(LocalDate.of(2026, 11, 17)),
                new ParametresDeploiement(Map.of(ParametresDeploiement.ADRESSE, bouchon.adresse(),
                        ParametresDeploiement.CLE, CLE)::get, nom -> null),
                new StaticListableBeanFactory().getBeanProvider(RestClient.Builder.class));
    }
}
