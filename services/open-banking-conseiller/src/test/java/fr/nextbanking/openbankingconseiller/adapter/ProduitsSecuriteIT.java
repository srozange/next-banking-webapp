package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
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
 * Contrôle d'accès du service open-banking-conseiller : sans jeton valide (absent, mal signé, expiré, illisible),
 * tout {@code /api/**} rend 401 « Authentification requise », sans pile ni cookie ; la santé est publique.
 */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-010-01")
class ProduitsSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Test
    void sans_jeton_valide_tout_rend_401() throws Exception {
        Instant maintenant = Instant.now();
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = Jetons.jeton(generateur.generateKeyPair(), "ADMIN", maintenant, maintenant.plusSeconds(1800));
        String expire = Jetons.jeton(Conteneurs.CLES, "ADMIN", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));
        List<String[]> requetes = List.of(
                new String[] {"GET", "/api/open-banking-conseiller/produits", null},
                new String[] {"GET", "/api/open-banking-conseiller/produits?type=EPARGNE", null},
                new String[] {"POST", "/api/open-banking-conseiller/produits", "{}"},
                new String[] {"GET", "/api/open-banking-conseiller/inconnu", null},
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
    void une_requete_rejetee_par_le_pare_feu_passe_par_le_dispatch_d_erreur_et_ne_rend_que_le_message()
            throws Exception {
        Instant maintenant = Instant.now();
        String valide = Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant, maintenant.plusSeconds(1800));

        for (String jeton : new String[] {null, valide}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET", "/api/open-banking-conseiller//produits",
                    jeton, null);
            assertThat(reponse.statusCode()).as(jeton == null ? "sans jeton" : "avec jeton valide").isEqualTo(400);
            assertThat(reponse.body()).as(jeton == null ? "sans jeton" : "avec jeton valide")
                    .isEqualTo("{\"message\":\"Requête invalide\"}");
        }
    }

    @Test
    void la_sante_est_publique() throws Exception {
        assertThat(Jetons.envoyer(port, "GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
    }
}
