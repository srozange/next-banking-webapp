package fr.nextbanking.coeurbancaire.adapter.out.evenements;

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
 * Cas d'échec de l'envoi des lignes d'{@code outbox_audit} qui ne demandent ni base ni Kafka : la ligne reste au
 * relais, qui s'arrête à la première ligne en échec.
 */
@Tag("FR-026-03")
class JournalAuditKafkaTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    @Test
    void un_envoi_interrompu_laisse_la_ligne_au_relais_et_garde_l_interruption() throws Exception {
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> envoi = mock(CompletableFuture.class);
        when(envoi.get(anyLong(), org.mockito.ArgumentMatchers.any(TimeUnit.class)))
                .thenThrow(new InterruptedException());
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(envoi);
        JournalAuditKafka journal = new JournalAuditKafka(null, kafka, null);

        try {
            assertThat(journal.envoyer(new LigneOutboxAudit(1L, "t", "1", "{}"))).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void un_envoi_en_echec_laisse_la_ligne_au_relais() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka indisponible")));

        assertThat(new JournalAuditKafka(null, kafka, null).envoyer(new LigneOutboxAudit(1L, "t", "1", "{}")))
                .isFalse();
    }

    @Test
    void le_relais_s_arrete_a_la_premiere_ligne_en_echec() {
        JournalAuditKafka journal = mock(JournalAuditKafka.class);
        LigneOutboxAudit premiere = new LigneOutboxAudit(1L, "t", "1", "{}");
        LigneOutboxAudit seconde = new LigneOutboxAudit(2L, "t", "2", "{}");
        when(journal.nonPubliees()).thenReturn(List.of(premiere, seconde));
        when(journal.envoyer(premiere)).thenReturn(false);

        new RelaisOutboxAudit(journal).relayer();

        verify(journal, never()).envoyer(seconde);
    }

    @Test
    void le_relais_envoie_toutes_les_lignes_quand_tout_passe() {
        JournalAuditKafka journal = mock(JournalAuditKafka.class);
        LigneOutboxAudit premiere = new LigneOutboxAudit(1L, "t", "1", "{}");
        LigneOutboxAudit seconde = new LigneOutboxAudit(2L, "t", "2", "{}");
        when(journal.nonPubliees()).thenReturn(List.of(premiere, seconde));
        when(journal.envoyer(premiere)).thenReturn(true);
        when(journal.envoyer(seconde)).thenReturn(true);

        new RelaisOutboxAudit(journal).relayer();

        verify(journal).envoyer(seconde);
    }
}
