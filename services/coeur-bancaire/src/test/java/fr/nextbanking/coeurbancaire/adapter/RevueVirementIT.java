package fr.nextbanking.coeurbancaire.adapter;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Suspension, revue de l'administrateur et suivi du client de bout en bout, sur une vraie base : réponse de la
 * soumission (état, heure butoir), file triée, rôle vérifié par le service, libération et rejet (statuts, motifs,
 * heure butoir qui fait foi), historique, suivi, nom du client lu dans la projection.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"nb.reseau.mandataires-de-confiance=127.0.0.0/8", "spring.kafka.listener.auto-startup=false"})
@DirtiesContext
@Import(RevueVirementIT.ConfigurationHorloge.class)
@Tag("FR-016-19")
@Tag("FR-027-04")
@Tag("FR-027-16")
@Tag("FR-027-17")
@Tag("FR-023-04")
@Tag("FR-003-13")
@Tag("BUG-076")
class RevueVirementIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @TestConfiguration
    static class ConfigurationHorloge {
        @Bean
        @Primary
        HorlogeFixe horlogeFixe() {
            return new HorlogeFixe();
        }
    }

    @LocalServerPort
    private int port;
    @Autowired
    private HorlogeFixe horloge;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private DataSource source;
    @Autowired
    private RecevoirClientModifie recevoirClientModifie;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void recharger() throws Exception {
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS);
        DonneesDeReference.recharger(source, jdbc);
        jdbc.sql("update comptes set solde = 50000.00 where id = 1").update();
        // Bénéficiaire actif : seuls le montant et l'historique pilotent la suspension de ces tests.
        jdbc.sql("""
                insert into beneficiaires_externes (client_id, titulaire, iban, libelle, actif)
                values (1, 'Titulaire', 'FR76-0001-0002', 'Mon épargne', true)""").update();
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    private JsonNode suspendre(String montant) throws Exception {
        HttpResponse<String> reponse = envoyer("POST", "/api/coeur-bancaire/virements", Jetons.pour(1, "CLIENT"),
                "{\"compteSourceId\":1,\"compteDestinataireId\":2,\"montant\":\"" + montant + "\",\"motif\":\"loyer\"}");
        assertThat(reponse.statusCode()).isEqualTo(201);
        return json.readTree(reponse.body());
    }

    @Test
    void la_soumission_suspendue_rend_l_etat_et_l_heure_butoir_sans_aucun_effet_sur_les_soldes() throws Exception {
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(15 * 60));

        JsonNode corps = suspendre("10000.00");

        assertThat(corps.get("etat").asString()).isEqualTo("SUSPENDU");
        assertThat(corps.get("heureButoir").asString()).isEqualTo("2026-11-18T09:15:00Z");
        assertThat(corps.get("montant").asString()).isEqualTo("10000.00");
        assertThat(corps.get("compteDestinataireId").asLong()).isEqualTo(2);
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(String.class).single())
                .isEqualTo("50000.00");
        assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from outbox where topic = 'coeur-bancaire.virement-a-traiter.v1'")
                .query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select charge from outbox where topic = 'coeur-bancaire.virement-decide.v1'")
                .query(String.class).single()).contains("\"etat\":\"SUSPENDU\"").contains("\"type\":\"FRAUDE\"")
                .contains("avant le 18/11/2026 à 10:15");
    }

    @Test
    void la_file_est_reservee_a_l_administrateur_triee_par_heure_butoir_avec_le_nom_du_client() throws Exception {
        recevoirClientModifie.recevoir(UUID.randomUUID(), 1, "Jean Dupont", 1);
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.minusSeconds(3600));
        long second = suspendre("11000.00").get("virementId").asLong();
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS);
        long premier = suspendre("10000.00").get("virementId").asLong();

        for (String role : List.of("CLIENT", "CONSEILLER")) {
            HttpResponse<String> refuse = envoyer("GET", "/api/coeur-bancaire/revue/virements-suspendus",
                    Jetons.pour(1, role), null);
            assertThat(refuse.statusCode()).isEqualTo(403);
            assertThat(refuse.body()).isEqualTo("{\"message\":\"Accès refusé\"}");
        }
        HttpResponse<String> file = envoyer("GET", "/api/coeur-bancaire/revue/virements-suspendus",
                Jetons.pour(99, "ADMIN"), null);

        assertThat(file.statusCode()).isEqualTo(200);
        JsonNode lignes = json.readTree(file.body());
        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).get("virementId").asLong()).isEqualTo(second);
        assertThat(lignes.get(1).get("virementId").asLong()).isEqualTo(premier);
        assertThat(lignes.get(0).get("nomClient").asString()).isEqualTo("Jean Dupont");
        assertThat(lignes.get(0).get("montant").asString()).isEqualTo("11000.00");
        assertThat(lignes.get(0).get("compteDestinataire").asString()).isEqualTo("FR76-0001-0002");
        assertThat(lignes.get(0).get("motifSuspension").asString())
                .isEqualTo("Montant supérieur ou égal au seuil anti-fraude (10000 EUR)");
        assertThat(lignes.get(0).get("heureButoir").asString()).isEqualTo("2026-11-18T08:00:00Z");
    }

    @Test
    void la_liberation_passe_a_libere_remet_le_virement_et_passe_les_alertes_open_a_reviewed() throws Exception {
        long id = suspendre("10000.00").get("virementId").asLong();
        jdbc.sql("""
                insert into alertes_fraude (client_id, virement_id, regle, score, description, statut, cree_le)
                values (1, :id, 'LARGE_AMOUNT', 40, 'a', 'OPEN', timestamptz '2026-11-17 09:00:00+00'),
                       (1, :id, 'ROUND_AMOUNT', 20, 'b', 'OPEN', timestamptz '2026-11-17 09:00:00+00'),
                       (1, :id, 'FOREIGN_IBAN', 30, 'c', 'CONFIRMED', timestamptz '2026-11-17 09:00:00+00')""")
                .param("id", id).update();

        HttpResponse<String> reponse = envoyer("POST", "/api/coeur-bancaire/revue/virements/" + id + "/liberer",
                Jetons.pour(99, "ADMIN"), "{\"motif\":\"vérifié par téléphone\"}");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(json.readTree(reponse.body()).get("issue").asString()).isEqualTo("ACCEPTEE");
        assertThat(etat(id)).isEqualTo("LIBERE");
        assertThat(jdbc.sql("select statut from alertes_fraude order by id").query(String.class).list())
                .containsExactly("REVIEWED", "REVIEWED", "CONFIRMED");
        assertThat(jdbc.sql("select count(*) from outbox where topic = 'coeur-bancaire.virement-a-traiter.v1' "
                + "and cle = :cle").param("cle", Long.toString(id)).query(Long.class).single()).isEqualTo(1L);
        assertThat(jdbc.sql("select count(*) from outbox where topic = 'coeur-bancaire.virement-decide.v1'")
                .query(Long.class).single()).isEqualTo(1L);
        assertThat(jdbc.sql("select charge from outbox_audit order by id").query(String.class).list())
                .anySatisfy(charge -> assertThat(charge).contains("ADMIN_TRANSFER_RELEASE").contains("\"utilisateur\":\"99\"")
                        .contains("Liberation virement id=" + id + " - Motif: vérifié par téléphone - Issue: EXECUTE")
                        .contains("\"adresseIp\":\"127.0.0.1\""));
    }

    @Test
    void un_motif_vide_rend_400_un_role_insuffisant_403_et_le_virement_ne_change_pas() throws Exception {
        long id = suspendre("10000.00").get("virementId").asLong();
        String chemin = "/api/coeur-bancaire/revue/virements/" + id;

        HttpResponse<String> vide = envoyer("POST", chemin + "/liberer", Jetons.pour(99, "ADMIN"), "{\"motif\":\"   \"}");
        HttpResponse<String> absent = envoyer("POST", chemin + "/rejeter", Jetons.pour(99, "ADMIN"), "{}");
        HttpResponse<String> client = envoyer("POST", chemin + "/liberer", Jetons.pour(1, "CLIENT"),
                "{\"motif\":\"moi-même\"}");
        HttpResponse<String> conseiller = envoyer("POST", chemin + "/rejeter", Jetons.pour(5, "CONSEILLER"),
                "{\"motif\":\"non\"}");

        assertThat(vide.statusCode()).isEqualTo(400);
        assertThat(vide.body()).isEqualTo("{\"message\":\"Veuillez indiquer un motif\"}");
        assertThat(absent.statusCode()).isEqualTo(400);
        assertThat(client.statusCode()).isEqualTo(403);
        assertThat(conseiller.statusCode()).isEqualTo(403);
        assertThat(etat(id)).isEqualTo("SUSPENDU");
        assertThat(jdbc.sql("select count(*) from outbox_audit").query(Long.class).single()).isEqualTo(1L);
    }

    @Test
    void le_rejet_passe_a_rejete_sans_debit_et_un_virement_deja_traite_n_est_plus_trouve() throws Exception {
        long id = suspendre("10000.00").get("virementId").asLong();
        String chemin = "/api/coeur-bancaire/revue/virements/" + id + "/rejeter";

        HttpResponse<String> rejet = envoyer("POST", chemin, Jetons.pour(99, "ADMIN"), "{\"motif\":\"client injoignable\"}");
        HttpResponse<String> encore = envoyer("POST", chemin, Jetons.pour(99, "ADMIN"), "{\"motif\":\"client injoignable\"}");

        assertThat(rejet.statusCode()).isEqualTo(200);
        assertThat(encore.statusCode()).isEqualTo(404);
        assertThat(etat(id)).isEqualTo("REJETE");
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(String.class).single())
                .isEqualTo("50000.00");
    }

    @Test
    void a_l_heure_butoir_la_liberation_et_le_rejet_sont_refuses_avec_le_texte_de_l_heure_butoir() throws Exception {
        long premier = suspendre("10000.00").get("virementId").asLong();
        long second = suspendre("11000.00").get("virementId").asLong();
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(24 * 3600));

        HttpResponse<String> liberation = envoyer("POST", "/api/coeur-bancaire/revue/virements/" + premier + "/liberer",
                Jetons.pour(99, "ADMIN"), "{\"motif\":\"trop tard\"}");
        HttpResponse<String> rejet = envoyer("POST", "/api/coeur-bancaire/revue/virements/" + second + "/rejeter",
                Jetons.pour(99, "ADMIN"), "{\"motif\":\"trop tard\"}");

        for (HttpResponse<String> reponse : List.of(liberation, rejet)) {
            assertThat(reponse.statusCode()).isEqualTo(409);
            assertThat(json.readTree(reponse.body()).get("message").asString())
                    .isEqualTo("Heure butoir dépassée : le virement est rejeté.");
            assertThat(json.readTree(reponse.body()).get("issue").asString()).isEqualTo("REFUSEE_HEURE_BUTOIR");
        }
        assertThat(etat(premier)).isEqualTo("REJETE");
        assertThat(etat(second)).isEqualTo("REJETE");
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(String.class).single())
                .isEqualTo("50000.00");
    }

    @Test
    void une_liberation_sur_solde_devenu_insuffisant_rend_409_et_rejette_le_virement() throws Exception {
        long id = suspendre("10000.00").get("virementId").asLong();
        jdbc.sql("update comptes set solde = 9999.99 where id = 1").update();

        HttpResponse<String> reponse = envoyer("POST", "/api/coeur-bancaire/revue/virements/" + id + "/liberer",
                Jetons.pour(99, "ADMIN"), "{\"motif\":\"vérifié\"}");

        assertThat(reponse.statusCode()).isEqualTo(409);
        assertThat(json.readTree(reponse.body()).get("message").asString())
                .isEqualTo("Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit.");
        assertThat(etat(id)).isEqualTo("REJETE");
    }

    @Test
    void l_historique_liste_les_virements_traites_du_plus_recent_au_plus_ancien_et_le_suivi_les_quatre_premiers()
            throws Exception {
        long libere = suspendre("10000.00").get("virementId").asLong();
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(60));
        long rejete = suspendre("11000.00").get("virementId").asLong();
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(120));
        long suspendu = suspendre("12000.00").get("virementId").asLong();
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(180));
        envoyer("POST", "/api/coeur-bancaire/revue/virements/" + libere + "/liberer", Jetons.pour(99, "ADMIN"),
                "{\"motif\":\"ok\"}");
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(240));
        envoyer("POST", "/api/coeur-bancaire/revue/virements/" + rejete + "/rejeter", Jetons.pour(99, "ADMIN"),
                "{\"motif\":\"non\"}");

        JsonNode historique = json.readTree(envoyer("GET", "/api/coeur-bancaire/revue/virements-traites",
                Jetons.pour(99, "ADMIN"), null).body());
        JsonNode suivi = json.readTree(envoyer("GET", "/api/coeur-bancaire/virements/suivi",
                Jetons.pour(1, "CLIENT"), null).body());

        assertThat(historique).hasSize(2);
        assertThat(historique.get(0).get("virement").get("virementId").asLong()).isEqualTo(rejete);
        assertThat(historique.get(0).get("issue").asString()).isEqualTo("REJETE");
        assertThat(historique.get(1).get("virement").get("virementId").asLong()).isEqualTo(libere);
        assertThat(historique.get(1).get("issue").asString()).isEqualTo("LIBERE");
        assertThat(historique.get(1).get("dateIssue").asString()).isEqualTo("2026-11-17T09:03:00Z");
        assertThat(suivi).hasSize(3);
        assertThat(suivi.get(0).get("virementId").asLong()).isEqualTo(suspendu);
        assertThat(suivi.get(0).get("etat").asString()).isEqualTo("SUSPENDU");
        assertThat(suivi.get(0).get("heureButoir").asString()).isEqualTo("2026-11-18T09:02:00Z");
        assertThat(suivi.get(2).get("etat").asString()).isEqualTo("LIBERE");
        assertThat(suivi.get(0).get("compteDestinataire").asString()).isEqualTo("FR76-0001-0002");
        assertThat(envoyer("GET", "/api/coeur-bancaire/revue/virements-traites", Jetons.pour(1, "CLIENT"), null)
                .statusCode()).isEqualTo(403);
        assertThat(envoyer("GET", "/api/coeur-bancaire/virements/suivi", Jetons.pour(8, "CLIENT"), null).body())
                .isEqualTo("[]");
    }

    private String etat(long id) {
        return jdbc.sql("select etat from virements where id = :id").param("id", id).query(String.class).single();
    }

    private HttpResponse<String> envoyer(String methode, String chemin, String jeton, String corps) throws Exception {
        HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + chemin))
                .method(methode, corps == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corps))
                .header("Authorization", "Bearer " + jeton);
        if (corps != null) {
            requete.header("Content-Type", "application/json");
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(requete.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
