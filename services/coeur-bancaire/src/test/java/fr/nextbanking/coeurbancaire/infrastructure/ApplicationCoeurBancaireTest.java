package fr.nextbanking.coeurbancaire.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.adapter.in.PointEntreeTemoin;
import fr.nextbanking.coeurbancaire.adapter.out.DepotTemoinsEnMemoire;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(classes = ApplicationCoeurBancaire.class)
class ApplicationCoeurBancaireTest {

    @Autowired
    private PointEntreeTemoin pointEntree;

    @Autowired
    private DepotTemoinsEnMemoire depot;

    @Autowired
    private Horloge horloge;

    @Test
    void le_contexte_assemble_le_point_d_entree_jusqu_au_depot() {
        Temoin temoin = pointEntree.ecrire("premier témoin");

        assertThat(depot.tous()).contains(temoin);
    }

    @Test
    void l_horloge_est_dans_le_fuseau_metier() {
        assertThat(horloge.fuseau()).isEqualTo(ZoneId.of("Europe/Paris"));
    }

    @Test
    void refuse_de_demarrer_avec_un_fuseau_invalide() {
        SpringApplicationBuilder application = new SpringApplicationBuilder(ApplicationCoeurBancaire.class)
                .web(WebApplicationType.NONE);

        // Argument de ligne de commande : prime sur application.properties.
        assertThatThrownBy(() -> application.run("--nb.fuseau=Mars/Olympus"))
                .hasStackTraceContaining("nb.fuseau n'est pas un fuseau valide : Mars/Olympus");
    }

    @Test
    void main_demarre_le_service() {
        ApplicationCoeurBancaire.main(new String[] {
            "--spring.main.web-application-type=none",
            "--spring.main.sources=" + ContexteDemarre.class.getName()
        });

        try (ConfigurableApplicationContext contexte = ContexteDemarre.retirer()) {
            assertThat(contexte).isNotNull();
            assertThat(contexte.isActive()).isTrue();
        }
    }
}
