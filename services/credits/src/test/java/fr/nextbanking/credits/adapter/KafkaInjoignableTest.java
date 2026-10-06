package fr.nextbanking.credits.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
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
 * Vrai {@code KafkaTemplate} sans broker joignable : la demande répond 201 dans un délai borné (« transmise » = ligne
 * d'outbox validée) et la ligne reste au relais.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class KafkaInjoignableTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Ces tests constatent le prêt EN_ATTENTE : le scoring (écouteur) ne doit pas le décider.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
        registre.add("spring.kafka.bootstrap-servers", () -> "localhost:1");
        registre.add("spring.kafka.admin.auto-create", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @Tag("FR-024-01")
    @Tag("FR-007-07")
    void la_demande_repond_201_en_temps_borne_et_laisse_la_ligne_au_relais() throws Exception {
        OutilsHttp.chargerDonnees(base);

        HttpResponse<String> reponse = OutilsHttp.publier(port, "/api/credits/prets",
                "{\"type\":\"CONSOMMATION\",\"montant\":\"10000\",\"tauxAnnuel\":\"3.5\",\"dureeMois\":\"36\","
                        + "\"objet\":\"kafka injoignable\"}", OutilsHttp.jetonValide("5"));

        assertThat(reponse.statusCode()).isEqualTo(201);
        long pretId = json.readTree(reponse.body()).get("id").asLong();
        assertThat(jdbc.sql("select publie from outbox where cle = :cle").param("cle", Long.toString(pretId))
                .query(Boolean.class).single()).isFalse();
    }
}
