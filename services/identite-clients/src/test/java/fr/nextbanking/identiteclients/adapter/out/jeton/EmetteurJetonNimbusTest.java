package fr.nextbanking.identiteclients.adapter.out.jeton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.Role;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class EmetteurJetonNimbusTest {

    private static final KeyPair CLES = paire();
    private static final String CLE_PRIVEE = Base64.getEncoder().encodeToString(CLES.getPrivate().getEncoded());
    private static final Client USER1 = new Client(1L, "user1", "Jean Dupont", Role.CLIENT, true, "h");

    // Décodeur sans contrôle de date : l'horloge du test est figée dans le passé.
    private final JwtDecoder decodeur = decodeur((RSAPublicKey) CLES.getPublic());

    @Test
    @Tag("FR-001-04")
    void signe_en_rs256_un_jeton_aux_revendications_attendues() {
        Instant maintenant = Instant.parse("2026-10-06T08:30:00.789Z");
        EmetteurJetonNimbus emetteur = new EmetteurJetonNimbus(CLE_PRIVEE, Duration.ofMinutes(30),
                Clock.fixed(maintenant, ZoneOffset.UTC));

        Jwt jeton = decodeur.decode(emetteur.emettre(USER1));

        assertThat(jeton.getHeaders()).containsEntry("alg", "RS256");
        assertThat(jeton.getSubject()).isEqualTo("1");
        assertThat(jeton.getClaimAsStringList("roles")).containsExactly("CLIENT");
        assertThat(UUID.fromString(jeton.getId())).isNotNull();
        assertThat(jeton.getIssuedAt()).isEqualTo(Instant.parse("2026-10-06T08:30:00Z"));
        assertThat(Duration.between(jeton.getIssuedAt(), jeton.getExpiresAt())).isEqualTo(Duration.ofSeconds(1800));
    }

    @Test
    @Tag("FR-001-09")
    @Tag("FR-030-08")
    @Tag("BUG-003")
    @Tag("BUG-092")
    void deux_jetons_successifs_ont_des_jti_differents() {
        EmetteurJetonNimbus emetteur = new EmetteurJetonNimbus(CLE_PRIVEE, Duration.ofMinutes(30));

        Jwt premier = decodeur.decode(emetteur.emettre(USER1));
        Jwt second = decodeur.decode(emetteur.emettre(USER1));

        assertThat(premier.getId()).isNotEqualTo(second.getId());
        assertThat(premier.getIssuedAt()).isNotNull();
    }

    @Test
    void porte_le_role_du_client() {
        EmetteurJetonNimbus emetteur = new EmetteurJetonNimbus(CLE_PRIVEE, Duration.ofMinutes(30));
        Client admin = new Client(10L, "admin", "Alice Martin", Role.ADMIN, true, "h");

        Jwt jeton = decodeur.decode(emetteur.emettre(admin));

        assertThat(jeton.getSubject()).isEqualTo("10");
        assertThat(jeton.getClaimAsStringList("roles")).containsExactly("ADMIN");
    }

    @Test
    void accepte_une_cle_privee_collee_en_pem() {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(CLES.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        String jeton = new EmetteurJetonNimbus(pem, Duration.ofMinutes(30)).emettre(USER1);

        assertThat(decodeur.decode(jeton).getSubject()).isEqualTo("1");
    }

    @Test
    void un_jeton_signe_par_une_autre_cle_est_refuse() {
        String autreCle = Base64.getEncoder().encodeToString(paire().getPrivate().getEncoded());

        String jeton = new EmetteurJetonNimbus(autreCle, Duration.ofMinutes(30)).emettre(USER1);

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(JwtException.class);
    }

    @Test
    void refuse_une_cle_privee_absente_ou_vide() {
        assertThatThrownBy(() -> new EmetteurJetonNimbus(null, Duration.ofMinutes(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nb.jeton.cle-privee est obligatoire");
        assertThatThrownBy(() -> new EmetteurJetonNimbus(" ", Duration.ofMinutes(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nb.jeton.cle-privee est obligatoire");
    }

    @Test
    void refuse_une_cle_privee_illisible_sans_la_reprendre_dans_le_message() {
        assertThatThrownBy(() -> new EmetteurJetonNimbus("pas-une-cle", Duration.ofMinutes(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nb.jeton.cle-privee")
                .hasNoCause()
                .message().doesNotContain("pas-une-cle");
        String clePublique = Base64.getEncoder().encodeToString(CLES.getPublic().getEncoded());
        assertThatThrownBy(() -> new EmetteurJetonNimbus(clePublique, Duration.ofMinutes(30)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JwtDecoder decodeur(RSAPublicKey cle) {
        NimbusJwtDecoder decodeur = NimbusJwtDecoder.withPublicKey(cle).build();
        decodeur.setJwtValidator(jeton -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());
        return decodeur;
    }

    private static KeyPair paire() {
        try {
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
            generateur.initialize(2048);
            return generateur.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
