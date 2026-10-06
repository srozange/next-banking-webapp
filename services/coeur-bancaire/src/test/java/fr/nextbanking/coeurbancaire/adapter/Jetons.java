package fr.nextbanking.coeurbancaire.adapter;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
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

/**
 * Jetons de test signés par la paire RSA des conteneurs de test, au format de l'identité (sub = clientId, roles).
 */
public final class Jetons {

    private Jetons() {
    }

    /** Jeton valide 30 minutes pour le client et le rôle donnés. */
    public static String pour(long clientId, String role) {
        RSAKey cle = new RSAKey.Builder((RSAPublicKey) Conteneurs.CLES.getPublic())
                .privateKey((RSAPrivateKey) Conteneurs.CLES.getPrivate()).build();
        Instant emission = Instant.now();
        JwtClaimsSet revendications = JwtClaimsSet.builder().subject(Long.toString(clientId))
                .claim("roles", List.of(role)).id(UUID.randomUUID().toString()).issuedAt(emission)
                .expiresAt(emission.plusSeconds(1800)).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(cle)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), revendications))
                .getTokenValue();
    }
}
