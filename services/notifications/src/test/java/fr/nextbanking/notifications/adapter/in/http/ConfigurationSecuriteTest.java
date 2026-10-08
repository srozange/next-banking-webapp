package fr.nextbanking.notifications.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-002-07")
class ConfigurationSecuriteTest {

    @Test
    void lit_une_cle_publique_rsa_en_der_base64() throws Exception {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        byte[] der = generateur.generateKeyPair().getPublic().getEncoded();

        assertThat(ConfigurationSecurite.lireClePublique(Base64.getEncoder().encodeToString(der)).getEncoded())
                .isEqualTo(der);
    }

    @Test
    void refuse_une_cle_absente_ou_vide() {
        for (String valeur : new String[] {null, "", "  "}) {
            assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique(valeur))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nb.jeton.cle-publique est obligatoire");
        }
    }

    @Test
    void refuse_une_cle_illisible() {
        assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique("AAAA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("n'est pas une clé publique RSA X.509 lisible");
        assertThatThrownBy(() -> ConfigurationSecurite.lireClePublique("%%%"))
                .isInstanceOf(IllegalStateException.class);
    }
}
