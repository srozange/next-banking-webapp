package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Notifications ALERTE d'une échéance non exécutée : textes de {@code textes-nouveaux.md} et événement. */
@Tag("FR-004-12")
@Tag("FR-004-13")
@Tag("BUG-014")
@Tag("BUG-068")
class NotificationVirementTest {

    private static final Instant T = Instant.parse("2026-11-17T09:00:00Z");

    @Test
    void le_solde_insuffisant_donne_une_alerte_titree() {
        NotificationVirement notification = NotificationVirement.pourEcheanceSoldeInsuffisant(7, "Livret");

        assertThat(notification.clientId()).isEqualTo(7L);
        assertThat(notification.type()).isEqualTo(TypeNotification.ALERTE);
        assertThat(notification.titre()).isEqualTo("Virement programmé non exécuté");
        assertThat(notification.message())
                .isEqualTo("Virement programmé vers Livret non exécuté : solde insuffisant, rien n'a été débité");
    }

    @Test
    void le_compte_introuvable_donne_une_alerte_titree() {
        NotificationVirement notification = NotificationVirement.pourEcheanceCompteIntrouvable(7, "Livret");

        assertThat(notification.type()).isEqualTo(TypeNotification.ALERTE);
        assertThat(notification.titre()).isEqualTo("Virement programmé refusé");
        assertThat(notification.message()).isEqualTo(
                "Virement programmé vers Livret refusé : compte destinataire introuvable, rien n'a été débité");
    }

    @Test
    void l_evenement_exige_ses_champs_et_un_titre() {
        NotificationVirement titree = NotificationVirement.pourEcheanceSoldeInsuffisant(7, "Livret");
        NotificationVirement sansTitre = new NotificationVirement(7, TypeNotification.ALERTE, null, "x");
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new EcheanceNonExecutee(null, 1, titree, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EcheanceNonExecutee(id, 1, null, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EcheanceNonExecutee(id, 1, titree, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EcheanceNonExecutee(id, 1, sansTitre, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new EcheanceNonExecutee(id, 3, titree, T).virementProgrammeId()).isEqualTo(3L);
    }
}
