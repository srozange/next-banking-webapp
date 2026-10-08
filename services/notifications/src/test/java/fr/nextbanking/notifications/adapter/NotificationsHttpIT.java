package fr.nextbanking.notifications.adapter;

import static fr.nextbanking.notifications.adapter.OutilsHttp.avecJeton;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.notifications.infrastructure.ApplicationNotifications;
import fr.nextbanking.notifications.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
 * Lecture, marquage et effacement des notifications sur une vraie base : seules les notifications de l'utilisateur
 * du jeton sont lues ou modifiées, celles d'un autre restent intactes.
 */
@SpringBootTest(classes = ApplicationNotifications.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-002-07")
@Tag("FR-002-08")
@Tag("FR-025-05")
@Tag("FR-025-06")
@Tag("FR-025-07")
class NotificationsHttpIT {

    private static final String LISTE = "/api/notifications/notifications";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void donnees() {
        jdbc.sql("truncate table notifications restart identity").update();
        // user1 (id 1) : trois types, du plus ancien au plus récent ; user2 (id 2) : deux notifications.
        inserer(1, "CONFIRMATION", "confirmation de user1", true, "2026-11-17T09:00:00Z");
        inserer(2, "ALERTE", "alerte de user2", false, "2026-11-17T09:30:00Z");
        inserer(1, "ALERTE", "alerte de user1", false, "2026-11-17T10:00:00.123456Z");
        inserer(2, "FRAUDE", "fraude de user2", false, "2026-11-17T10:30:00Z");
        inserer(1, "FRAUDE", "fraude de user1", false, "2026-11-17T11:00:00Z");
    }

    private void inserer(long clientId, String type, String message, boolean lue, String creeLe) {
        jdbc.sql("""
                insert into notifications (virement_id, client_id, type, titre, message, lue, cree_le)
                values (10, :c, :t, 'titre non affiché', :m, :l, cast(:d as timestamptz))""")
                .param("c", clientId).param("t", type).param("m", message).param("l", lue).param("d", creeLe)
                .update();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> lister(String sujet) throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", LISTE, OutilsHttp.jetonValide(sujet));
        assertThat(reponse.statusCode()).isEqualTo(200);
        return json.readValue(reponse.body(), List.class);
    }

    private long nombreEnBase(long clientId, String condition) {
        return jdbc.sql("select count(*) from notifications where client_id = :c " + condition)
                .param("c", clientId).query(Long.class).single();
    }

    @Test
    void la_liste_ne_contient_que_les_notifications_de_l_utilisateur_la_plus_recente_d_abord() throws Exception {
        List<Map<String, Object>> liste = lister("1");

        assertThat(liste).extracting(n -> n.get("message"))
                .containsExactly("fraude de user1", "alerte de user1", "confirmation de user1");
        assertThat(liste).extracting(n -> n.get("type")).containsExactly("FRAUDE", "ALERTE", "CONFIRMATION");
        assertThat(liste).extracting(n -> n.get("lue")).containsExactly(false, false, true);
        assertThat(liste.getFirst().keySet()).containsExactlyInAnyOrder("id", "type", "message", "creeLe", "lue");
        assertThat(lister("2")).extracting(n -> n.get("message"))
                .containsExactly("fraude de user2", "alerte de user2");
        assertThat(lister("3")).isEmpty();
    }

    @Test
    void l_horodatage_est_un_instant_iso_8601_utc() throws Exception {
        List<Map<String, Object>> liste = lister("1");

        assertThat(liste).extracting(n -> n.get("creeLe"))
                .containsExactly("2026-11-17T11:00:00Z", "2026-11-17T10:00:00.123Z", "2026-11-17T09:00:00Z");
    }

    @Test
    void tout_marquer_lu_marque_les_notifications_de_l_utilisateur_et_laisse_celles_d_un_autre() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "POST", LISTE + "/lecture", OutilsHttp.jetonValide("1"));

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(nombreEnBase(1, "and not lue")).isZero();
        assertThat(nombreEnBase(1, "")).isEqualTo(3);
        assertThat(nombreEnBase(2, "and not lue")).isEqualTo(2);
    }

    @Test
    void tout_effacer_supprime_les_notifications_de_l_utilisateur_et_laisse_celles_d_un_autre() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "DELETE", LISTE, OutilsHttp.jetonValide("1"));

        assertThat(reponse.statusCode()).isEqualTo(204);
        assertThat(nombreEnBase(1, "")).isZero();
        assertThat(nombreEnBase(2, "")).isEqualTo(2);
        assertThat(lister("1")).isEmpty();
    }
}
