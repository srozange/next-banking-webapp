package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.coeurbancaire.application.port.in.EcrireTemoin;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Kafka indisponible à la publication : l'écriture réussit, la ligne reste au relais, qui s'arrête à la première
 * ligne en échec, puis publie dans l'ordre quand Kafka revient.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
class EchecPublicationTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @MockitoBean
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private EcrireTemoin ecrireTemoin;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void l_ecriture_reussit_et_le_relais_republie_dans_l_ordre_quand_kafka_revient() {
        // Un seul bouchon, sûr entre fils : le relais planifié appelle send pendant le test.
        AtomicBoolean kafkaDisponible = new AtomicBoolean(false);
        when(kafka.send(anyString(), anyString(), anyString())).thenAnswer(appel -> kafkaDisponible.get()
                ? CompletableFuture.<SendResult<String, String>>completedFuture(null)
                : CompletableFuture.<SendResult<String, String>>failedFuture(
                        new IllegalStateException("Kafka indisponible")));
        jdbc.sql("update outbox set publie = true").update();

        long premier = ecrireTemoin.ecrire("premier en échec");
        long second = ecrireTemoin.ecrire("second en échec");

        assertThat(publie(premier)).isFalse();
        assertThat(publie(second)).isFalse();
        // Le relais retente la première ligne et ne passe jamais à la suivante : la seconde n'a connu
        // que la tentative faite après validation, dans le fil de l'écriture.
        verify(kafka, after(3000).atLeast(3)).send(anyString(), eq(Long.toString(premier)), anyString());
        verify(kafka, times(1)).send(anyString(), eq(Long.toString(second)), anyString());

        kafkaDisponible.set(true);

        await().atMost(Duration.ofSeconds(10)).until(() -> publie(premier) && publie(second));
    }

    private boolean publie(long temoinId) {
        return jdbc.sql("select publie from outbox where cle = :cle").param("cle", Long.toString(temoinId))
                .query(Boolean.class).single();
    }
}
