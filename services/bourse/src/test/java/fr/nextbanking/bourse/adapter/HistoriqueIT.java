package fr.nextbanking.bourse.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.bourse.adapter.out.fournisseur.AlphaVantageSimule;
import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.infrastructure.ApplicationBourse;
import fr.nextbanking.bourse.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Historique des cours servi par l'adaptateur simulé (actif sans aucune propriété {@code nb.fournisseurs.*}), avec
 * une horloge fixée au 2026-11-17.
 */
@SpringBootTest(classes = ApplicationBourse.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(HorlogeFixe.class)
@DirtiesContext
class HistoriqueIT {

    private static final String CHEMIN = "/api/bourse/historique";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private Cotations cotations;

    @Test
    @Tag("FR-029-05")
    void sans_reglage_le_fournisseur_est_simule() {
        assertThat(cotations).isInstanceOf(AlphaVantageSimule.class);
    }

    @Test
    @Tag("FR-009-01")
    @Tag("FR-009-08")
    @Tag("FR-029-01")
    @Tag("FR-029-02")
    void les_trois_roles_lisent_le_meme_mois_journalier_trie_avec_des_prix_en_chaine() throws Exception {
        String reference = null;
        for (String role : new String[] {"CLIENT", "CONSEILLER", "ADMIN"}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN + "?symbole=AI.PA&periode=1M",
                    jeton(role), null);
            assertThat(reponse.statusCode()).as(role).isEqualTo(200);
            reference = reference == null ? reponse.body() : reference;
            assertThat(reponse.body()).as(role).isEqualTo(reference);
        }

        JsonNode historique = JSON.readTree(reference);
        assertThat(historique.get("symbole").asString()).isEqualTo("AI.PA");
        assertThat(historique.get("periode").asString()).isEqualTo("1M");
        List<LocalDate> dates = dates(historique);
        assertThat(dates.getFirst()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(dates.getLast()).isEqualTo(HorlogeFixe.AUJOURDHUI);
        assertThat(dates).isSorted().doesNotHaveDuplicates();
        for (JsonNode point : historique.get("points")) {
            assertThat(point.get("date").isString()).isTrue();
            for (String prix : new String[] {"ouverture", "haut", "bas", "cloture"}) {
                assertThat(point.get(prix).isString()).as(prix).isTrue();
                assertThat(point.get(prix).asString()).as(prix).matches("\\d+\\.\\d{4}");
            }
            assertThat(point.get("volume").isIntegralNumber()).isTrue();
        }
        assertThat(reference).contains("\"date\":\"2026-10-19\",\"ouverture\":\"");
    }

    @Test
    @Tag("FR-009-08")
    @Tag("FR-029-01")
    @Tag("FR-029-02")
    void chaque_periode_lit_sa_serie_depuis_sa_date_limite() throws Exception {
        List<LocalDate> semaine = dates(historique("AI.PA", "1W"));
        assertThat(semaine.getFirst()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(semaine).hasSize(6).isSorted();

        List<LocalDate> an = dates(historique("MSFT", "1Y"));
        assertThat(an.getFirst()).isEqualTo(LocalDate.of(2025, 11, 21));
        assertThat(an.getLast()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(an).isSorted().allSatisfy(date -> assertThat(date.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY));

        List<LocalDate> cinqAns = dates(historique("MSFT", "5Y"));
        assertThat(cinqAns.getFirst()).isEqualTo(LocalDate.of(2021, 11, 19));
        assertThat(cinqAns).isSorted().allSatisfy(date -> assertThat(date.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY));
    }

    @Test
    @Tag("FR-029-01")
    void une_periode_absente_ou_inconnue_vaut_un_mois_journalier() throws Exception {
        JsonNode mois = historique("BNP.PA", "1M");
        for (String requete : new String[] {"?symbole=BNP.PA&periode=XX", "?symbole=BNP.PA", "?symbole=BNP.PA&periode="}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN + requete, jeton("CLIENT"), null);
            assertThat(reponse.statusCode()).as(requete).isEqualTo(200);
            assertThat(JSON.readTree(reponse.body())).as(requete).isEqualTo(mois);
        }
    }

    @Test
    @Tag("FR-009-09")
    void un_symbole_inconnu_rend_une_liste_vide_et_le_symbole_est_rogne_sans_changer_sa_casse() throws Exception {
        assertThat(Jetons.envoyer(port, "GET", CHEMIN + "?symbole=INCONNU.XX&periode=1M", jeton("CLIENT"), null)
                .body()).isEqualTo("{\"symbole\":\"INCONNU.XX\",\"periode\":\"1M\",\"points\":[]}");
        assertThat(Jetons.envoyer(port, "GET", CHEMIN + "?symbole=%20ai.pa%20&periode=1W", jeton("CLIENT"), null)
                .body()).isEqualTo("{\"symbole\":\"ai.pa\",\"periode\":\"1W\",\"points\":[]}");
    }

    @Test
    void un_symbole_absent_ou_blanc_rend_400() throws Exception {
        for (String requete : new String[] {"", "?periode=1M", "?symbole=&periode=1M", "?symbole=%20%20"}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN + requete, jeton("CLIENT"), null);
            assertThat(reponse.statusCode()).as(requete).isEqualTo(400);
            assertThat(reponse.body()).as(requete).isEqualTo("{\"message\":\"Requête invalide\"}");
        }
    }

    @Test
    @Tag("FR-009-01")
    void sans_jeton_l_historique_rend_401() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN + "?symbole=AI.PA", null, null);

        assertThat(reponse.statusCode()).isEqualTo(401);
    }

    @Test
    @Tag("FR-009-05")
    @Tag("FR-009-06")
    @Tag("FR-029-08")
    void la_reponse_porte_le_graphique_svg_de_la_periode() throws Exception {
        JsonNode historique = historique("AI.PA", "1M");
        String svg = historique.get("graphique").asString();
        JsonNode points = historique.get("points");
        boolean hausse = new java.math.BigDecimal(points.get(points.size() - 1).get("cloture").asString())
                .compareTo(new java.math.BigDecimal(points.get(0).get("cloture").asString())) >= 0;

        assertThat(svg).contains("width=\"800\"", "height=\"300\"", "viewBox=\"0 0 800 300\"");
        assertThat(svg.split("<line ", -1)).hasSize(6);
        assertThat(svg).contains("1 mois (données journalières)");
        assertThat(svg.split("text-anchor=\"middle\"", -1)).hasSize(6);
        assertThat(svg).contains(hausse ? "#2e7d32" : "#E3000F").doesNotContain(hausse ? "#E3000F" : "#2e7d32");
    }

    @Test
    @Tag("FR-009-05")
    void un_historique_vide_n_a_pas_de_graphique() throws Exception {
        assertThat(historique("INCONNU.XX", "1M").has("graphique")).isFalse();
    }

    @Test
    @Tag("FR-029-08")
    void vingt_appels_sur_toutes_les_periodes_restent_sous_trois_secondes_au_95e_centile() throws Exception {
        String[] periodes = {"1W", "1M", "1Y", "5Y"};
        List<Long> durees = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long debut = System.nanoTime();
            historique("AI.PA", periodes[i % periodes.length]);
            durees.add((System.nanoTime() - debut) / 1_000_000);
        }
        java.util.Collections.sort(durees);

        assertThat(durees.get(18)).isLessThan(3_000L);
    }

    private JsonNode historique(String symbole, String periode) throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "GET",
                CHEMIN + "?symbole=" + symbole + "&periode=" + periode, jeton("CLIENT"), null);
        assertThat(reponse.statusCode()).isEqualTo(200);
        return JSON.readTree(reponse.body());
    }

    static List<LocalDate> dates(JsonNode historique) {
        List<LocalDate> dates = new ArrayList<>();
        historique.get("points").forEach(point -> dates.add(LocalDate.parse(point.get("date").asString())));
        return dates;
    }

    static String jeton(String role) {
        Instant maintenant = Instant.now();
        return Jetons.jeton(Conteneurs.CLES, role, maintenant, maintenant.plusSeconds(1800));
    }
}
