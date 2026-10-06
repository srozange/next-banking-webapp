package fr.nextbanking.identiteclients.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Plage de confiance par défaut ({@code 172.30.0.0/24}) : l'appelant (boucle locale) en est hors, son
 * {@code X-Forwarded-For} est ignoré et l'adresse distante est retenue.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-001-13")
@Tag("FR-026-05")
@Tag("BUG-083")
class AdresseIpHorsPlageIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    @Test
    void l_en_tete_venant_d_une_adresse_hors_plage_est_ignore() throws Exception {
        assertThat(AdresseIpIT.adresseAuditee(port, jdbc, "203.0.113.7", AdresseIpIT.USER1))
                .isIn(AdresseIpIT.BOUCLE_LOCALE);
        assertThat(AdresseIpIT.adresseAuditee(port, jdbc, null, AdresseIpIT.USER1))
                .isIn(AdresseIpIT.BOUCLE_LOCALE);
    }
}
