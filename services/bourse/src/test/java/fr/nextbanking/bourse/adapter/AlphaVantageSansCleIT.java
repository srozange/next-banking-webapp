package fr.nextbanking.bourse.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.bourse.adapter.out.fournisseur.BouchonAlphaVantage;
import fr.nextbanking.bourse.adapter.out.fournisseur.ParametresDeploiement;
import fr.nextbanking.bourse.infrastructure.ApplicationBourse;
import fr.nextbanking.bourse.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Mode réel sans clé : aucune requête n'est émise vers le fournisseur (aucune clé par défaut), la page se comporte
 * comme une absence de données, et l'avertissement est tracé.
 */
@SpringBootTest(classes = ApplicationBourse.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nb.fournisseurs.alphavantage.mode=reel")
@Import({HorlogeFixe.class, AlphaVantageSansCleIT.Parametres.class})
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class AlphaVantageSansCleIT {

    static final BouchonAlphaVantage BOUCHON = new BouchonAlphaVantage();
    private static final Pattern TRACE = Pattern.compile("traceId=(\\S*) spanId=(\\S*)\\]");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @AfterAll
    static void arreter() {
        BOUCHON.close();
    }

    @LocalServerPort
    private int port;

    @Test
    @Tag("FR-029-05")
    @Tag("FR-009-14")
    @Tag("BUG-091")
    @Tag("BUG-034")
    void sans_cle_aucune_requete_n_est_emise_et_l_historique_est_vide(CapturedOutput sortie) throws Exception {
        for (String periode : new String[] {"1M", "1Y"}) {
            HttpResponse<String> reponse = Jetons.envoyer(port, "GET",
                    "/api/bourse/historique?symbole=AI.PA&periode=" + periode, HistoriqueIT.jeton("CLIENT"), null);
            assertThat(reponse.statusCode()).isEqualTo(200);
            assertThat(reponse.body()).isEqualTo("{\"symbole\":\"AI.PA\",\"periode\":\"" + periode + "\",\"points\":[]}");
        }

        assertThat(BOUCHON.nombreDeRequetes()).isZero();
        assertThat(Arrays.stream(sortie.getOut().split("\\R")).filter(ligne -> ligne.contains("clé absente")))
                .hasSize(2).allSatisfy(ligne -> {
                    assertThat(ligne).contains(" WARN ");
                    Matcher trace = TRACE.matcher(ligne);
                    assertThat(trace.find()).isTrue();
                    assertThat(trace.group(1)).isNotEmpty();
                });
    }

    /** Adresse du bouchon, aucune clé, quel que soit l'environnement du processus. */
    @TestConfiguration
    static class Parametres {

        @Bean
        @Primary
        ParametresDeploiement parametresSansCle() {
            return new ParametresDeploiement(
                    nom -> nom.equals("ALPHA_VANTAGE_BASE_URL") ? BOUCHON.adresse() : null, nom -> null);
        }
    }
}
