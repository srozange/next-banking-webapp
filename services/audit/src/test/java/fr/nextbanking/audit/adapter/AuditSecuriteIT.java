package fr.nextbanking.audit.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import fr.nextbanking.audit.infrastructure.ApplicationAudit;
import fr.nextbanking.audit.infrastructure.Conteneurs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Contrôle d'accès du service audit, qui n'expose aucun point d'API HTTP métier : sans jeton valide, 401
 * « Authentification requise » ; avec un jeton valide, 404 « Ressource introuvable » ; santé publique.
 */
@SpringBootTest(classes = ApplicationAudit.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-026-06")
class AuditSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";
    private static final String RESSOURCE_INTROUVABLE = "{\"message\":\"Ressource introuvable\"}";
    private static final List<String> CHEMINS = List.of("/api/audit/", "/api/audit", "/api/audit/journal",
            "/api/audit/logs?username=user1", "/actuator/info");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Test
    void sans_jeton_ou_avec_un_jeton_invalide_tout_rend_401() throws Exception {
        Instant maintenant = Instant.now();
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = jeton(generateur.generateKeyPair(), maintenant, maintenant.plusSeconds(1800));
        String expire = jeton(Conteneurs.CLES, maintenant.minusSeconds(7200), maintenant.minusSeconds(3600));

        for (String chemin : CHEMINS) {
            for (String jeton : new String[] {null, malSigne, expire, "illisible"}) {
                HttpResponse<String> reponse = envoyer("GET", chemin, jeton);
                assertThat(reponse.statusCode()).as(chemin).isEqualTo(401);
                assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
                assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
            }
        }
        assertThat(envoyer("POST", "/api/audit/journal", null).statusCode()).isEqualTo(401);
    }

    @Test
    void avec_un_jeton_valide_aucune_route_n_existe() throws Exception {
        Instant maintenant = Instant.now();
        String valide = jeton(Conteneurs.CLES, maintenant, maintenant.plusSeconds(1800));

        for (String chemin : CHEMINS) {
            HttpResponse<String> reponse = envoyer("GET", chemin, valide);
            assertThat(reponse.statusCode()).as(chemin).isEqualTo(404);
            assertThat(reponse.body()).as(chemin).isEqualTo(RESSOURCE_INTROUVABLE);
        }
        assertThat(envoyer("POST", "/api/audit/journal", valide).statusCode()).isEqualTo(404);
    }

    @Test
    void la_sante_est_publique() throws Exception {
        assertThat(envoyer("GET", "/actuator/health", null).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> envoyer(String methode, String chemin, String jeton) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .method(methode, HttpRequest.BodyPublishers.noBody());
        if (jeton != null) {
            requete.header("Authorization", "Bearer " + jeton);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(requete.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private static String jeton(KeyPair cles, Instant emission, Instant expiration) {
        RSAKey cle = new RSAKey.Builder((RSAPublicKey) cles.getPublic()).privateKey((RSAPrivateKey) cles.getPrivate())
                .build();
        JwtClaimsSet revendications = JwtClaimsSet.builder().subject("10").claim("roles", List.of("ADMIN"))
                .id(UUID.randomUUID().toString()).issuedAt(emission).expiresAt(expiration).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(cle)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), revendications))
                .getTokenValue();
    }
}
