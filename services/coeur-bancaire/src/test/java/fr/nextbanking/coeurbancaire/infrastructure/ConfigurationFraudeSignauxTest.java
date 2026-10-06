package fr.nextbanking.coeurbancaire.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ParametresControleFraude;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Seuils {@code nb.fraude.*} du contrôle de fraude (NFR-SEC-08) : valides, ils sont lus ; absents, illisibles, nuls ou
 * négatifs, le service refuse de démarrer.
 */
@Tag("FR-027-01")
@Tag("NFR-SEC-08")
class ConfigurationFraudeSignauxTest {

    /** Seuils du contrôle de fraude seuls (les seuils 3.1 sont lus par {@code ConfigurationFraudeTest}). */
    static final String[] PROPRIETES_SIGNAUX = {
        "nb.fraude.score-cumule-max=75",
        "nb.fraude.montant-eleve=5000", "nb.fraude.montant-tres-eleve=15000", "nb.fraude.plafond-quotidien=8",
        "nb.fraude.succession-rapide-secondes=30", "nb.fraude.velocite-fenetre-minutes=60",
        "nb.fraude.velocite-nombre-max=5", "nb.fraude.velocite-montant=500",
        "nb.fraude.seuil-fraude=10000", "nb.fraude.acces-historique.delai=2s"};

    private static final String[] PROPRIETES_VALIDES = Stream.concat(
            Stream.of("nb.fraude.seuil-attente=5000", "nb.fraude.seuil-alerte=5000"), Arrays.stream(PROPRIETES_SIGNAUX))
            .toArray(String[]::new);

    private static final String[] PROPRIETES = {"score-cumule-max", "montant-eleve", "montant-tres-eleve",
        "plafond-quotidien", "succession-rapide-secondes", "velocite-fenetre-minutes", "velocite-nombre-max",
        "velocite-montant", "seuil-fraude", "acces-historique.delai"};

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withUserConfiguration(ConfigurationFraude.class);

    @Test
    void des_seuils_valides_sont_lus() {
        contexte.withPropertyValues(PROPRIETES_VALIDES).run(c -> {
            assertThat(c).hasNotFailed();
            ParametresControleFraude p = c.getBean(ParametresControleFraude.class);
            assertThat(p).isEqualTo(new ParametresControleFraude(75, new BigDecimal("5000"),
                    new BigDecimal("15000"), 8, 30, 60, 5, new BigDecimal("500")));
        });
    }

    @Test
    void un_seuil_negatif_ou_nul_empeche_le_demarrage() {
        for (String propriete : PROPRIETES) {
            for (String valeur : new String[] {"0", "-1"}) {
                contexte.withPropertyValues(PROPRIETES_VALIDES).withPropertyValues("nb.fraude." + propriete + "=" + valeur)
                        .run(c -> assertThat(c).as(propriete + "=" + valeur).hasFailed().getFailure().rootCause()
                                .hasMessageContaining("nb.fraude." + propriete));
            }
        }
    }

    @Test
    void un_seuil_absent_ou_illisible_empeche_le_demarrage() {
        for (String propriete : PROPRIETES) {
            String[] sans = Arrays.stream(PROPRIETES_VALIDES)
                    .filter(p -> !p.startsWith("nb.fraude." + propriete + "=")).toArray(String[]::new);
            contexte.withPropertyValues(sans).run(c -> assertThat(c).as(propriete).hasFailed());
            contexte.withPropertyValues(PROPRIETES_VALIDES).withPropertyValues("nb.fraude." + propriete + "=beaucoup")
                    .run(c -> assertThat(c).as(propriete).hasFailed());
        }
    }
}
