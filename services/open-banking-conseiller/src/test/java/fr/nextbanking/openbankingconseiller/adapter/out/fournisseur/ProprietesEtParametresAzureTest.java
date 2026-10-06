package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProprietesEtParametresAzureTest {

    @Test
    @Tag("FR-010-08")
    void les_defauts_sont_ceux_du_legacy_et_du_mode_simule() {
        ProprietesAzureOpenAi proprietes = new ProprietesAzureOpenAi(null, null, null, null, null, null);

        assertThat(proprietes.reel()).isFalse();
        assertThat(proprietes.delaiConnexion()).isEqualTo(Duration.ofSeconds(15));
        assertThat(proprietes.delaiLecture()).isEqualTo(Duration.ofSeconds(30));
        assertThat(proprietes.refusSimule()).isNull();
    }

    @Test
    void une_valeur_invalide_est_refusee() {
        assertThatThrownBy(() -> new ProprietesAzureOpenAi("autre", null, null, null, null, null))
                .hasMessageContaining("mode");
        assertThatThrownBy(() -> new ProprietesAzureOpenAi(null, Duration.ZERO, null, null, null, null))
                .hasMessageContaining("delai-connexion");
        assertThatThrownBy(() -> new ProprietesAzureOpenAi(null, null, Duration.ofSeconds(-1), null, null, null))
                .hasMessageContaining("delai-lecture");
        assertThatThrownBy(() -> new ProprietesAzureOpenAi(null, null, null, 0, null, null))
                .hasMessageContaining("seuil-echecs");
        assertThatThrownBy(() -> new ProprietesAzureOpenAi(null, null, null, null, Duration.ZERO, null))
                .hasMessageContaining("duree-ouverture");
        assertThatThrownBy(() -> new ProprietesAzureOpenAi(null, null, null, null, null, 500))
                .hasMessageContaining("refus-simule");
        assertThat(new ProprietesAzureOpenAi("reel", null, null, null, null, 403).reel()).isTrue();
    }

    @Test
    @Tag("FR-010-08")
    void l_environnement_prime_sur_la_propriete_systeme_qui_prime_sur_le_defaut() {
        ParametresDeploiementAzure defauts = new ParametresDeploiementAzure(nom -> null, nom -> " ");
        assertThat(defauts.cle()).isEmpty();
        assertThat(defauts.endpoint()).isEqualTo(ParametresDeploiementAzure.ENDPOINT_PAR_DEFAUT);
        assertThat(defauts.deploiement()).isEqualTo("gpt-5.4-nano");
        assertThat(defauts.version()).isEqualTo("2024-12-01-preview");

        Map<String, String> environnement = Map.of("AZURE_OPENAI_DEPLOYMENT", "env", "AZURE_OPENAI_API_KEY", " ");
        Map<String, String> systeme = Map.of("AZURE_OPENAI_DEPLOYMENT", "systeme", "AZURE_OPENAI_API_VERSION", "v1",
                "AZURE_OPENAI_ENDPOINT", "http://x", "AZURE_OPENAI_API_KEY", "cle");
        ParametresDeploiementAzure parametres = new ParametresDeploiementAzure(environnement::get, systeme::get);

        assertThat(parametres.deploiement()).isEqualTo("env");
        assertThat(parametres.version()).isEqualTo("v1");
        assertThat(parametres.endpoint()).isEqualTo("http://x");
        assertThat(parametres.cle()).contains("cle");
        assertThat(new ParametresDeploiementAzure().endpoint()).isNotBlank();
    }
}
