package fr.nextbanking.credits.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;

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
        assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique(""))
                .hasMessageContaining("nb.jeton.cle-publique est obligatoire");
        assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique("illisible"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nb.jeton.cle-publique")
                .hasNoCause();
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
