package fr.nextbanking.credits.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.util.Map;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Adresse IP de l'audit avec la plage de confiance par défaut du service (réseau compose) : l'appelant local est hors
 * plage, son en-tête {@code X-Forwarded-For} est ignoré et l'adresse distante est retenue (FR-026-05).
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-026-05")
class AdresseIpDecisionHttpIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
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
    void l_en_tete_d_un_appelant_hors_plage_est_ignore() throws Exception {
        long pretId = jdbc.sql("""
                insert into loans (customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (5, 'CONSOMMATION', 1200, 0, 12, 100, 1200, 0, 'x', 'EN_ATTENTE', date '2026-10-01',
                        date '2027-10-01') returning id""").query(Long.class).single();

        HttpResponse<String> reponse = OutilsHttp.envoyer(port, "POST",
                "/api/credits/administration/prets/" + pretId + "/refus", null,
                Map.of("Authorization", "Bearer " + OutilsHttp.jetonValide("10", "ADMIN"),
                        "X-Forwarded-For", "203.0.113.7"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        String ip = JsonMapper.builder().build()
                .readTree(jdbc.sql("select charge from outbox_audit").query(String.class).single())
                .get("donnees").get("adresseIp").asString();
        assertThat(ip).isIn("127.0.0.1", "0:0:0:0:0:0:0:1").isNotEqualTo("203.0.113.7");
    }
}
