package fr.nextbanking.notifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.notifications.adapter.in.RecepteurTemoin;
import fr.nextbanking.notifications.adapter.out.RegistreTemoinsRecusEnMemoire;
import fr.nextbanking.notifications.domain.TemoinRecu;
import fr.nextbanking.notifications.application.port.out.Horloge;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApplicationNotifications.class)
class ApplicationNotificationsTest {

    @Autowired
    private RecepteurTemoin recepteur;

    @Autowired
    private RegistreTemoinsRecusEnMemoire registre;

    @Autowired
    private Horloge horloge;

    @Test
    void le_contexte_assemble_le_recepteur_jusqu_au_registre() {
        TemoinRecu temoin = recepteur.recevoir(7L);

        assertThat(registre.tous()).contains(temoin);
    }

    @Test
    void l_horloge_est_dans_le_fuseau_metier() {
        assertThat(horloge.fuseau()).isEqualTo(ZoneId.of("Europe/Paris"));
    }

    @Test
    void refuse_de_demarrer_avec_un_fuseau_invalide() {
        SpringApplicationBuilder application = new SpringApplicationBuilder(ApplicationNotifications.class)
                .web(WebApplicationType.NONE);

        // Argument de ligne de commande : prime sur application.properties.
        assertThatThrownBy(() -> application.run("--nb.fuseau=Mars/Olympus"))
                .hasStackTraceContaining("nb.fuseau n'est pas un fuseau valide : Mars/Olympus");
    }
}
