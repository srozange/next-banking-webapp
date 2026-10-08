package fr.nextbanking.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-002-07")
class NotificationDuClientTest {

    private static final NotificationDuClient NOTIFICATION =
            new NotificationDuClient(5, 7, "ALERTE", "message", false, Instant.parse("2026-11-17T09:00:00Z"));

    @Test
    void n_appartient_qu_a_son_utilisateur() {
        assertThat(NOTIFICATION.appartientA(7)).isTrue();
        assertThat(NOTIFICATION.appartientA(8)).isFalse();
    }
}
