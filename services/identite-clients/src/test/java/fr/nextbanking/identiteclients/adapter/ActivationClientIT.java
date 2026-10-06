package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.MDP_CLIENT;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.connexion;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.publier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.application.ChangerEtatClientCasUsage;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.Role;
import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.sql.DataSource;
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
 * Désactivation et activation d'un client par un administrateur, de bout en bout (base, outbox d'audit) : rôles,
 * client inconnu, audit avec l'adresse IP, audit conservé quand l'écriture est annulée, effet sur la connexion.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-030-07")
@Tag("FR-001-07")
@Tag("FR-016-01")
@Tag("FR-016-11")
@Tag("FR-026-02")
@Tag("FR-026-04")
@Tag("FR-026-05")
@Tag("BUG-093")
@Tag("BUG-001")
class ActivationClientIT {

    private static final String USER1 = "{\"identifiant\":\"user1\",\"motDePasse\":\"" + MDP_CLIENT + "\"}";
    private static final String DESACTIVATION = "/api/identite-clients/clients/1/desactivation";
    private static final String ACTIVATION = "/api/identite-clients/clients/1/activation";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("nb.reseau.mandataires-de-confiance", () -> "127.0.0.1/32,::1/128");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DepotClients depot;

    @Autowired
    private JournalAudit journal;

    @Autowired
    private Transaction transaction;

    @Autowired
    private Horloge horloge;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    private HttpResponse<String> appeler(String chemin, String role) throws Exception {
        return publier(port, chemin, OutilsHttp.jetonValide("10", role), Map.of("X-Forwarded-For", "203.0.113.7"));
    }

    private boolean actif(long id) {
        return jdbc.sql("select active from customers where id = :id").param("id", id).query(Boolean.class).single();
    }

    @Test
    void l_administrateur_desactive_puis_reactive_user1_avec_audit_et_adresse_ip() throws Exception {
        long depuis = LignesAudit.dernierId(jdbc);

        HttpResponse<String> desactivation = appeler(DESACTIVATION, "ADMIN");
        assertThat(desactivation.statusCode()).isEqualTo(204);
        assertThat(desactivation.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(actif(1L)).isFalse();

        HttpResponse<String> activation = appeler(ACTIVATION, "ADMIN");
        assertThat(activation.statusCode()).isEqualTo(204);
        assertThat(actif(1L)).isTrue();

        List<LignesAudit.Ligne> lignes = LignesAudit.depuis(jdbc, depuis);
        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).cle()).isEqualTo("10");
        assertThat(lignes.get(0).donnees().get("action").asString()).isEqualTo("ADMIN_DEACTIVATE");
        assertThat(lignes.get(0).donnees().get("detail").asString()).isEqualTo("Désactivation client id=1");
        assertThat(lignes.get(0).donnees().get("utilisateur").asString()).isEqualTo("10");
        assertThat(lignes.get(0).donnees().get("adresseIp").asString()).isEqualTo("203.0.113.7");
        assertThat(lignes.get(1).donnees().get("action").asString()).isEqualTo("ADMIN_ACTIVATE");
        assertThat(lignes.get(1).donnees().get("detail").asString()).isEqualTo("Activation client id=1");
        assertThat(lignes.get(1).donnees().get("adresseIp").asString()).isEqualTo("203.0.113.7");
    }

    @Test
    void un_role_insuffisant_rend_403_sans_modifier_l_etat_ni_ecrire_d_audit() throws Exception {
        long depuis = LignesAudit.dernierId(jdbc);

        for (String role : new String[] {"CLIENT", "CONSEILLER"}) {
            HttpResponse<String> reponse = appeler(DESACTIVATION, role);
            assertThat(reponse.statusCode()).as(role).isEqualTo(403);
            assertThat(reponse.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
        }

        assertThat(actif(1L)).isTrue();
        assertThat(LignesAudit.depuis(jdbc, depuis)).isEmpty();
    }

    @Test
    void un_client_inexistant_rend_404_sans_audit() throws Exception {
        long depuis = LignesAudit.dernierId(jdbc);

        HttpResponse<String> reponse = appeler("/api/identite-clients/clients/9999/desactivation", "ADMIN");

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
        assertThat(LignesAudit.depuis(jdbc, depuis)).isEmpty();
    }

    @Test
    void un_client_desactive_ne_se_connecte_plus_puis_se_reconnecte_apres_reactivation() throws Exception {
        assertThat(connexion(port, USER1).statusCode()).isEqualTo(200);

        appeler(DESACTIVATION, "ADMIN");
        HttpResponse<String> refus = connexion(port, USER1);
        assertThat(refus.statusCode()).isEqualTo(401);
        assertThat(refus.body()).isEqualTo("{\"message\":\"Identifiants incorrects\"}");

        appeler(ACTIVATION, "ADMIN");
        assertThat(connexion(port, USER1).statusCode()).isEqualTo(200);
    }

    @Test
    void une_desactivation_annulee_garde_son_entree_d_audit() {
        long depuis = LignesAudit.dernierId(jdbc);
        Transaction quiAnnule = new Transaction() {
            @Override
            public <T> T executer(Supplier<T> travail) {
                return transaction.executer(() -> {
                    travail.get();
                    throw new IllegalStateException("annulation voulue");
                });
            }

            @Override
            public <T> T executerIndependamment(Supplier<T> travail) {
                return transaction.executerIndependamment(travail);
            }
        };
        ChangerEtatClientCasUsage casUsage = new ChangerEtatClientCasUsage(depot, journal, quiAnnule, horloge);

        assertThatThrownBy(() -> casUsage.desactiver(10L, Set.of(Role.ADMIN), 1L, "203.0.113.7"))
                .hasMessage("annulation voulue");

        assertThat(actif(1L)).isTrue();
        assertThat(LignesAudit.depuis(jdbc, depuis)).singleElement().satisfies(ligne -> {
            assertThat(ligne.donnees().get("action").asString()).isEqualTo("ADMIN_DEACTIVATE");
            assertThat(ligne.donnees().get("adresseIp").asString()).isEqualTo("203.0.113.7");
        });
    }
}
