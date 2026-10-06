package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
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
 * Contrôle d'accès de coeur-bancaire : tout est authentifié sauf {@code POST /api/coeur-bancaire/temoin} et
 * {@code GET /actuator/health} ; sans jeton valide, 401 « Authentification requise » en JSON, sans redirection ni
 * cookie ; avec un jeton valide, une route inconnue rend 404 « Ressource introuvable ».
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-030-01")
class CoeurBancaireSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";
    private static final String RESSOURCE_INTROUVABLE = "{\"message\":\"Ressource introuvable\"}";
    private static final List<String> CHEMINS = List.of("/api/coeur-bancaire/tableau-de-bord",
            "/api/coeur-bancaire/temoin", "/api/coeur-bancaire/temoin/", "/api/coeur-bancaire/inconnu",
            "/actuator/info");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Test
    void sans_jeton_ou_avec_un_jeton_invalide_les_points_proteges_rendent_401() throws Exception {
        Instant maintenant = Instant.now();
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = jeton(generateur.generateKeyPair(), maintenant, maintenant.plusSeconds(1800));
        String expire = jeton(Conteneurs.CLES, maintenant.minusSeconds(7200), maintenant.minusSeconds(3600));

        for (String chemin : CHEMINS) {
            for (String jeton : new String[] {null, malSigne, expire, "illisible"}) {
                HttpResponse<String> reponse = envoyer("GET", chemin, jeton, null);
                assertThat(reponse.statusCode()).as(chemin).isEqualTo(401);
                assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
                assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                        assertThat(type).startsWith("application/json"));
                assertThat(reponse.headers().firstValue("Location")).isEmpty();
                assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
            }
        }
    }

    @Test
    void avec_un_jeton_valide_une_route_inconnue_rend_404() throws Exception {
        Instant maintenant = Instant.now();
        String valide = jeton(Conteneurs.CLES, maintenant, maintenant.plusSeconds(1800));

        HttpResponse<String> reponse = envoyer("GET", "/api/coeur-bancaire/tableau-de-bord", valide, null);

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(reponse.body()).isEqualTo(RESSOURCE_INTROUVABLE);
    }

    @Test
    void le_temoin_et_la_sante_restent_publics() throws Exception {
        assertThat(envoyer("POST", "/api/coeur-bancaire/temoin", null, "{\"libelle\":\"x\"}").statusCode())
                .isEqualTo(201);
        assertThat(envoyer("GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> envoyer(String methode, String chemin, String jeton, String corps)
            throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .method(methode, corps == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corps));
        if (corps != null) {
            requete.header("Content-Type", "application/json");
        }
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
