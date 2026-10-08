package fr.nextbanking.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.notifications.application.port.out.Notifications;
import fr.nextbanking.notifications.domain.Notification;
import fr.nextbanking.notifications.domain.NotificationDuClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Cas d'usage de lecture et de gestion des notifications, sur un port sortant en mémoire. */
class GererNotificationsCasUsageTest {

    private static final Instant T = Instant.parse("2026-11-17T09:00:00Z");

    /** Port en mémoire : l'identifiant croît à chaque ajout. */
    private static final class EnMemoire implements Notifications {
        private final List<NotificationDuClient> lignes = new ArrayList<>();

        void ajouter(long clientId, String message, boolean lue) {
            lignes.add(new NotificationDuClient(lignes.size() + 1L, clientId, "CONFIRMATION", null, message, lue, T));
        }

        @Override
        public void ajouter(Notification notification) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void conserverLesPlusRecentes(long clientId, int plafond) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<NotificationDuClient> duClient(long clientId) {
            return lignes.stream().filter(n -> n.clientId() == clientId)
                    .sorted((a, b) -> Long.compare(b.id(), a.id())).toList();
        }

        @Override
        public Optional<NotificationDuClient> parId(long id) {
            return lignes.stream().filter(n -> n.id() == id).findFirst();
        }

        @Override
        public void marquerToutesLues(long clientId) {
            lignes.replaceAll(n -> n.clientId() == clientId
                    ? new NotificationDuClient(n.id(), n.clientId(), n.type(), n.titre(), n.message(), true, n.creeLe()) : n);
        }

        @Override
        public void supprimerToutes(long clientId) {
            lignes.removeIf(n -> n.clientId() == clientId);
        }
    }

    private final EnMemoire port = new EnMemoire();

    private EnMemoire avecDeuxUtilisateurs() {
        port.ajouter(1, "ancienne de user1", false);
        port.ajouter(2, "de user2", false);
        port.ajouter(1, "recente de user1", false);
        return port;
    }

    @Test
    @Tag("FR-002-07")
    @Tag("FR-025-05")
    void la_liste_ne_contient_que_les_notifications_de_l_utilisateur_la_plus_recente_d_abord() {
        avecDeuxUtilisateurs();

        assertThat(new ListerNotificationsCasUsage(port).lister(1)).extracting(NotificationDuClient::message)
                .containsExactly("recente de user1", "ancienne de user1");
        assertThat(new ListerNotificationsCasUsage(port).lister(3)).isEmpty();
    }

    @Test
    @Tag("FR-002-07")
    void le_proprietaire_consulte_sa_notification() {
        avecDeuxUtilisateurs();

        assertThat(new ConsulterNotificationCasUsage(port).consulter(2, 2).message()).isEqualTo("de user2");
    }

    @Test
    @Tag("FR-002-07")
    void la_notification_d_autrui_est_refusee_et_une_inconnue_est_introuvable() {
        avecDeuxUtilisateurs();
        ConsulterNotificationCasUsage consulter = new ConsulterNotificationCasUsage(port);

        assertThatThrownBy(() -> consulter.consulter(1, 2)).isInstanceOf(AccesNotificationRefuse.class)
                .hasMessageContaining("2");
        assertThatThrownBy(() -> consulter.consulter(1, 99)).isInstanceOf(NotificationIntrouvable.class)
                .hasMessageContaining("99");
    }

    @Test
    @Tag("FR-002-08")
    @Tag("FR-025-06")
    void tout_marquer_lu_ne_touche_que_l_utilisateur() {
        avecDeuxUtilisateurs();

        new MarquerToutesLuesCasUsage(port).marquer(1);

        assertThat(port.duClient(1)).allMatch(NotificationDuClient::lue);
        assertThat(port.duClient(2)).noneMatch(NotificationDuClient::lue);
    }

    @Test
    @Tag("FR-002-08")
    @Tag("FR-025-07")
    void tout_effacer_ne_supprime_que_les_notifications_de_l_utilisateur() {
        avecDeuxUtilisateurs();

        new EffacerToutesLesNotificationsCasUsage(port).effacer(1);

        assertThat(port.duClient(1)).isEmpty();
        assertThat(port.duClient(2)).hasSize(1);
    }
}
