package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible.Nature;
import fr.nextbanking.openbankingconseiller.domain.ConsigneSophie;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import fr.nextbanking.openbankingconseiller.domain.RoleEchange;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Adaptateur réel contre un bouchon local : requête émise, échecs rendus en erreur typée, coupe-circuit. */
@ExtendWith(OutputCaptureExtension.class)
class AzureOpenAiReelTest {

    private static final String CLE = "cle-de-test-secrete";
    private final BouchonAzureOpenAi bouchon = new BouchonAzureOpenAi();

    @AfterEach
    void arreter() {
        bouchon.close();
    }

    private AzureOpenAiReel adaptateur(Map<String, String> valeurs, int seuil) {
        ProprietesAzureOpenAi proprietes = new ProprietesAzureOpenAi("reel", Duration.ofSeconds(2),
                Duration.ofMillis(500), seuil, Duration.ofMinutes(10), null);
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(proprietes.delaiConnexion()).build());
        fabrique.setReadTimeout(proprietes.delaiLecture());
        return new AzureOpenAiReel(RestClient.builder().requestFactory(fabrique).build(),
                new ParametresDeploiementAzure(valeurs::get, nom -> null), proprietes);
    }

    private AzureOpenAiReel adaptateur() {
        return adaptateur(Map.of("AZURE_OPENAI_API_KEY", CLE, "AZURE_OPENAI_ENDPOINT", bouchon.adresse()), 3);
    }

    @Test
    @Tag("FR-010-06")
    @Tag("FR-010-07")
    @Tag("FR-010-08")
    void le_premier_message_part_avec_la_consigne_et_le_message_seulement() throws Exception {
        String reponse = adaptateur().repondre(ConsigneSophie.TEXTE, List.of(), "Bonjour");

        assertThat(reponse).isEqualTo("Bonjour !");
        BouchonAzureOpenAi.Requete requete = bouchon.requetes().getFirst();
        assertThat(requete.chemin()).isEqualTo("/openai/deployments/gpt-5.4-nano/chat/completions"
                + "?api-version=2024-12-01-preview");
        assertThat(requete.cle()).isEqualTo(CLE);
        JsonNode corps = JsonMapper.builder().build().readTree(requete.corps());
        assertThat(corps.get("max_tokens").asInt()).isEqualTo(500);
        assertThat(corps.get("temperature").asDouble()).isEqualTo(0.7);
        assertThat(corps.get("messages")).hasSize(2);
        assertThat(corps.get("messages").get(0).get("role").asString()).isEqualTo("system");
        assertThat(corps.get("messages").get(0).get("content").asString()).isEqualTo(ConsigneSophie.TEXTE);
        assertThat(corps.get("messages").get(1).get("role").asString()).isEqualTo("user");
        assertThat(corps.get("messages").get(1).get("content").asString()).isEqualTo("Bonjour");
    }

    @Test
    @Tag("FR-010-06")
    void le_deuxieme_message_porte_la_consigne_les_echanges_et_le_message() throws Exception {
        adaptateur().repondre(ConsigneSophie.TEXTE, List.of(new Echange(RoleEchange.UTILISATEUR, "Bonjour"),
                new Echange(RoleEchange.ASSISTANT, "Bonjour !")), "Et l'épargne ?");

        JsonNode messages = JsonMapper.builder().build().readTree(bouchon.requetes().getFirst().corps())
                .get("messages");
        assertThat(messages).hasSize(4);
        assertThat(messages.valueStream().map(m -> m.get("role").asString()).toList())
                .containsExactly("system", "user", "assistant", "user");
        assertThat(messages.get(3).get("content").asString()).isEqualTo("Et l'épargne ?");
    }

    @Test
    @Tag("FR-010-08")
    void endpoint_deploiement_et_version_viennent_des_parametres() {
        adaptateur(Map.of("AZURE_OPENAI_API_KEY", CLE, "AZURE_OPENAI_ENDPOINT", bouchon.adresse(),
                "AZURE_OPENAI_DEPLOYMENT", "mon-modele", "AZURE_OPENAI_API_VERSION", "2025-01-01"), 3)
                .repondre(ConsigneSophie.TEXTE, List.of(), "x");

        assertThat(bouchon.requetes().getFirst().chemin())
                .isEqualTo("/openai/deployments/mon-modele/chat/completions?api-version=2025-01-01");
    }

    @Test
    @Tag("FR-010-09")
    @Tag("BUG-038")
    void un_refus_401_ou_403_est_une_erreur_d_authentification() {
        for (int statut : new int[] {401, 403}) {
            bouchon.repondre(statut, "{\"error\":\"corps du fournisseur\"}");
            assertThatThrownBy(() -> adaptateur().repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                    .isInstanceOfSatisfying(ServiceIaIndisponible.class,
                            e -> assertThat(e.nature()).isEqualTo(Nature.AUTHENTIFICATION));
        }
    }

    @Test
    @Tag("FR-010-09")
    void un_autre_statut_est_rendu_sans_le_corps_du_fournisseur() {
        bouchon.repondre(500, "{\"error\":\"corps du fournisseur\"}");

        assertThatThrownBy(() -> adaptateur().repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                .isInstanceOfSatisfying(ServiceIaIndisponible.class, e -> {
                    assertThat(e.nature()).isEqualTo(Nature.STATUT);
                    assertThat(e.statutRecu()).isEqualTo(500);
                    assertThat(e.getMessage()).doesNotContain("corps du fournisseur");
                });
    }

    @Test
    @Tag("FR-010-09")
    void un_corps_illisible_ou_un_delai_depasse_est_une_indisponibilite(CapturedOutput sortie) {
        AzureOpenAiReel adaptateur = adaptateur(Map.of("AZURE_OPENAI_API_KEY", CLE,
                "AZURE_OPENAI_ENDPOINT", bouchon.adresse()), 10);
        for (String corps : new String[] {"<html>", "{}", "{\"choices\":[]}"}) {
            bouchon.repondre(200, corps);
            assertThatThrownBy(() -> adaptateur.repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                    .isInstanceOfSatisfying(ServiceIaIndisponible.class,
                            e -> assertThat(e.nature()).isEqualTo(Nature.INDISPONIBLE));
        }
        bouchon.repondreApres(Duration.ofSeconds(2), BouchonAzureOpenAi.REPONSE);
        assertThatThrownBy(() -> adaptateur.repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                .isInstanceOfSatisfying(ServiceIaIndisponible.class, e -> {
                    assertThat(e.nature()).isEqualTo(Nature.INDISPONIBLE);
                    assertThat(e.getMessage()).isEqualTo("délai dépassé");
                });
        assertThat(sortie.getAll()).doesNotContain(CLE);
    }

    @Test
    @Tag("FR-010-08")
    void une_adresse_injoignable_est_une_indisponibilite_sans_l_adresse_dans_le_motif() {
        String adresse = bouchon.adresse();
        bouchon.close();

        assertThatThrownBy(() -> adaptateur(Map.of("AZURE_OPENAI_API_KEY", CLE, "AZURE_OPENAI_ENDPOINT", adresse), 3)
                .repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                .isInstanceOfSatisfying(ServiceIaIndisponible.class, e -> {
                    assertThat(e.nature()).isEqualTo(Nature.INDISPONIBLE);
                    assertThat(e.getMessage()).startsWith("erreur d'appel").doesNotContain("localhost", CLE);
                });
    }

    @Test
    @Tag("FR-010-08")
    void sans_cle_aucune_requete_n_est_emise(CapturedOutput sortie) {
        AzureOpenAiReel adaptateur = adaptateur(Map.of("AZURE_OPENAI_ENDPOINT", bouchon.adresse()), 3);

        assertThatThrownBy(() -> adaptateur.repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                .isInstanceOfSatisfying(ServiceIaIndisponible.class,
                        e -> assertThat(e.nature()).isEqualTo(Nature.INDISPONIBLE));
        assertThat(bouchon.requetes()).isEmpty();
        assertThat(sortie.getAll()).contains("clé absente");
    }

    @Test
    @Tag("FR-010-09")
    void le_coupe_circuit_ouvert_n_appelle_plus_le_fournisseur() {
        bouchon.repondre(500, "{}");
        AzureOpenAiReel adaptateur = adaptateur();
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> adaptateur.repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                    .isInstanceOfSatisfying(ServiceIaIndisponible.class,
                            e -> assertThat(e.nature()).isEqualTo(Nature.STATUT));
        }
        bouchon.repondre(200, BouchonAzureOpenAi.REPONSE);

        assertThatThrownBy(() -> adaptateur.repondre(ConsigneSophie.TEXTE, List.of(), "x"))
                .isInstanceOfSatisfying(ServiceIaIndisponible.class, e -> {
                    assertThat(e.nature()).isEqualTo(Nature.INDISPONIBLE);
                    assertThat(e.getMessage()).isEqualTo("coupe-circuit ouvert");
                });
        assertThat(bouchon.requetes()).hasSize(3);
    }
}
