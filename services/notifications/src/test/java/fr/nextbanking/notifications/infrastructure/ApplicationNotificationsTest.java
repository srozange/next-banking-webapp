package fr.nextbanking.notifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.notifications.adapter.in.RecepteurTemoin;
import fr.nextbanking.notifications.adapter.out.RegistreTemoinsRecusEnMemoire;
import fr.nextbanking.notifications.domain.TemoinRecu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApplicationNotifications.class)
class ApplicationNotificationsTest {

    @Autowired
    private RecepteurTemoin recepteur;

    @Autowired
    private RegistreTemoinsRecusEnMemoire registre;

    @Test
    void le_contexte_assemble_le_recepteur_jusqu_au_registre() {
        TemoinRecu temoin = recepteur.recevoir(7L);

        assertThat(registre.tous()).contains(temoin);
    }
}
