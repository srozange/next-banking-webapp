package fr.nextbanking.documents.adapter;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import fr.nextbanking.documents.infrastructure.Conteneurs;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Outils des tests d'intégration : requêtes HTTP, jetons forgés, données de référence du module.
 */
public final class OutilsHttp {

    private static final Path REFERENCE_DATA = Path.of("reference-data/reference-data.sql");

    private OutilsHttp() {
    }

    /**
     * Remet les tables {@code documents}, {@code comptes}, {@code clients} et {@code evenements_traites} aux données
     * de référence (le fichier fait reprendre la séquence des documents après le dernier identifiant).
     */
    public static void chargerDonnees(DataSource base) {
        try {
            executer(base, "truncate table documents, comptes, clients, evenements_traites restart identity;\n"
                    + Files.readString(REFERENCE_DATA, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Ajoute un compte à la projection (en version 1), par exemple celui d'un autre client. */
    public static void insererCompte(DataSource base, long compteId, long clientId, String numero, String type,
            String solde) {
        executer(base, "insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)"
                + " values (" + compteId + ", " + clientId + ", '" + numero + "', '" + type + "', '" + numero
                + "', 'DEMOFR21XXX', " + solde + ", 1)");
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

    /** GET authentifié dont le corps est lu en octets (un PDF). */
    public static HttpResponse<byte[]> octetsAvecJeton(int port, String chemin, String jeton)
            throws IOException, InterruptedException {
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .header("Authorization", "Bearer " + jeton).GET().build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(requete, HttpResponse.BodyHandlers.ofByteArray());
        }
    }

    public static HttpResponse<String> avecJeton(int port, String methode, String chemin, String jeton)
            throws IOException, InterruptedException {
        return envoyer(port, methode, chemin, null, Map.of("Authorization", "Bearer " + jeton));
    }

    /** POST JSON authentifié. */
    public static HttpResponse<String> publier(int port, String chemin, String corps, String jeton)
            throws IOException, InterruptedException {
        return envoyer(port, "POST", chemin, corps,
                Map.of("Authorization", "Bearer " + jeton, "Content-Type", "application/json"));
    }

    /** Jeton forgé avec la paire donnée : sujet, rôle, émission et expiration au choix. */
    public static String jeton(KeyPair cles, String sujet, String role, Instant emission, Instant expiration) {
        RSAKey cle = new RSAKey.Builder((RSAPublicKey) cles.getPublic()).privateKey((RSAPrivateKey) cles.getPrivate())
                .build();
        JwtClaimsSet revendications = JwtClaimsSet.builder().subject(sujet).claim("roles", List.of(role))
                .id(UUID.randomUUID().toString()).issuedAt(emission).expiresAt(expiration).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(cle)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), revendications))
                .getTokenValue();
    }

    /** Jeton valide d'un client, signé par la clé du service de test. */
    public static String jetonValide(String sujet) {
        return jetonValide(sujet, "CLIENT");
    }

    /** Jeton valide du rôle donné, signé par la clé du service de test. */
    public static String jetonValide(String sujet, String role) {
        Instant maintenant = Instant.now();
        return jeton(Conteneurs.CLES, sujet, role, maintenant, maintenant.plusSeconds(1800));
    }
}
