package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.identiteclients.adapter.OutilsHttp.jetonValide;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Administration de bout en bout sur les données de référence : indicateurs, gestion des clients (rôle CLIENT seul,
 * conseiller affecté, date de création de la fiche), tous les utilisateurs ; supprimés exclus, paramètres ignorés.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class AdministrationIT {

    private static final String INDICATEURS = "/api/identite-clients/administration/indicateurs";
    private static final String CLIENTS = "/api/identite-clients/administration/clients";
    private static final String UTILISATEURS = "/api/identite-clients/administration/utilisateurs";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    private final String admin = jetonValide("10", "ADMIN");

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
        // Dates de création distinctes des dates d'affectation (advisor_since) : BUG-055.
        OutilsHttp.executer(base, "update customers set date_creation = date '2024-02-01' where id = 1;"
                + "update customers set date_creation = date '2023-11-20' where id <> 1;"
                + "update customers set advisor_since = date '2025-06-15' where id = 1;");
    }

    @Test
    @Tag("FR-016-02")
    void les_indicateurs_de_reference_comptent_4_clients_dont_3_actifs_2_conseillers_et_7_utilisateurs()
            throws Exception {
        OutilsHttp.executer(base, "update customers set active = false where id = 4;");

        HttpResponse<String> reponse = avecJeton(port, "GET", INDICATEURS, admin);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body())
                .isEqualTo("{\"totalClients\":4,\"clientsActifs\":3,\"conseillers\":2,\"totalUtilisateurs\":7}");
    }

    @Test
    @Tag("FR-016-02")
    void un_utilisateur_supprime_sort_des_indicateurs() throws Exception {
        OutilsHttp.executer(base, "update customers set supprime = true where id = 3;");

        assertThat(avecJeton(port, "GET", INDICATEURS, admin).body())
                .isEqualTo("{\"totalClients\":3,\"clientsActifs\":3,\"conseillers\":2,\"totalUtilisateurs\":6}");
    }

    @Test
    @Tag("FR-016-08")
    @Tag("FR-016-09")
    @Tag("FR-016-10")
    @Tag("BUG-055")
    void la_gestion_des_clients_rend_les_4_clients_seulement_avec_conseiller_et_date_de_creation() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", CLIENTS + "?role=ADMIN&clientId=10", admin);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("["
                + "{\"clientId\":1,\"nomComplet\":\"Jean Dupont\",\"login\":\"user1\","
                + "\"email\":\"jean.dupont@example.com\",\"telephone\":\"+33601020304\",\"role\":\"CLIENT\","
                + "\"actif\":true,\"nomConseiller\":\"Sophie Durand\",\"dateCreation\":\"2024-02-01\"},"
                + "{\"clientId\":4,\"nomComplet\":\"Lucie Martin\",\"login\":\"user4\","
                + "\"email\":\"lucie.martin@example.com\",\"telephone\":\"+33601020307\",\"role\":\"CLIENT\","
                + "\"actif\":true,\"nomConseiller\":\"Marc Lefevre\",\"dateCreation\":\"2023-11-20\"},"
                + "{\"clientId\":2,\"nomComplet\":\"Marie Lebrun\",\"login\":\"user2\","
                + "\"email\":\"marie.lebrun@example.com\",\"telephone\":\"+33601020305\",\"role\":\"CLIENT\","
                + "\"actif\":true,\"nomConseiller\":\"Sophie Durand\",\"dateCreation\":\"2023-11-20\"},"
                + "{\"clientId\":3,\"nomComplet\":\"Pierre Bernard\",\"login\":\"user3\","
                + "\"email\":\"pierre.bernard@example.com\",\"telephone\":\"+33601020306\",\"role\":\"CLIENT\","
                + "\"actif\":true,\"nomConseiller\":\"Marc Lefevre\",\"dateCreation\":\"2023-11-20\"}]");
    }

    @Test
    @Tag("FR-016-09")
    @Tag("FR-016-10")
    @Tag("BUG-055")
    void un_client_sans_conseiller_ni_telephone_a_des_champs_nuls_et_garde_sa_date_de_creation() throws Exception {
        OutilsHttp.executer(base, "update customers set advisor_id = null, phone = null, active = false"
                + " where id = 1;");

        String corps = avecJeton(port, "GET", CLIENTS, admin).body();

        assertThat(corps).startsWith("[{\"clientId\":1,\"nomComplet\":\"Jean Dupont\",\"login\":\"user1\","
                + "\"email\":\"jean.dupont@example.com\",\"telephone\":null,\"role\":\"CLIENT\",\"actif\":false,"
                + "\"nomConseiller\":null,\"dateCreation\":\"2024-02-01\"},");
        assertThat(corps).doesNotContain("2025-06-15");
    }

    @Test
    @Tag("FR-016-08")
    @Tag("FR-016-13")
    void un_utilisateur_supprime_sort_des_deux_listes() throws Exception {
        OutilsHttp.executer(base, "update customers set supprime = true where id in (2, 12);");

        assertThat(avecJeton(port, "GET", CLIENTS, admin).body()).doesNotContain("\"clientId\":2,");
        assertThat(avecJeton(port, "GET", UTILISATEURS, admin).body())
                .doesNotContain("\"utilisateurId\":2,", "\"utilisateurId\":12,");
    }

    @Test
    @Tag("FR-016-13")
    void tous_les_utilisateurs_sont_listes_tous_roles_tries_par_nom_complet() throws Exception {
        OutilsHttp.executer(base, "update customers set active = false where id = 12;");

        HttpResponse<String> reponse = avecJeton(port, "GET", UTILISATEURS + "?role=CLIENT", admin);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("["
                + "{\"utilisateurId\":10,\"nomComplet\":\"Alice Martin\",\"login\":\"admin\","
                + "\"email\":\"alice.martin@demobank.fr\",\"role\":\"ADMIN\",\"actif\":true},"
                + "{\"utilisateurId\":1,\"nomComplet\":\"Jean Dupont\",\"login\":\"user1\","
                + "\"email\":\"jean.dupont@example.com\",\"role\":\"CLIENT\",\"actif\":true},"
                + "{\"utilisateurId\":4,\"nomComplet\":\"Lucie Martin\",\"login\":\"user4\","
                + "\"email\":\"lucie.martin@example.com\",\"role\":\"CLIENT\",\"actif\":true},"
                + "{\"utilisateurId\":12,\"nomComplet\":\"Marc Lefevre\",\"login\":\"marc\","
                + "\"email\":\"marc.lefevre@demobank.fr\",\"role\":\"CONSEILLER\",\"actif\":false},"
                + "{\"utilisateurId\":2,\"nomComplet\":\"Marie Lebrun\",\"login\":\"user2\","
                + "\"email\":\"marie.lebrun@example.com\",\"role\":\"CLIENT\",\"actif\":true},"
                + "{\"utilisateurId\":3,\"nomComplet\":\"Pierre Bernard\",\"login\":\"user3\","
                + "\"email\":\"pierre.bernard@example.com\",\"role\":\"CLIENT\",\"actif\":true},"
                + "{\"utilisateurId\":11,\"nomComplet\":\"Sophie Durand\",\"login\":\"sophie\","
                + "\"email\":\"sophie.durand@demobank.fr\",\"role\":\"CONSEILLER\",\"actif\":true}]");
    }
}
