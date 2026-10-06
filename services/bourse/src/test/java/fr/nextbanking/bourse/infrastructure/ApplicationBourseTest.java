package fr.nextbanking.bourse.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.bourse.application.port.in.AjouterValeur;
import fr.nextbanking.bourse.application.port.in.ConsulterWatchlist;
import fr.nextbanking.bourse.application.port.in.RetirerValeur;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = ApplicationBourse.class)
@DirtiesContext
class ApplicationBourseTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private ConsulterWatchlist consulterWatchlist;

    @Autowired
    private AjouterValeur ajouterValeur;

    @Autowired
    private RetirerValeur retirerValeur;

    @Test
    void les_cas_d_usage_sont_assembles() {
        assertThat(consulterWatchlist).isNotNull();
        assertThat(ajouterValeur).isNotNull();
        assertThat(retirerValeur).isNotNull();
    }

    @Test
    @Tag("FR-009-01")
    void refuse_de_demarrer_sans_cle_publique() {
        assertThatThrownBy(() -> demarrer(avec("--nb.jeton.cle-publique=")))
                .hasStackTraceContaining("nb.jeton.cle-publique est obligatoire");
    }

    @Test
    void refuse_de_demarrer_sans_mot_de_passe_de_base() {
        assertThatThrownBy(() -> demarrer(avec("--spring.datasource.password=")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password est obligatoire");
    }

    @Test
    void main_demarre_le_service() {
        List<String> arguments = new ArrayList<>(Conteneurs.arguments());
        arguments.add("--server.port=0");
        arguments.add("--spring.main.sources=" + ContexteDemarre.class.getName());
        ApplicationBourse.main(arguments.toArray(String[]::new));

        try (ConfigurableApplicationContext contexte = ContexteDemarre.retirer()) {
            assertThat(contexte).isNotNull();
            assertThat(contexte.isActive()).isTrue();
        }
    }

    // Valeur vide explicite : un argument prime sur une variable d'environnement exportée.
    private static List<String> avec(String argument) {
        String prefixe = argument.substring(0, argument.indexOf('=') + 1);
        List<String> arguments = new ArrayList<>(Conteneurs.arguments().stream()
                .filter(existant -> !existant.startsWith(prefixe)).toList());
        arguments.add(argument);
        return arguments;
    }

    // Arguments de ligne de commande : priment sur application.properties. Contexte web (sécurité comprise).
    private static void demarrer(List<String> arguments) {
        List<String> complets = new ArrayList<>(arguments);
        complets.add("--server.port=0");
        new SpringApplicationBuilder(ApplicationBourse.class)
                .web(WebApplicationType.SERVLET)
                .run(complets.toArray(String[]::new))
                .close();
    }
}
