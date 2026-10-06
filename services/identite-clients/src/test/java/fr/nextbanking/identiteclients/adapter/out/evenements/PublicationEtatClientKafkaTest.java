package fr.nextbanking.identiteclients.adapter.out.evenements;

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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * Cas d'échec de l'envoi et ordre du relais, sans base ni Kafka.
 */
@Tag("FR-016-11")
class PublicationEtatClientKafkaTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    @Test
    void un_envoi_en_echec_laisse_la_ligne_au_relais() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("Kafka indisponible")));
        PublicationEtatClientKafka publication = new PublicationEtatClientKafka(null, kafka, null);

        assertThat(publication.envoyer(new LigneOutbox(1L, "t", "1", "{}"))).isFalse();
    }

    @Test
    void un_envoi_interrompu_laisse_la_ligne_au_relais_et_garde_l_interruption() throws Exception {
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> envoi = mock(CompletableFuture.class);
        when(envoi.get(anyLong(), org.mockito.ArgumentMatchers.any(TimeUnit.class)))
                .thenThrow(new InterruptedException());
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(envoi);
        PublicationEtatClientKafka publication = new PublicationEtatClientKafka(null, kafka, null);

        try {
            assertThat(publication.envoyer(new LigneOutbox(1L, "t", "1", "{}"))).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void le_relais_s_arrete_a_la_premiere_ligne_en_echec() {
        PublicationEtatClientKafka publication = mock(PublicationEtatClientKafka.class);
        LigneOutbox premiere = new LigneOutbox(1L, "t", "1", "{}");
        LigneOutbox seconde = new LigneOutbox(2L, "t", "2", "{}");
        when(publication.nonPubliees()).thenReturn(List.of(premiere, seconde));
        when(publication.envoyer(premiere)).thenReturn(false);

        new RelaisOutboxClients(publication).relayer();

        verify(publication, never()).envoyer(seconde);
    }

    @Test
    void le_relais_publie_toutes_les_lignes_dans_l_ordre() {
        PublicationEtatClientKafka publication = mock(PublicationEtatClientKafka.class);
        LigneOutbox premiere = new LigneOutbox(1L, "t", "1", "{}");
        LigneOutbox seconde = new LigneOutbox(2L, "t", "2", "{}");
        when(publication.nonPubliees()).thenReturn(List.of(premiere, seconde));
        when(publication.envoyer(premiere)).thenReturn(true);
        when(publication.envoyer(seconde)).thenReturn(true);

        new RelaisOutboxClients(publication).relayer();

        org.mockito.InOrder ordre = org.mockito.Mockito.inOrder(publication);
        ordre.verify(publication).envoyer(premiere);
        ordre.verify(publication).envoyer(seconde);
    }
}
