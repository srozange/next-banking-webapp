package fr.nextbanking.coeurbancaire.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
class ApplicationCoeurBancaireTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private Horloge horloge;

    @Autowired
    private NewTopic topicTemoinEcrit;

    @Test
    void l_horloge_est_dans_le_fuseau_metier() {
        assertThat(horloge.fuseau()).isEqualTo(ZoneId.of("Europe/Paris"));
    }

    @Test
    void le_topic_du_temoin_est_declare_avec_3_partitions_et_une_replique() {
        assertThat(topicTemoinEcrit.name()).isEqualTo("coeur-bancaire.temoin-ecrit.v1");
        assertThat(topicTemoinEcrit.numPartitions()).isEqualTo(3);
        assertThat(topicTemoinEcrit.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void refuse_de_demarrer_avec_un_fuseau_invalide() {
        List<String> arguments = new ArrayList<>(Conteneurs.arguments());
        arguments.add("--nb.fuseau=Mars/Olympus");

        assertThatThrownBy(() -> demarrer(arguments.toArray(String[]::new)))
                .hasStackTraceContaining("nb.fuseau n'est pas un fuseau valide : Mars/Olympus");
    }

    @Test
    void refuse_de_demarrer_sans_mot_de_passe_de_base() {
        assertThatThrownBy(() -> demarrer(Conteneurs.arguments().stream()
                        .filter(argument -> !argument.startsWith("--spring.datasource.password="))
                        .toArray(String[]::new)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password est obligatoire");
    }

    @Test
    void main_demarre_le_service() {
        List<String> arguments = new ArrayList<>(Conteneurs.arguments());
        arguments.add("--server.port=0");
        arguments.add("--spring.main.sources=" + ContexteDemarre.class.getName());
        ApplicationCoeurBancaire.main(arguments.toArray(String[]::new));

        try (ConfigurableApplicationContext contexte = ContexteDemarre.retirer()) {
            assertThat(contexte).isNotNull();
            assertThat(contexte.isActive()).isTrue();
        }
    }

    // Arguments de ligne de commande : priment sur application.properties.
    private static void demarrer(String... arguments) {
        new SpringApplicationBuilder(ApplicationCoeurBancaire.class)
                .web(WebApplicationType.NONE)
                .run(arguments)
                .close();
    }
}
