package fr.nextbanking.bourse.adapter;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Jetons RS256 fabriqués comme ceux d'identite-clients, et requêtes HTTP de test. */
final class Jetons {

    private Jetons() {
    }

    static String jeton(KeyPair cles, String role, Instant emission, Instant expiration) {
        RSAKey cle = new RSAKey.Builder((RSAPublicKey) cles.getPublic()).privateKey((RSAPrivateKey) cles.getPrivate())
                .build();
        JwtClaimsSet revendications = JwtClaimsSet.builder().subject("10").claim("roles", List.of(role))
                .id(UUID.randomUUID().toString()).issuedAt(emission).expiresAt(expiration).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(cle)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), revendications))
                .getTokenValue();
    }

    static HttpResponse<String> envoyer(int port, String methode, String chemin, String jeton, String corps)
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
}
