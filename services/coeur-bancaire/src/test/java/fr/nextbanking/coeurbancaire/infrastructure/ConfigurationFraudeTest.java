package fr.nextbanking.coeurbancaire.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Seuils {@code nb.fraude.*} (NFR-SEC-08) : valides, ils sont lus ; absents, illisibles, nuls ou négatifs, le service
 * refuse de démarrer.
 */
@Tag("FR-003-10")
@Tag("NFR-SEC-08")
class ConfigurationFraudeTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withUserConfiguration(ConfigurationFraude.class);

    @Test
    void des_seuils_valides_sont_lus() {
        contexte.withPropertyValues("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=5000.50")
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    SeuilsFraude seuils = c.getBean(SeuilsFraude.class);
                    assertThat(seuils.seuilAttente()).isEqualByComparingTo(new BigDecimal("5000"));
                    assertThat(seuils.seuilAlerte()).isEqualByComparingTo(new BigDecimal("5000.50"));
                });
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
