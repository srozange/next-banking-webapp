package fr.nextbanking.openbankingconseiller.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ConfigurationSecuriteTest {

    private static final PublicKey CLE = cle();

    @Test
    void lit_une_cle_publique_der_en_base64_ou_pem() {
        String base64 = Base64.getEncoder().encodeToString(CLE.getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(CLE.getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        assertThat(ConfigurationSecurite.lireClePublique(base64)).isEqualTo(CLE);
        assertThat(ConfigurationSecurite.lireClePublique(pem)).isEqualTo(CLE);
    }

    @Test
    void refuse_une_cle_publique_absente_vide_ou_illisible() {
        assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique(null))
                .hasMessageContaining("nb.jeton.cle-publique est obligatoire");
        assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique(" "))
                .hasMessageContaining("nb.jeton.cle-publique est obligatoire");
        assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique("illisible"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nb.jeton.cle-publique")
                .hasNoCause();
    }

    @Test
    void ramene_chaque_statut_a_un_statut_publie_et_a_son_texte() {
        assertThat(Textes.statutPublie(404)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(Textes.statutPublie(415)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Textes.statutPublie(399)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Textes.statutPublie(503)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Textes.texte(HttpStatus.BAD_REQUEST)).isEqualTo("Requête invalide");
        assertThat(Textes.texte(HttpStatus.UNAUTHORIZED)).isEqualTo("Authentification requise");
        assertThat(Textes.texte(HttpStatus.FORBIDDEN)).isEqualTo("Accès refusé");
        assertThat(Textes.texte(HttpStatus.NOT_FOUND)).isEqualTo("Ressource introuvable");
        assertThat(Textes.texte(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo("Ressource introuvable");
        assertThat(Textes.texte(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("Une erreur technique est survenue");
    }

    @Test
    void le_dispatch_d_erreur_ne_rend_que_le_message() {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/error");
        requete.setAttribute("jakarta.servlet.error.status_code", 404);

        assertThat(new AttributsErreur().getErrorAttributes(new ServletWebRequest(requete),
                ErrorAttributeOptions.defaults())).containsExactly(Map.entry("message", "Ressource introuvable"));
        assertThat(new AttributsErreur().getErrorAttributes(
                new ServletWebRequest(new MockHttpServletRequest("GET", "/error")), ErrorAttributeOptions.defaults()))
                .containsExactly(Map.entry("message", "Une erreur technique est survenue"));
    }

    private static PublicKey cle() {
        try {
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
            generateur.initialize(2048);
            return generateur.generateKeyPair().getPublic();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
