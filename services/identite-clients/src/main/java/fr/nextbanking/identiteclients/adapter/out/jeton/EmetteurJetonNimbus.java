package fr.nextbanking.identiteclients.adapter.out.jeton;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.domain.Client;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : jeton JWT signé RS256 par {@link NimbusJwtEncoder}, avec la clé privée lue dans
 * {@code nb.jeton.cle-privee} (PKCS#8 DER en base64, PEM accepté). Revendications : {@code sub} (identifiant
 * numérique du client), {@code roles}, {@code jti} neuf, {@code iat} lu sur la {@link Clock} système et
 * {@code exp} = {@code iat} + {@code nb.jeton.duree-de-vie}. Sans clé privée lisible, le service ne démarre pas.
 */
@Component
public class EmetteurJetonNimbus implements EmetteurJeton {

    static final String PROPRIETE_CLE_PRIVEE = "nb.jeton.cle-privee";

    private final JwtEncoder encodeur;
    private final Duration dureeDeVie;
    private final Clock horloge;

    @Autowired
    public EmetteurJetonNimbus(@Value("${nb.jeton.cle-privee:}") String clePrivee,
            @Value("${nb.jeton.duree-de-vie}") Duration dureeDeVie) {
        this(clePrivee, dureeDeVie, Clock.systemUTC());
    }

    EmetteurJetonNimbus(String clePrivee, Duration dureeDeVie, Clock horloge) {
        this.encodeur = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(cleRsa(lireClePrivee(clePrivee)))));
        this.dureeDeVie = dureeDeVie;
        this.horloge = horloge;
    }

    @Override
    public String emettre(Client client) {
        Instant emission = horloge.instant().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet revendications = JwtClaimsSet.builder()
                .subject(Long.toString(client.id()))
                .claim("roles", List.of(client.role().name()))
                .id(UUID.randomUUID().toString())
                .issuedAt(emission)
                .expiresAt(emission.plus(dureeDeVie))
                .build();
        JwsHeader entete = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encodeur.encode(JwtEncoderParameters.from(entete, revendications)).getTokenValue();
    }

    static RSAPrivateCrtKey lireClePrivee(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("La propriété " + PROPRIETE_CLE_PRIVEE
                    + " est obligatoire (variable d'environnement NB_JETON_CLE_PRIVEE).");
        }
        try {
            byte[] der = Base64.getMimeDecoder().decode(valeur.replaceAll("-----[A-Z ]+-----", ""));
            return (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException e) {
            // Cause non chaînée : son message pourrait reprendre une partie de la clé.
            throw new IllegalStateException("La propriété " + PROPRIETE_CLE_PRIVEE
                    + " n'est pas une clé privée RSA PKCS#8 lisible.");
        }
    }

    private static RSAKey cleRsa(RSAPrivateCrtKey clePrivee) {
        try {
            RSAPublicKey clePublique = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(clePrivee.getModulus(), clePrivee.getPublicExponent()));
            return new RSAKey.Builder(clePublique).privateKey(clePrivee).build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Clé publique non dérivable de " + PROPRIETE_CLE_PRIVEE + ".");
        }
    }
}
