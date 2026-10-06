package fr.nextbanking.credits.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * Cas d'échec de l'envoi qui ne demandent ni base ni Kafka.
 */
class EvenementsSortantsKafkaTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    @Test
    void un_envoi_interrompu_laisse_la_ligne_au_relais_et_garde_l_interruption() throws Exception {
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> envoi = mock(CompletableFuture.class);
        when(envoi.get(anyLong(), org.mockito.ArgumentMatchers.any(TimeUnit.class)))
                .thenThrow(new InterruptedException());
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(envoi);
        EvenementsSortantsKafka evenements = new EvenementsSortantsKafka(null, kafka, null);

        try {
            assertThat(evenements.envoyer(new LigneOutbox(1L, "t", "1", "{}"))).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void le_relais_s_arrete_a_la_premiere_ligne_en_echec() {
        EvenementsSortantsKafka evenements = mock(EvenementsSortantsKafka.class);
        LigneOutbox premiere = new LigneOutbox(1L, "t", "1", "{}");
        LigneOutbox seconde = new LigneOutbox(2L, "t", "2", "{}");
        when(evenements.nonPubliees()).thenReturn(List.of(premiere, seconde));
        when(evenements.envoyer(premiere)).thenReturn(false);

        new RelaisOutbox(evenements).relayer();

        verify(evenements, never()).envoyer(seconde);
    }
}
