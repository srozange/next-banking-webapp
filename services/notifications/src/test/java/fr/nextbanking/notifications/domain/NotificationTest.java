package fr.nextbanking.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-002-09")
@Tag("FR-023-05")
@Tag("FR-023-06")
@Tag("FR-025-04")
@Tag("BUG-006")
@Tag("BUG-007")
class NotificationTest {

    private static final Instant T = Instant.parse("2026-11-17T09:00:00Z");

    @Test
    void une_notification_recue_est_non_lue_et_garde_le_texte_rendu() {
        Notification notification = Notification.recue(12, 7, "CONFIRMATION", null, "Virement de 100.00 EUR", T);

        assertThat(notification.lue()).isFalse();
        assertThat(notification.titre()).isNull();
        assertThat(notification.message()).isEqualTo("Virement de 100.00 EUR");
        assertThat(notification.creeLe()).isEqualTo(T);
    }

    @Test
    @Tag("FR-004-12")
    @Tag("FR-021-04")
    void une_notification_d_echeance_se_rattache_au_virement_programme_et_garde_son_titre() {
        Notification notification = Notification.recuePourEcheance(3, 7, "ALERTE", "Titre", "Texte", T);

        assertThat(notification.virementId()).isNull();
        assertThat(notification.virementProgrammeId()).isEqualTo(3L);
        assertThat(notification.titre()).isEqualTo("Titre");
        assertThat(notification.lue()).isFalse();
    }

    @Test
    @Tag("FR-004-12")
    @Tag("FR-021-04")
    void exactement_un_rattachement_positif_est_exige() {
        assertThatThrownBy(() -> new Notification(null, null, 7, "ALERTE", null, "m", false, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Notification(12L, 3L, 7, "ALERTE", null, "m", false, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recuePourEcheance(0, 7, "ALERTE", "t", "m", T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recuePourEcheance(3, 0, "ALERTE", "t", "m", T))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_une_notification_incomplete() {
        assertThatThrownBy(() -> Notification.recue(0, 7, "ALERTE", null, "m", T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recue(12, 0, "ALERTE", null, "m", T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recue(12, 7, null, null, "m", T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recue(12, 7, " ", null, "m", T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recue(12, 7, "ALERTE", null, null, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recue(12, 7, "ALERTE", null, " ", T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.recue(12, 7, "ALERTE", null, "m", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_plafond_doit_etre_strictement_positif() {
        assertThat(new PlafondNotifications(20).valeur()).isEqualTo(20);
        assertThatThrownBy(() -> new PlafondNotifications(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nb.notifications.plafond");
        assertThatThrownBy(() -> new PlafondNotifications(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
