package fr.nextbanking.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.audit.application.port.in.EnregistrerEntreeAudit;
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

@SpringBootTest(classes = ApplicationAudit.class)
@DirtiesContext
class ApplicationAuditTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private EnregistrerEntreeAudit enregistrerEntreeAudit;

    @Test
    void le_cas_d_usage_est_assemble() {
        assertThat(enregistrerEntreeAudit).isNotNull();
    }

    @Test
    @Tag("FR-026-06")
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
        ApplicationAudit.main(arguments.toArray(String[]::new));

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
        new SpringApplicationBuilder(ApplicationAudit.class)
                .web(WebApplicationType.SERVLET)
                .run(complets.toArray(String[]::new))
                .close();
    }
}
