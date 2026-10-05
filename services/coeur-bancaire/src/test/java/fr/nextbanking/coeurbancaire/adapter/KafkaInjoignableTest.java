package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
 * Vrai {@code KafkaTemplate} sans broker joignable : le POST répond 201 dans un délai borné (délais de
 * {@code KafkaAdmin} et {@code max.block.ms}, bien en dessous des 60 s de nginx), et la ligne d'outbox reste au relais.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class KafkaInjoignableTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        registre.add("spring.datasource.url", Conteneurs.POSTGRES::getJdbcUrl);
        registre.add("spring.datasource.username", Conteneurs.POSTGRES::getUsername);
        registre.add("spring.datasource.password", Conteneurs.POSTGRES::getPassword);
        registre.add("spring.kafka.bootstrap-servers", () -> "localhost:1");
        registre.add("spring.kafka.admin.auto-create", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void le_post_repond_201_en_temps_borne_et_laisse_la_ligne_au_relais() throws Exception {
        HttpResponse<String> reponse;
        try (HttpClient client = HttpClient.newHttpClient()) {
            reponse = client.send(HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + "/api/coeur-bancaire/temoin"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"libelle\":\"kafka injoignable\"}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
        }

        assertThat(reponse.statusCode()).isEqualTo(201);
        long temoinId = json.readTree(reponse.body()).get("temoinId").asLong();
        assertThat(jdbc.sql("select publie from outbox where cle = :cle").param("cle", Long.toString(temoinId))
                .query(Boolean.class).single()).isFalse();
    }
}
