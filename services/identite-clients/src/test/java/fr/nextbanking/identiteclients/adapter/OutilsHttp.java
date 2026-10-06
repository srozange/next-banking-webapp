package fr.nextbanking.identiteclients.adapter;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import fr.nextbanking.identiteclients.referencedata.GenerateurReferenceData;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Outils des tests d'intégration : requêtes HTTP, jetons forgés, données de référence aux mots de passe de test.
 */
public final class OutilsHttp {

    public static final String MDP_ADMIN = "mdp-admin-de-test";
    public static final String MDP_CONSEILLER = "mdp-conseiller-de-test";
    public static final String MDP_CLIENT = "mdp-client-de-test";

    /** Données de référence générées une fois avec les mots de passe de test. */
    public static final String DONNEES = GenerateurReferenceData.generer(Map.of(
            "RETRO_ADMIN_PASSWORD", MDP_ADMIN,
            "RETRO_CONSEILLER_PASSWORD", MDP_CONSEILLER,
            "RETRO_CLIENT_PASSWORD", MDP_CLIENT)::get);

    private OutilsHttp() {
    }

    /** Remet la table {@code customers} aux données de référence de test. */
    public static void chargerDonnees(DataSource base) {
        executer(base, "truncate table customers restart identity;\n" + DONNEES);
    }

    /** Exécute un script SQL de plusieurs instructions. */
    public static void executer(DataSource base, String script) {
        try (Connection connexion = base.getConnection(); Statement instruction = connexion.createStatement()) {
            instruction.execute(script);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static HttpResponse<String> envoyer(int port, String methode, String chemin, String corps,
            Map<String, String> entetes) throws IOException, InterruptedException {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .method(methode, corps == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corps));
        entetes.forEach(requete::header);
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(requete.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    public static HttpResponse<String> connexion(int port, String corps) throws IOException, InterruptedException {
        return envoyer(port, "POST", "/api/identite-clients/connexion", corps,
                Map.of("Content-Type", "application/json"));
    }

    public static HttpResponse<String> avecJeton(int port, String methode, String chemin, String jeton)
            throws IOException, InterruptedException {
        return envoyer(port, methode, chemin, null, Map.of("Authorization", "Bearer " + jeton));
    }

    /** Jeton forgé avec la paire donnée : sujet, émission et expiration au choix. */
    public static String jeton(KeyPair cles, String sujet, Instant emission, Instant expiration) {
        RSAKey cle = new RSAKey.Builder((RSAPublicKey) cles.getPublic()).privateKey((RSAPrivateKey) cles.getPrivate())
                .build();
        JwtClaimsSet revendications = JwtClaimsSet.builder().subject(sujet).claim("roles", List.of("CLIENT"))
                .id(UUID.randomUUID().toString()).issuedAt(emission).expiresAt(expiration).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(cle)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), revendications))
                .getTokenValue();
    }

    /** Jeton valide, signé par la clé du service de test. */
    public static String jetonValide(String sujet) {
        Instant maintenant = Instant.now();
        return jeton(Conteneurs.CLES, sujet, maintenant, maintenant.plusSeconds(1800));
    }
}
