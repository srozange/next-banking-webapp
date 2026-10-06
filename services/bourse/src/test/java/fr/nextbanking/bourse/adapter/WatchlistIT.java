package fr.nextbanking.bourse.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.bourse.application.port.out.DepotWatchlist;
import fr.nextbanking.bourse.domain.AjoutRefuse;
import fr.nextbanking.bourse.domain.NouvelleValeur;
import fr.nextbanking.bourse.infrastructure.ApplicationBourse;
import fr.nextbanking.bourse.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * API de la watchlist commune, sur une base chargée du {@code reference-data.sql} avant chaque test.
 */
@SpringBootTest(classes = ApplicationBourse.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class WatchlistIT {

    private static final String CHEMIN = "/api/bourse/watchlist";
    private static final List<String> REFERENCE = List.of(
            valeur(1, "AI.PA", "Air Liquide", "Euronext Paris", "EUR"),
            valeur(2, "BNP.PA", "BNP Paribas", "Euronext Paris", "EUR"),
            valeur(3, "MC.PA", "LVMH", "Euronext Paris", "EUR"),
            valeur(4, "OR.PA", "L Oreal", "Euronext Paris", "EUR"),
            valeur(5, "SAN.PA", "Sanofi", "Euronext Paris", "EUR"),
            valeur(6, "TTE.PA", "TotalEnergies", "Euronext Paris", "EUR"),
            valeur(7, "AAPL", "Apple", "NASDAQ", "USD"),
            valeur(8, "GOOGL", "Alphabet", "NASDAQ", "USD"),
            valeur(9, "MSFT", "Microsoft", "NASDAQ", "USD"));
    private static final String REQUETE_INVALIDE = "{\"message\":\"Requête invalide\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DepotWatchlist depot;

    private String jeton;

    @BeforeEach
    void chargerLesDonneesDeReference() throws Exception {
        jdbc.execute("truncate watchlist_items restart identity");
        jdbc.execute(Files.readString(Path.of("reference-data/reference-data.sql"), StandardCharsets.UTF_8));
        jeton = jeton("CLIENT");
    }

    @Test
    @Tag("FR-009-01")
    @Tag("FR-009-10")
    @Tag("FR-029-11")
    void les_trois_roles_lisent_la_meme_liste_triee_par_marche_puis_symbole() throws Exception {
        for (String role : new String[] {"CLIENT", "CONSEILLER", "ADMIN"}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET", CHEMIN, jeton(role), null);

            assertThat(reponse.statusCode()).as(role).isEqualTo(200);
            assertThat(reponse.body()).as(role).isEqualTo(liste(REFERENCE));
        }
    }

    @Test
    @Tag("FR-009-12")
    @Tag("FR-029-10")
    @Tag("FR-029-11")
    void ajoute_une_valeur_normalisee_et_la_range_par_marche_puis_symbole() throws Exception {
        HttpResponse<String> reponse = ajouter(
                "{\"symbole\":\"  sg.pa \",\"nom\":\" Société Générale \",\"marche\":\"Euronext Paris\",\"devise\":\"eur\"}");

        String creee = valeur(10, "SG.PA", "Société Générale", "Euronext Paris", "EUR");
        assertThat(reponse.statusCode()).isEqualTo(201);
        assertThat(reponse.body()).isEqualTo(creee);
        List<String> attendue = new java.util.ArrayList<>(REFERENCE);
        attendue.add(5, creee);
        assertThat(Jetons.envoyer(port, "GET", CHEMIN, jeton, null).body()).isEqualTo(liste(attendue));
    }

    @Test
    @Tag("FR-029-11")
    void trie_les_symboles_dans_l_ordre_binaire_ponctuation_comprise() throws Exception {
        assertThat(ajouter("{\"symbole\":\"SGO.PA\",\"nom\":\"Saint-Gobain\",\"marche\":\"Euronext Paris\","
                + "\"devise\":\"EUR\"}").statusCode()).isEqualTo(201);
        assertThat(ajouter("{\"symbole\":\"SG.PA\",\"nom\":\"Société Générale\",\"marche\":\"Euronext Paris\","
                + "\"devise\":\"EUR\"}").statusCode()).isEqualTo(201);

        List<String> attendue = new java.util.ArrayList<>(REFERENCE);
        attendue.add(5, valeur(11, "SG.PA", "Société Générale", "Euronext Paris", "EUR"));
        attendue.add(6, valeur(10, "SGO.PA", "Saint-Gobain", "Euronext Paris", "EUR"));
        assertThat(Jetons.envoyer(port, "GET", CHEMIN, jeton, null).body()).isEqualTo(liste(attendue));
    }

    @Test
    @Tag("FR-009-11")
    @Tag("FR-029-09")
    void refuse_un_symbole_ou_un_nom_manquant_sans_rien_enregistrer() throws Exception {
        String symboleRequis = "{\"message\":\"Le symbole est obligatoire (ex: AMZN, SG.PA)\"}";
        String nomRequis = "{\"message\":\"Le nom de la valeur est obligatoire\"}";

        refuse("{\"symbole\":\"\",\"nom\":\"Amazon\",\"marche\":\"NASDAQ\",\"devise\":\"USD\"}", symboleRequis);
        refuse("{\"symbole\":\"   \",\"nom\":\"Amazon\",\"marche\":\"NASDAQ\",\"devise\":\"USD\"}", symboleRequis);
        refuse("{\"nom\":\"Amazon\",\"marche\":\"NASDAQ\",\"devise\":\"USD\"}", symboleRequis);
        refuse("{\"symbole\":\"AMZN\",\"nom\":\"\",\"marche\":\"NASDAQ\",\"devise\":\"USD\"}", nomRequis);
        refuse("{\"symbole\":\"AMZN\",\"nom\":\"  \",\"marche\":\"NASDAQ\",\"devise\":\"USD\"}", nomRequis);
        refuse("{\"symbole\":\"AMZN\",\"marche\":\"NASDAQ\",\"devise\":\"USD\"}", nomRequis);
    }

    @Test
    @Tag("FR-009-11")
    @Tag("FR-029-09")
    void refuse_un_doublon_sans_tenir_compte_de_la_casse() throws Exception {
        refuse("{\"symbole\":\"bnp.pa\",\"nom\":\"BNP\",\"marche\":\"Euronext Paris\",\"devise\":\"EUR\"}",
                "{\"message\":\"Le symbole « BNP.PA » existe déjà dans votre watchlist\"}");
    }

    @Test
    @Tag("FR-029-09")
    void un_doublon_qui_franchit_le_controle_est_refuse_par_la_contrainte_unique() {
        assertThatThrownBy(() -> depot.ajouter(new NouvelleValeur("bnp.pa", "BNP", "Euronext Paris", "EUR")))
                .isInstanceOfSatisfying(AjoutRefuse.class, refus -> {
                    assertThat(refus.motif()).isEqualTo(AjoutRefuse.Motif.DOUBLON);
                    assertThat(refus.symbole()).isEqualTo("BNP.PA");
                });
        assertThat(jdbc.queryForObject("select count(*) from watchlist_items", Long.class)).isEqualTo(9L);
    }

    @Test
    @Tag("FR-029-13")
    void refuse_un_symbole_ou_un_nom_trop_long() throws Exception {
        refuse("{\"symbole\":\"" + "X".repeat(21) + "\",\"nom\":\"N\",\"marche\":\"NYSE\",\"devise\":\"USD\"}",
                REQUETE_INVALIDE);
        refuse("{\"symbole\":\"X\",\"nom\":\"" + "N".repeat(101) + "\",\"marche\":\"NYSE\",\"devise\":\"USD\"}",
                REQUETE_INVALIDE);
    }

    @Test
    @Tag("FR-029-14")
    void refuse_un_marche_ou_une_devise_hors_liste_ou_absents() throws Exception {
        refuse("{\"symbole\":\"7203.T\",\"nom\":\"Toyota\",\"marche\":\"Bourse de Tokyo\",\"devise\":\"EUR\"}",
                REQUETE_INVALIDE);
        refuse("{\"symbole\":\"7203.T\",\"nom\":\"Toyota\",\"marche\":\"Autre\",\"devise\":\"JPY\"}", REQUETE_INVALIDE);
        refuse("{\"symbole\":\"7203.T\",\"nom\":\"Toyota\",\"devise\":\"EUR\"}", REQUETE_INVALIDE);
        refuse("{\"symbole\":\"7203.T\",\"nom\":\"Toyota\",\"marche\":\"Autre\"}", REQUETE_INVALIDE);
    }

    @Test
    @Tag("FR-029-09")
    void refuse_un_corps_illisible() throws Exception {
        refuse("{\"symbole\":", REQUETE_INVALIDE);
        HttpResponse<String> sansCorps = Jetons.envoyer(port, "POST", CHEMIN, jeton, null);
        assertThat(sansCorps.statusCode()).isEqualTo(400);
        assertThat(sansCorps.body()).isEqualTo(REQUETE_INVALIDE);
    }

    @Test
    @Tag("FR-009-13")
    void retire_une_valeur_sans_confirmation() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "DELETE", CHEMIN + "/1", jeton, null);

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(reponse.body()).isEmpty();
        assertThat(Jetons.envoyer(port, "GET", CHEMIN, jeton, null).body())
                .isEqualTo(liste(REFERENCE.subList(1, REFERENCE.size())));
    }

    @Test
    @Tag("FR-009-13")
    void retirer_une_valeur_inconnue_rend_404_ou_400() throws Exception {
        HttpResponse<String> inconnue = Jetons.envoyer(port, "DELETE", CHEMIN + "/999", jeton, null);
        assertThat(inconnue.statusCode()).isEqualTo(404);
        assertThat(inconnue.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");

        HttpResponse<String> nonNumerique = Jetons.envoyer(port, "DELETE", CHEMIN + "/abc", jeton, null);
        assertThat(nonNumerique.statusCode()).isEqualTo(400);
        assertThat(nonNumerique.body()).isEqualTo(REQUETE_INVALIDE);
        assertThat(jdbc.queryForObject("select count(*) from watchlist_items", Long.class)).isEqualTo(9L);
    }

    @Test
    @Tag("FR-009-01")
    void une_route_ou_une_methode_inconnue_rend_le_format_message() throws Exception {
        HttpResponse<String> route = Jetons.envoyer(port, "GET", "/api/bourse/inconnu", jeton, null);
        assertThat(route.statusCode()).isEqualTo(404);
        assertThat(route.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");

        HttpResponse<String> methode = Jetons.envoyer(port, "PUT", CHEMIN, jeton, "{}");
        assertThat(methode.statusCode()).isEqualTo(405);
        assertThat(methode.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
    }

    private HttpResponse<String> ajouter(String corps) throws Exception {
        return Jetons.envoyer(port, "POST", CHEMIN, jeton, corps);
    }

    private void refuse(String corps, String message) throws Exception {
        HttpResponse<String> reponse = ajouter(corps);
        assertThat(reponse.statusCode()).as(corps).isEqualTo(400);
        assertThat(reponse.body()).as(corps).isEqualTo(message);
        assertThat(jdbc.queryForObject("select count(*) from watchlist_items", Long.class)).isEqualTo(9L);
    }

    private static String jeton(String role) {
        Instant maintenant = Instant.now();
        return Jetons.jeton(Conteneurs.CLES, role, maintenant, maintenant.plusSeconds(1800));
    }

    private static String valeur(long id, String symbole, String nom, String marche, String devise) {
        return "{\"id\":" + id + ",\"symbole\":\"" + symbole + "\",\"nom\":\"" + nom + "\",\"marche\":\"" + marche
                + "\",\"devise\":\"" + devise + "\"}";
    }

    private static String liste(List<String> valeurs) {
        return valeurs.stream().collect(Collectors.joining(",", "[", "]"));
    }
}
