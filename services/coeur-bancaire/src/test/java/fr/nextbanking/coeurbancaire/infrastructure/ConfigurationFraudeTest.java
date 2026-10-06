package fr.nextbanking.coeurbancaire.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.DelaiAccesHistorique;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Seuils {@code nb.fraude.*} (NFR-SEC-08) : valides, ils sont lus ; absents, illisibles, nuls ou négatifs, le service
 * refuse de démarrer.
 */
@Tag("FR-003-10")
@Tag("NFR-SEC-08")
@Tag("FR-027-02")
class ConfigurationFraudeTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withUserConfiguration(ConfigurationFraude.class)
            .withPropertyValues(ConfigurationFraudeSignauxTest.PROPRIETES_SIGNAUX);

    private static final String[] SANS_SEUIL_FRAUDE = Arrays.stream(ConfigurationFraudeSignauxTest.PROPRIETES_SIGNAUX)
            .filter(p -> !p.startsWith("nb.fraude.seuil-fraude=") && !p.startsWith("nb.fraude.acces-historique"))
            .toArray(String[]::new);

    @Test
    void des_seuils_valides_sont_lus() {
        contexte.withPropertyValues("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=5000.50")
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    SeuilsFraude seuils = c.getBean(SeuilsFraude.class);
                    assertThat(seuils.seuilAttente()).isEqualByComparingTo(new BigDecimal("5000"));
                    assertThat(seuils.seuilAlerte()).isEqualByComparingTo(new BigDecimal("5000.50"));
                    assertThat(seuils.seuilFraude()).isEqualByComparingTo(new BigDecimal("10000"));
                    assertThat(c.getBean(DelaiAccesHistorique.class).valeur()).isEqualTo(Duration.ofSeconds(2));
                });
    }

    @Test
    void un_seuil_de_fraude_ou_un_delai_d_acces_a_l_historique_nul_negatif_absent_ou_illisible_empeche_le_demarrage() {
        for (String valeur : new String[] {"0", "-1"}) {
            contexte.withPropertyValues("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=5000",
                    "nb.fraude.seuil-fraude=" + valeur)
                    .run(c -> assertThat(c).hasFailed().getFailure().rootCause()
                            .hasMessageContaining("nb.fraude.seuil-fraude"));
        }
        for (String valeur : new String[] {"0", "-1", "0s", "-2s"}) {
            contexte.withPropertyValues("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=5000",
                    "nb.fraude.acces-historique.delai=" + valeur)
                    .run(c -> assertThat(c).hasFailed().getFailure().rootCause()
                            .hasMessageContaining("nb.fraude.acces-historique.delai"));
        }
        new ApplicationContextRunner().withUserConfiguration(ConfigurationFraude.class)
                .withPropertyValues(SANS_SEUIL_FRAUDE)
                .withPropertyValues("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=5000")
                .run(c -> assertThat(c).hasFailed());
        contexte.withPropertyValues("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=5000",
                "nb.fraude.acces-historique.delai=longtemps").run(c -> assertThat(c).hasFailed());
    }

    @Test
    void un_seuil_d_attente_nul_ou_negatif_empeche_le_demarrage() {
        for (String valeur : new String[] {"0", "-1", "-0.01"}) {
            contexte.withPropertyValues("nb.fraude.seuil-attente=" + valeur, "nb.fraude.seuil-alerte=5000")
                    .run(c -> assertThat(c).hasFailed().getFailure().rootCause()
                            .hasMessageContaining("nb.fraude.seuil-attente"));
        }
    }

    @Test
    void un_seuil_d_alerte_nul_ou_negatif_empeche_le_demarrage() {
        for (String valeur : new String[] {"0", "-1"}) {
            contexte.withPropertyValues("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=" + valeur)
                    .run(c -> assertThat(c).hasFailed().getFailure().rootCause()
                            .hasMessageContaining("nb.fraude.seuil-alerte"));
        }
    }

    @Test
    void un_seuil_absent_ou_illisible_empeche_le_demarrage() {
        contexte.withPropertyValues("nb.fraude.seuil-alerte=5000").run(c -> assertThat(c).hasFailed());
        contexte.withPropertyValues("nb.fraude.seuil-attente=5000").run(c -> assertThat(c).hasFailed());
        contexte.withPropertyValues("nb.fraude.seuil-attente=beaucoup", "nb.fraude.seuil-alerte=5000")
                .run(c -> assertThat(c).hasFailed());
    }
}
