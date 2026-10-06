package fr.nextbanking.identiteclients.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.application.port.in.ConsulterProfil;
import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
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

@SpringBootTest(classes = ApplicationIdentiteClients.class)
@DirtiesContext
class ApplicationIdentiteClientsTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private SeConnecter seConnecter;

    @Autowired
    private ConsulterProfil consulterProfil;

    @Autowired
    private Horloge horloge;

    @Test
    void les_cas_d_usage_sont_assembles() {
        assertThat(seConnecter).isNotNull();
        assertThat(consulterProfil).isNotNull();
    }

    @Test
    void refuse_de_demarrer_sans_cle_privee() {
        assertThatThrownBy(() -> demarrer(avec("--nb.jeton.cle-privee=")))
                .hasStackTraceContaining("nb.jeton.cle-privee est obligatoire");
    }

    @Test
    void refuse_de_demarrer_avec_une_cle_privee_illisible_sans_la_reprendre() {
        assertThatThrownBy(() -> demarrer(avec("--nb.jeton.cle-privee=pas-une-cle")))
                .hasStackTraceContaining("nb.jeton.cle-privee n'est pas une clé privée RSA PKCS#8 lisible")
                .satisfies(erreur -> assertThat(trace(erreur)).doesNotContain("pas-une-cle"));
    }

    @Test
    void refuse_de_demarrer_sans_cle_publique() {
        assertThatThrownBy(() -> demarrer(avec("--nb.jeton.cle-publique=")))
                .hasStackTraceContaining("nb.jeton.cle-publique est obligatoire");
    }

    @Test
    @Tag("FR-001-13")
    void refuse_de_demarrer_avec_une_plage_de_confiance_illisible() {
        assertThatThrownBy(() -> demarrer(avec("--nb.reseau.mandataires-de-confiance=172.30.0.0/99")))
                .hasStackTraceContaining("nb.reseau.mandataires-de-confiance contient une plage CIDR illisible");
    }

    @Test
    @Tag("FR-001-13")
    void refuse_de_demarrer_sans_plage_de_confiance() {
        assertThatThrownBy(() -> demarrer(avec("--nb.reseau.mandataires-de-confiance=")))
                .hasStackTraceContaining("nb.reseau.mandataires-de-confiance est obligatoire");
    }

    @Test
    void refuse_de_demarrer_avec_un_fuseau_invalide() {
        assertThatThrownBy(() -> demarrer(avec("--nb.fuseau=Mars/Olympus")))
                .hasStackTraceContaining("nb.fuseau n'est pas un fuseau valide : Mars/Olympus");
    }

    @Test
    void l_horloge_est_dans_le_fuseau_metier() {
        assertThat(horloge.fuseau()).isEqualTo(java.time.ZoneId.of("Europe/Paris"));
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
        ApplicationIdentiteClients.main(arguments.toArray(String[]::new));

        try (ConfigurableApplicationContext contexte = ContexteDemarre.retirer()) {
            assertThat(contexte).isNotNull();
            assertThat(contexte.isActive()).isTrue();
        }
    }

    // Valeur vide explicite (ou remplacée) : un argument prime sur une variable d'environnement exportée.
    private static List<String> avec(String argument) {
        String prefixe = argument.substring(0, argument.indexOf('=') + 1);
        List<String> arguments = new ArrayList<>(Conteneurs.arguments().stream()
                .filter(existant -> !existant.startsWith(prefixe)).toList());
        arguments.add(argument);
        return arguments;
    }

    private static String trace(Throwable erreur) {
        java.io.StringWriter texte = new java.io.StringWriter();
        erreur.printStackTrace(new java.io.PrintWriter(texte));
        return texte.toString();
    }

    // Arguments de ligne de commande : priment sur application.properties. Contexte web (sécurité comprise).
    private static void demarrer(List<String> arguments) {
        List<String> complets = new ArrayList<>(arguments);
        complets.add("--server.port=0");
        new SpringApplicationBuilder(ApplicationIdentiteClients.class)
                .web(WebApplicationType.SERVLET)
                .run(complets.toArray(String[]::new))
                .close();
    }
}
