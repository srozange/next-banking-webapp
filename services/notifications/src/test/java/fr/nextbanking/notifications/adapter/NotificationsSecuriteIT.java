package fr.nextbanking.notifications.adapter;

import static fr.nextbanking.notifications.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.notifications.adapter.OutilsHttp.envoyer;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.notifications.infrastructure.ApplicationNotifications;
import fr.nextbanking.notifications.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.security.KeyPairGenerator;
import java.time.Instant;
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

/**
 * Contrôle d'accès du service notifications : tout est authentifié (tout rôle) sauf {@code GET /actuator/health} ;
 * refus en JSON {@code {"message"}} : 401 sans jeton valide, 403 pour la notification d'un autre utilisateur, 404
 * pour une notification inexistante.
 */
@SpringBootTest(classes = ApplicationNotifications.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-002-07")
class NotificationsSecuriteIT {

    private static final String AUTHENTIFICATION_REQUISE = "{\"message\":\"Authentification requise\"}";
    private static final String ACCES_REFUSE = "{\"message\":\"Accès refusé\"}";
    private static final String RESSOURCE_INTROUVABLE = "{\"message\":\"Ressource introuvable\"}";
    private static final String LISTE = "/api/notifications/notifications";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Ces tests ne reçoivent aucun événement : l'écouteur Kafka ne démarre pas.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    private long notificationDeUser1;
    private long notificationDeUser2;

    @BeforeEach
    void donnees() {
        jdbc.sql("truncate table notifications restart identity").update();
        notificationDeUser1 = inserer(1);
        notificationDeUser2 = inserer(2);
    }

    private long inserer(long clientId) {
        return jdbc.sql("""
                insert into notifications (virement_id, client_id, type, message, lue, cree_le)
                values (10, :c, 'CONFIRMATION', 'message', false, now()) returning id""")
                .param("c", clientId).query(Long.class).single();
    }

    @Test
    void sans_jeton_les_points_et_les_chemins_voisins_rendent_401() throws Exception {
        assertRefus(envoyer(port, "GET", LISTE, null, Map.of()));
        assertRefus(envoyer(port, "GET", LISTE + "/" + notificationDeUser1, null, Map.of()));
        assertRefus(envoyer(port, "POST", LISTE + "/lecture", null, Map.of()));
        assertRefus(envoyer(port, "DELETE", LISTE, null, Map.of()));
        assertRefus(envoyer(port, "GET", "/api/notifications/inconnu", null, Map.of()));
        assertRefus(envoyer(port, "GET", "/actuator/info", null, Map.of()));
    }

    @Test
    void un_jeton_expire_mal_signe_ou_illisible_rend_401() throws Exception {
        Instant maintenant = Instant.now();
        String expire = OutilsHttp.jeton(Conteneurs.CLES, "1", "CLIENT", maintenant.minusSeconds(7200),
                maintenant.minusSeconds(3600));
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        String malSigne = OutilsHttp.jeton(generateur.generateKeyPair(), "1", "CLIENT", maintenant,
                maintenant.plusSeconds(1800));

        for (String jeton : new String[] {expire, malSigne, "illisible", OutilsHttp.jetonValide("1") + "x"}) {
            assertRefus(avecJeton(port, "GET", LISTE, jeton));
        }
    }

    @Test
    void un_sujet_qui_n_est_pas_un_identifiant_numerique_rend_401() throws Exception {
        String jeton = OutilsHttp.jetonValide("user1");

        assertRefus(avecJeton(port, "GET", LISTE, jeton));
        assertRefus(avecJeton(port, "GET", LISTE + "/" + notificationDeUser1, jeton));
        assertRefus(avecJeton(port, "POST", LISTE + "/lecture", jeton));
        assertRefus(avecJeton(port, "DELETE", LISTE, jeton));
    }

    @Test
    void la_sante_est_publique() throws Exception {
        HttpResponse<String> reponse = envoyer(port, "GET", "/actuator/health", null, Map.of());

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void tout_role_authentifie_accede_a_la_liste() throws Exception {
        for (String role : new String[] {"CLIENT", "CONSEILLER", "ADMIN"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET", LISTE, OutilsHttp.jetonValide("1", role));

            assertThat(reponse.statusCode()).as(role).isEqualTo(200);
            assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        }
    }

    @Test
    void la_notification_d_un_autre_utilisateur_rend_403_et_une_notification_inexistante_404() throws Exception {
        HttpResponse<String> autrui = avecJeton(port, "GET", LISTE + "/" + notificationDeUser2,
                OutilsHttp.jetonValide("1"));
        assertThat(autrui.statusCode()).isEqualTo(403);
        assertThat(autrui.body()).isEqualTo(ACCES_REFUSE);
        assertThat(autrui.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));

        HttpResponse<String> inconnue = avecJeton(port, "GET", LISTE + "/999999", OutilsHttp.jetonValide("1"));
        assertThat(inconnue.statusCode()).isEqualTo(404);
        assertThat(inconnue.body()).isEqualTo(RESSOURCE_INTROUVABLE);
    }

    @Test
    void le_proprietaire_consulte_sa_notification() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", LISTE + "/" + notificationDeUser1,
                OutilsHttp.jetonValide("1"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).contains("\"id\":" + notificationDeUser1, "\"lue\":false");
    }

    @Test
    void avec_jeton_une_route_inconnue_rend_404_une_methode_non_prevue_405_et_un_identifiant_illisible_400()
            throws Exception {
        String jeton = OutilsHttp.jetonValide("1");

        HttpResponse<String> inconnue = avecJeton(port, "GET", "/api/notifications/inconnu", jeton);
        assertThat(inconnue.statusCode()).isEqualTo(404);
        assertThat(inconnue.body()).isEqualTo(RESSOURCE_INTROUVABLE);

        HttpResponse<String> methode = avecJeton(port, "PUT", LISTE, jeton);
        assertThat(methode.statusCode()).isEqualTo(405);
        assertThat(methode.body()).isEqualTo(RESSOURCE_INTROUVABLE);

        HttpResponse<String> illisible = avecJeton(port, "GET", LISTE + "/abc", jeton);
        assertThat(illisible.statusCode()).isEqualTo(400);
        assertThat(illisible.body()).isEqualTo("{\"message\":\"Requête invalide\"}");
    }

    private static void assertRefus(HttpResponse<String> reponse) {
        assertThat(reponse.statusCode()).as(reponse.uri().toString()).isEqualTo(401);
        assertThat(reponse.body()).isEqualTo(AUTHENTIFICATION_REQUISE);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
    }
}
