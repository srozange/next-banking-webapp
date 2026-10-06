package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.BouchonAzureOpenAi;
import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.ParametresDeploiementAzure;
import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur réel contre un bouchon local, de l'endpoint au fournisseur (mode réel posé en propriété de test
 * seulement, clé et adresse fournies par le test). Le coupe-circuit est éprouvé en dernier : ouvert, il le reste.
 */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"nb.fournisseurs.azure-openai.mode=reel", "nb.fournisseurs.azure-openai.delai-lecture=300ms",
                "nb.fournisseurs.azure-openai.delai-connexion=2s", "nb.fournisseurs.azure-openai.seuil-echecs=4",
                "nb.fournisseurs.azure-openai.duree-ouverture=10m"})
@Import(ChatReelIT.Parametres.class)
@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatReelIT {

    private static final String CHEMIN = "/api/open-banking-conseiller/chat/messages";
    private static final String CLE = "cle-de-test";
    private static final String GENERIQUE = "{\"message\":\"Je suis momentanement indisponible. "
            + "Veuillez reessayer ou contacter votre conseiller en agence.\"}";
    static final BouchonAzureOpenAi BOUCHON = new BouchonAzureOpenAi();
    static volatile Map<String, String> valeurs = Map.of("AZURE_OPENAI_API_KEY", CLE,
            "AZURE_OPENAI_ENDPOINT", BOUCHON.adresse());
    private static final JsonMapper JSON = JsonMapper.builder().build();

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
    @Tag("FR-010-05")
    @Tag("FR-010-06")
    void le_premier_puis_le_deuxieme_message_portent_consigne_historique_et_message() throws Exception {
        BOUCHON.repondre(200, BouchonAzureOpenAi.REPONSE);
        int avant = BOUCHON.requetes().size();

        HttpResponse<String> premier = envoyer("{\"message\":\"Bonjour\"}");
        HttpResponse<String> second = envoyer("{\"message\":\"Et l'épargne ?\",\"historique\":["
                + "{\"role\":\"utilisateur\",\"contenu\":\"Bonjour\"},{\"role\":\"assistant\",\"contenu\":\"Bonjour !\"}]}");

        assertThat(premier.statusCode()).isEqualTo(200);
        assertThat(premier.body()).isEqualTo("{\"reponse\":\"Bonjour !\"}");
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(BOUCHON.requetes()).hasSize(avant + 2);
        BouchonAzureOpenAi.Requete requete = BOUCHON.requetes().get(avant);
        assertThat(requete.cle()).isEqualTo(CLE);
        JsonNode un = JSON.readTree(requete.corps());
        assertThat(un.get("max_tokens").asInt()).isEqualTo(500);
        assertThat(un.get("temperature").asDouble()).isEqualTo(0.7);
        assertThat(roles(un)).containsExactly("system", "user");
        JsonNode deux = JSON.readTree(BOUCHON.requetes().get(avant + 1).corps());
        assertThat(roles(deux)).containsExactly("system", "user", "assistant", "user");
        assertThat(deux.get("messages").get(3).get("content").asString()).isEqualTo("Et l'épargne ?");
    }

    @Test
    @Order(2)
    @Tag("FR-010-09")
    void un_statut_500_rend_502_avec_le_statut_sans_le_corps_du_fournisseur() throws Exception {
        BOUCHON.repondre(500, "{\"error\":\"detail interne\"}");

        HttpResponse<String> reponse = envoyer("{\"message\":\"Bonjour\"}");
        retablir();

        assertThat(reponse.statusCode()).isEqualTo(502);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Je suis momentanement indisponible (HTTP 500). "
                + "Veuillez reessayer.\"}");
    }

    @Test
    @Order(3)
    @Tag("FR-010-09")
    @Tag("BUG-038")
    void un_refus_401_ou_403_rend_503_sans_code() throws Exception {
        for (int statut : new int[] {401, 403}) {
            BOUCHON.repondre(statut, "{\"error\":\"refus\"}");
            HttpResponse<String> reponse = envoyer("{\"message\":\"Bonjour\"}");
            retablir();

            assertThat(reponse.statusCode()).isEqualTo(503);
            assertThat(reponse.body()).isEqualTo(GENERIQUE).doesNotContain("401", "403");
        }
    }

    @Test
    @Order(4)
    @Tag("FR-010-09")
    void un_corps_illisible_ou_un_delai_depasse_rend_503_generique() throws Exception {
        BOUCHON.repondre(200, "<html>");
        HttpResponse<String> illisible = envoyer("{\"message\":\"Bonjour\"}");
        retablir();
        BOUCHON.repondreApres(Duration.ofSeconds(1), BouchonAzureOpenAi.REPONSE);
        HttpResponse<String> lent = envoyer("{\"message\":\"Bonjour\"}");
        retablir();

        assertThat(illisible.statusCode()).isEqualTo(503);
        assertThat(illisible.body()).isEqualTo(GENERIQUE);
        assertThat(lent.statusCode()).isEqualTo(503);
        assertThat(lent.body()).isEqualTo(GENERIQUE);
    }

    @Test
    @Order(5)
    @Tag("FR-010-09")
    void sans_cle_rend_503_et_le_bouchon_ne_recoit_aucune_requete() throws Exception {
        valeurs = Map.of("AZURE_OPENAI_ENDPOINT", BOUCHON.adresse());
        int avant = BOUCHON.requetes().size();
        try {
            HttpResponse<String> reponse = envoyer("{\"message\":\"Bonjour\"}");

            assertThat(reponse.statusCode()).isEqualTo(503);
            assertThat(reponse.body()).isEqualTo(GENERIQUE);
            assertThat(BOUCHON.requetes()).hasSize(avant);
        } finally {
            valeurs = Map.of("AZURE_OPENAI_API_KEY", CLE, "AZURE_OPENAI_ENDPOINT", BOUCHON.adresse());
        }
    }

    @Test
    @Order(6)
    @Tag("FR-010-09")
    void un_coupe_circuit_ouvert_rend_503_sans_appeler_le_bouchon() throws Exception {
        BOUCHON.repondre(500, "{}");
        for (int i = 0; i < 4; i++) {
            assertThat(envoyer("{\"message\":\"Bonjour\"}").statusCode()).isEqualTo(502);
        }
        BOUCHON.repondre(200, BouchonAzureOpenAi.REPONSE);
        int avant = BOUCHON.requetes().size();

        HttpResponse<String> reponse = envoyer("{\"message\":\"Bonjour\"}");

        assertThat(reponse.statusCode()).isEqualTo(503);
        assertThat(reponse.body()).isEqualTo(GENERIQUE);
        assertThat(BOUCHON.requetes()).hasSize(avant);
    }

    /** Un succès après un échec : le coupe-circuit (4 échecs consécutifs) reste fermé. */
    private void retablir() throws Exception {
        BOUCHON.repondre(200, BouchonAzureOpenAi.REPONSE);
        assertThat(envoyer("{\"message\":\"Bonjour\"}").statusCode()).isEqualTo(200);
    }

    private static java.util.List<String> roles(JsonNode corps) {
        return corps.get("messages").valueStream().map(m -> m.get("role").asString()).toList();
    }

    private HttpResponse<String> envoyer(String corps) throws Exception {
        Instant maintenant = Instant.now();
        String jeton = Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant, maintenant.plusSeconds(1800));
        return Jetons.envoyer(port, "POST", CHEMIN, jeton, corps);
    }

    /** Clé et adresse du bouchon, à la place de l'environnement du processus. */
    @TestConfiguration
    static class Parametres {

        @Bean
        @Primary
        ParametresDeploiementAzure parametresDuBouchon() {
            return new ParametresDeploiementAzure(nom -> valeurs.get(nom), nom -> null);
        }
    }
}
