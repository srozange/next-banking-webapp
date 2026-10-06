package fr.nextbanking.bourse.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.bourse.adapter.out.fournisseur.BouchonAlphaVantage;
import fr.nextbanking.bourse.adapter.out.fournisseur.ParametresDeploiement;
import fr.nextbanking.bourse.infrastructure.ApplicationBourse;
import fr.nextbanking.bourse.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Mode réel contre un bouchon local (aucun réseau, aucune clé réelle) : adresse et requêtes émises, cache, échecs du
 * fournisseur et délai rendus en 200 liste vide avec un journal tracé, coupe-circuit. Le coupe-circuit est éprouvé en
 * dernier : ouvert, il le reste pour la suite du contexte.
 */
@SpringBootTest(classes = ApplicationBourse.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"nb.fournisseurs.alphavantage.mode=reel", "nb.fournisseurs.alphavantage.delai-attente=500ms",
                "nb.fournisseurs.alphavantage.seuil-echecs=3", "nb.fournisseurs.alphavantage.duree-ouverture=10m"})
@Import({HorlogeFixe.class, AlphaVantageReelIT.Parametres.class})
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlphaVantageReelIT {

    static final String CLE = "cle-de-test";
    static final BouchonAlphaVantage BOUCHON = new BouchonAlphaVantage();
    private static final Pattern TRACE = Pattern.compile("traceId=(\\S*) spanId=(\\S*)\\]");
    private static final String VIDE_1M = "{\"symbole\":\"%s\",\"periode\":\"1M\",\"points\":[]}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @AfterAll
    static void arreter() {
        BOUCHON.close();
    }

    @LocalServerPort
    private int port;

    @Test
    @Order(1)
    @Tag("FR-009-08")
    @Tag("FR-029-01")
    @Tag("FR-029-02")
    @Tag("FR-029-04")
    void un_mois_puis_un_mois_puis_une_semaine_n_emettent_qu_une_requete_a_l_adresse_configuree() throws Exception {
        BOUCHON.repondre(200, BouchonAlphaVantage.SERIE_JOURNALIERE);
        int avant = BOUCHON.nombreDeRequetes();

        HttpResponse<String> mois = historique("AI.PA", "1M");
        assertThat(mois.statusCode()).isEqualTo(200);
        assertThat(mois.body()).startsWith("{\"symbole\":\"AI.PA\",\"periode\":\"1M\",\"points\":["
                + "{\"date\":\"2026-10-19\",\"ouverture\":\"170.1000\",\"haut\":\"171.0000\",\"bas\":\"169.9000\","
                + "\"cloture\":\"170.7700\",\"volume\":987654},"
                + "{\"date\":\"2026-11-17\",\"ouverture\":\"175.2000\",\"haut\":\"176.0500\",\"bas\":\"174.1000\","
                + "\"cloture\":\"175.9000\",\"volume\":1234567}],\"graphique\":\"<svg ");
        assertThat(historique("AI.PA", "1M").body()).isEqualTo(mois.body());
        assertThat(historique("AI.PA", "1W").body()).contains("\"periode\":\"1W\"", "2026-11-17")
                .doesNotContain("2026-10-19");

        assertThat(BOUCHON.nombreDeRequetes()).isEqualTo(avant + 1);
        assertThat(BOUCHON.requetes().getLast().split("&")).containsExactlyInAnyOrder(
                "function=TIME_SERIES_DAILY", "symbol=AI.PA", "outputsize=compact", "apikey=" + CLE);
    }

    @Test
    @Order(2)
    @Tag("FR-029-01")
    void un_an_demande_la_serie_hebdomadaire() throws Exception {
        BOUCHON.repondre(200, BouchonAlphaVantage.SERIE_HEBDOMADAIRE);

        assertThat(historique("AI.PA", "1Y").body()).contains("\"periode\":\"1Y\"", "2026-11-13")
                .doesNotContain("2025-11-14");
        assertThat(BOUCHON.requetes().getLast().split("&")).containsExactlyInAnyOrder(
                "function=TIME_SERIES_WEEKLY", "symbol=AI.PA", "apikey=" + CLE);
    }

    @Test
    @Order(3)
    @Tag("FR-009-09")
    @Tag("FR-029-03")
    void chaque_echec_du_fournisseur_rend_200_vide_dans_le_delai_et_un_journal_trace(CapturedOutput sortie)
            throws Exception {
        Map<String, Runnable> echecs = new java.util.LinkedHashMap<>();
        echecs.put("INFO.PA", () -> BOUCHON.repondre(200, "{\"Information\":\"rate limit\"}"));
        echecs.put("NOTE.PA", () -> BOUCHON.repondre(200, "{\"Note\":\"call frequency\"}"));
        echecs.put("ERREUR.PA", () -> BOUCHON.repondre(200, "{\"Error Message\":\"Invalid API call\"}"));
        echecs.put("STATUT.PA", () -> BOUCHON.repondre(500, "{}"));
        echecs.put("ILLISIBLE.PA", () -> BOUCHON.repondre(200, "<html>"));
        echecs.put("LENT.PA", () -> BOUCHON.repondreApres(Duration.ofSeconds(3), BouchonAlphaVantage.SERIE_JOURNALIERE));
        int succes = 0;

        for (var echec : echecs.entrySet()) {
            echec.getValue().run();
            long debut = System.nanoTime();
            HttpResponse<String> reponse = historique(echec.getKey(), "1M");
            assertThat(Duration.ofNanos(System.nanoTime() - debut)).as(echec.getKey()).isLessThan(Duration.ofSeconds(2));
            assertThat(reponse.statusCode()).as(echec.getKey()).isEqualTo(200);
            assertThat(reponse.body()).as(echec.getKey()).isEqualTo(VIDE_1M.formatted(echec.getKey()));
            // Un succès entre deux échecs : le coupe-circuit (3 échecs consécutifs) reste fermé.
            BOUCHON.repondre(200, BouchonAlphaVantage.SERIE_JOURNALIERE);
            assertThat(historique("SUCCES" + succes++, "1M").body()).contains("2026-11-17");
        }

        List<String> lignes = Arrays.stream(sortie.getOut().split("\\R"))
                .filter(ligne -> ligne.contains("Cotations AlphaVantage indisponibles")).toList();
        assertThat(lignes).hasSize(echecs.size()).allSatisfy(ligne -> {
            assertThat(ligne).containsAnyOf(" WARN ", " ERROR ").doesNotContain(CLE);
            Matcher trace = TRACE.matcher(ligne);
            assertThat(trace.find()).as(ligne).isTrue();
            assertThat(trace.group(1)).as(ligne).isNotEmpty();
        });
        assertThat(String.join("\n", lignes)).contains("champ Information", "champ Note", "champ Error Message",
                "statut 500", "corps illisible", "délai dépassé");
        assertThat(sortie.getOut()).doesNotContain(CLE);
    }

    @Test
    @Order(4)
    @Tag("FR-029-03")
    void le_coupe_circuit_ouvert_n_appelle_plus_le_fournisseur_et_rend_200_vide() throws Exception {
        BOUCHON.repondre(200, BouchonAlphaVantage.SERIE_JOURNALIERE);
        assertThat(historique("CACHE.PA", "1M").body()).contains("2026-11-17");
        BOUCHON.repondre(500, "{}");
        int avant = BOUCHON.nombreDeRequetes();
        for (String symbole : new String[] {"PANNE1", "PANNE2", "PANNE3"}) {
            assertThat(historique(symbole, "1M").body()).isEqualTo(VIDE_1M.formatted(symbole));
        }
        assertThat(BOUCHON.nombreDeRequetes()).isEqualTo(avant + 3);

        BOUCHON.repondre(200, BouchonAlphaVantage.SERIE_JOURNALIERE);
        for (String symbole : new String[] {"BNP.PA", "MC.PA"}) {
            HttpResponse<String> reponse = historique(symbole, "1M");
            assertThat(reponse.statusCode()).isEqualTo(200);
            assertThat(reponse.body()).isEqualTo(VIDE_1M.formatted(symbole));
        }
        assertThat(BOUCHON.nombreDeRequetes()).isEqualTo(avant + 3);
        // Une série déjà en cache reste servie.
        assertThat(historique("CACHE.PA", "1W").body()).contains("2026-11-17");
    }

    private HttpResponse<String> historique(String symbole, String periode) throws Exception {
        return Jetons.envoyer(port, "GET", "/api/bourse/historique?symbole=" + symbole + "&periode=" + periode,
                HistoriqueIT.jeton("CLIENT"), null);
    }

    /** Clé de test et adresse du bouchon, à la place de l'environnement du processus. */
    @TestConfiguration
    static class Parametres {

        @Bean
        @Primary
        ParametresDeploiement parametresDuBouchon() {
            Map<String, String> valeurs = Map.of("ALPHA_VANTAGE_API_KEY", CLE,
                    "ALPHA_VANTAGE_BASE_URL", BOUCHON.adresse());
            return new ParametresDeploiement(valeurs::get, nom -> null);
        }
    }
}
