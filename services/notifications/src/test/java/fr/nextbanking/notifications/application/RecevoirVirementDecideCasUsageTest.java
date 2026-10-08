package fr.nextbanking.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.notifications.application.port.in.RecevoirVirementDecide.Notification;
import fr.nextbanking.notifications.application.port.out.Horloge;
import fr.nextbanking.notifications.application.port.out.Transaction;
import fr.nextbanking.notifications.domain.PlafondNotifications;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-002-09")
@Tag("FR-023-05")
@Tag("FR-023-06")
@Tag("FR-025-04")
@Tag("BUG-006")
@Tag("BUG-007")
class RecevoirVirementDecideCasUsageTest {

    private static final Instant T = Instant.parse("2026-11-17T09:00:00Z");

    private final List<String> journal = new ArrayList<>();
    private final List<fr.nextbanking.notifications.domain.Notification> stockees = new ArrayList<>();
    private final Set<UUID> traites = new HashSet<>();

    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            journal.add("debut");
            T resultat = travail.get();
            journal.add("validation");
            return resultat;
        }

        @Override
        public <T> T executerIndependamment(Supplier<T> travail) {
            throw new UnsupportedOperationException();
        }
    };

    private final Horloge horloge = new Horloge() {
        @Override
        public Instant instant() {
            return T;
        }

        @Override
        public LocalDate aujourdhui() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ZoneId fuseau() {
            throw new UnsupportedOperationException();
        }
    };

    private final RecevoirVirementDecideCasUsage casUsage = new RecevoirVirementDecideCasUsage(
            new fr.nextbanking.notifications.application.port.out.Notifications() {
                @Override
                public void ajouter(fr.nextbanking.notifications.domain.Notification notification) {
                    journal.add("ajouter");
                    stockees.add(notification);
                }

                @Override
                public void conserverLesPlusRecentes(long clientId, int plafond) {
                    journal.add("plafonner " + clientId + " " + plafond);
                }

                @Override
                public List<fr.nextbanking.notifications.domain.NotificationDuClient> duClient(long clientId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public java.util.Optional<fr.nextbanking.notifications.domain.NotificationDuClient> parId(long id) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void marquerToutesLues(long clientId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void supprimerToutes(long clientId) {
                    throw new UnsupportedOperationException();
                }
            },
            idEvenement -> {
                journal.add("marquer");
                return traites.add(idEvenement);
            },
            transaction, horloge, new PlafondNotifications(20));

    private static Notification rendue() {
        return new Notification(7, "CONFIRMATION", null, "Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme");
    }

    @Test
    void stocke_la_notification_telle_que_rendue_non_lue_dans_la_transaction_puis_applique_le_plafond() {
        assertThat(casUsage.recevoir(UUID.randomUUID(), 12, rendue())).isTrue();

        assertThat(stockees).singleElement().satisfies(notification -> {
            assertThat(notification.virementId()).isEqualTo(12);
            assertThat(notification.clientId()).isEqualTo(7);
            assertThat(notification.type()).isEqualTo("CONFIRMATION");
            assertThat(notification.titre()).isNull();
            assertThat(notification.message())
                    .isEqualTo("Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme");
            assertThat(notification.lue()).isFalse();
            assertThat(notification.creeLe()).isEqualTo(T);
        });
        assertThat(journal).containsExactly("debut", "marquer", "ajouter", "plafonner 7 20", "validation");
    }

    @Test
    void un_evenement_deja_traite_ne_cree_pas_de_seconde_notification() {
        UUID id = UUID.randomUUID();
        casUsage.recevoir(id, 12, rendue());

        assertThat(casUsage.recevoir(id, 12, rendue())).isFalse();
        assertThat(stockees).hasSize(1);
    }

    @Test
    void refuse_un_evenement_sans_identifiant_ou_sans_notification() {
        assertThatThrownBy(() -> casUsage.recevoir(null, 12, rendue())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> casUsage.recevoir(UUID.randomUUID(), 12, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(journal).isEmpty();
    }
}
