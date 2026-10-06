package fr.nextbanking.openbankingconseiller.adapter.in.http;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Sécurité du service open-banking-conseiller : API sans état à jeton porteur (ni session, ni cookie, ni CSRF), tout authentifié sauf
 * {@code GET /actuator/health}, sans restriction de rôle (CLIENT, CONSEILLER et ADMIN identiques, FR-010-01). Le jeton
 * est vérifié par la clé publique {@code nb.jeton.cle-publique}. Refus en JSON : 401 « Authentification requise ».
 */
@Configuration
public class ConfigurationSecurite {

    static final String PROPRIETE_CLE_PUBLIQUE = "nb.jeton.cle-publique";

    private static final RequestMatcher SANTE = PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/actuator/health");

    @Bean
    SecurityFilterChain chaineDeSecurite(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint nonAuthentifie = (requete, reponse, erreur) ->
                ecrire(reponse, HttpServletResponse.SC_UNAUTHORIZED, Textes.AUTHENTIFICATION_REQUISE);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acces -> acces
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(SANTE).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(serveur -> serveur
                        .jwt(jwt -> { })
                        .authenticationEntryPoint(nonAuthentifie))
                .exceptionHandling(erreurs -> erreurs.authenticationEntryPoint(nonAuthentifie))
                .build();
    }

    @Bean
    JwtDecoder decodeurDeJeton(@Value("${nb.jeton.cle-publique:}") String clePublique) {
        return NimbusJwtDecoder.withPublicKey(lireClePublique(clePublique)).build();
    }

    static RSAPublicKey lireClePublique(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("La propriété " + PROPRIETE_CLE_PUBLIQUE
                    + " est obligatoire (variable d'environnement NB_JETON_CLE_PUBLIQUE).");
        }
        try {
            byte[] der = Base64.getMimeDecoder().decode(valeur.replaceAll("-----[A-Z ]+-----", ""));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException("La propriété " + PROPRIETE_CLE_PUBLIQUE
                    + " n'est pas une clé publique RSA X.509 lisible.");
        }
    }

    private static void ecrire(HttpServletResponse reponse, int statut, String texte) throws IOException {
        reponse.setStatus(statut);
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.getWriter().write("{\"message\":\"" + texte + "\"}");
    }
}
