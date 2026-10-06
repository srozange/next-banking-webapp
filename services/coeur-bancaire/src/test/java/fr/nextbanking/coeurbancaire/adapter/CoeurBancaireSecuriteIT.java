package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Contrôle d'accès du cœur bancaire : sans jeton valide (absent, mal signé, expiré, illisible), tout {@code /api/**}
 * rend 401 « Authentification requise », sans pile ni cookie ; la santé et le témoin restent publics.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-011-09")
class CoeurBancaireSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Test
    void sans_jeton_valide_tout_rend_401_sauf_le_temoin() throws Exception {
        Instant maintenant = Instant.now();
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = Jetons.jeton(generateur.generateKeyPair(), "ADMIN", maintenant, maintenant.plusSeconds(1800));
        String expire = Jetons.jeton(Conteneurs.CLES, "ADMIN", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));
        List<String[]> requetes = List.of(
                new String[] {"GET", "/api/coeur-bancaire/traitements", null},
                new String[] {"GET", "/api/coeur-bancaire/traitements?limite=50", null},
                new String[] {"GET", "/api/coeur-bancaire/temoin", null},
                new String[] {"GET", "/api/coeur-bancaire/inconnu", null},
                new String[] {"GET", "/actuator/info", null});

        for (String[] requete : requetes) {
            for (String jeton : new String[] {null, malSigne, expire, "illisible"}) {
                HttpResponse<String> reponse = Jetons.envoyer(port, requete[0], requete[1], jeton, requete[2]);
                assertThat(reponse.statusCode()).as(requete[0] + " " + requete[1]).isEqualTo(401);
                assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
                assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
            }
        }
    }

    @Test
    void le_temoin_reste_public() throws Exception {
        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", "/api/coeur-bancaire/temoin", null,
                "{\"libelle\":\"sans jeton\"}");

        assertThat(reponse.statusCode()).isEqualTo(201);
    }

    @Test
    void la_sante_est_publique() throws Exception {
        assertThat(Jetons.envoyer(port, "GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
    }
}
