package fr.nextbanking.documents.adapter;

import static fr.nextbanking.documents.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.documents.adapter.OutilsHttp.jetonValide;
import static fr.nextbanking.documents.adapter.OutilsHttp.octetsAvecJeton;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.infrastructure.ApplicationDocuments;
import fr.nextbanking.documents.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * Téléchargement du RIB et de l'attestation sur une vraie base : en-têtes, contenu relu du PDF, validations dans
 * l'ordre, compteur de téléchargements. Données de référence du service ; horloge fixe (10 octobre 2026 à Paris).
 */
@SpringBootTest(classes = {ApplicationDocuments.class, ConfigurationHorlogeFixe.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class TelechargementHttpIT {

    private static final String PIED = "Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012"
            + " — Établissement de crédit agréé par l'ACPR";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
        // Le compte 3 (COURANT de user2, client 2) vient des données de référence (import.sql du legacy).
    }

    private HttpResponse<byte[]> telecharger(String requete, String clientId) throws Exception {
        return octetsAvecJeton(port, "/api/documents/telechargement" + requete, jetonValide(clientId));
    }

    private static String texte(HttpResponse<byte[]> reponse) {
        return new String(reponse.body(), StandardCharsets.UTF_8);
    }

    private long compteur(String type, String parametres) {
        return jdbc.sql("""
                select coalesce(sum(download_count), 0) from documents
                where document_type = :type and parametres = :parametres""")
                .param("type", type).param("parametres", parametres).query(Long.class).single();
    }

    private long totalDesCompteurs() {
        return jdbc.sql("select coalesce(sum(download_count), 0) from documents").query(Long.class).single();
    }

    @Test
    @Tag("FR-017-05")
    @Tag("FR-008-08")
    @Tag("FR-017-11")
    void le_rib_du_titulaire_est_servi_avec_ses_en_tetes_et_ses_textes() throws Exception {
        HttpResponse<byte[]> reponse = telecharger("?type=RIB&compteId=1", "1");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).contains("application/pdf");
        assertThat(reponse.headers().firstValue("Content-Disposition")).contains("attachment; filename=\"rib_1.pdf\"");
        assertThat(reponse.headers().firstValue("Cache-Control")).contains("no-cache, no-store, must-revalidate");
        assertThat(reponse.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(PdfTexte.extraire(reponse.body())).contains(
                "Demo Bank — Releve d Identite Bancaire (RIB)", "Titulaire Jean Dupont", "Banque Demo Bank SA",
                "Code banque 30004", "Code guichet 00001", "Numero de compte 00010001", "Cle RIB 42",
                "IBAN FR76-0001-0001", "BIC / SWIFT DEMOFR21XXX", "Domiciliation Demo Bank - Agence Paris Centre",
                "Document genere le 10/10/2026 - Demo Bank SA, 1 Place Vendome, 75001 Paris", PIED);
    }

    @Test
    @Tag("FR-017-02")
    @Tag("FR-008-11")
    void le_type_se_lit_sans_egard_a_la_casse() throws Exception {
        for (String type : new String[] {"rib", "Rib", "rIB"}) {
            assertThat(telecharger("?type=" + type + "&compteId=1", "1").statusCode()).as(type).isEqualTo(200);
        }
        assertThat(telecharger("?type=attestation&compteId=1", "1").headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"attestation_solde_1.pdf\"");
    }

    @Test
    @Tag("FR-017-06")
    @Tag("FR-008-09")
    @Tag("FR-017-11")
    void l_attestation_porte_le_solde_de_ce_compte_seul_et_la_date_de_l_horloge() throws Exception {
        HttpResponse<byte[]> reponse = telecharger("?type=ATTESTATION&compteId=1", "1");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).contains("application/pdf");
        assertThat(reponse.headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"attestation_solde_1.pdf\"");
        assertThat(reponse.headers().firstValue("Cache-Control")).contains("no-cache, no-store, must-revalidate");
        String texte = PdfTexte.extraire(reponse.body());
        assertThat(texte).contains("Demo Bank — Attestation de Solde",
                "Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que :",
                "M. / Mme Jean Dupont", "est titulaire du compte n° FR76-0001-0001",
                "Le solde de ce compte, arrete au 10 Octobre 2026, est de :", "2450.00 EUR",
                "Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit.",
                "Fait a Paris, le 10 Octobre 2026", PIED);
        assertThat(texte).doesNotContain("15000.00", "17450.00");
    }

    @Test
    @Tag("FR-017-02")
    @Tag("FR-017-03")
    @Tag("FR-008-11")
    void les_parametres_invalides_rendent_400_avec_le_texte_du_legacy_sans_pdf_ni_compteur() throws Exception {
        long avant = totalDesCompteurs();
        String[][] cas = {
            {"", "Parametre type manquant"},
            {"?compteId=1", "Parametre type manquant"},
            {"?type=FACTURE", "Type inconnu : FACTURE"},
            {"?type=FACTURE&compteId=abc", "Type inconnu : FACTURE"},
            {"?type=RIB", "accountId manquant"},
            {"?type=RIB&compteId=", "accountId manquant"},
            {"?type=RIB&compteId=abc", "accountId manquant"},
            {"?type=ATTESTATION&compteId=1.5", "accountId manquant"},
        };

        for (String[] attendu : cas) {
            HttpResponse<byte[]> reponse = telecharger(attendu[0], "1");

            assertThat(reponse.statusCode()).as(attendu[0]).isEqualTo(400);
            assertThat(texte(reponse)).as(attendu[0]).isEqualTo("{\"message\":\"" + attendu[1] + "\"}");
            assertThat(reponse.headers().firstValue("Content-Type")).as(attendu[0])
                    .hasValueSatisfying(type -> assertThat(type).startsWith("application/json"));
        }
        assertThat(totalDesCompteurs()).isEqualTo(avant);
    }

    @Test
    @Tag("FR-017-09")
    void un_compte_inexistant_rend_404_compte_introuvable() throws Exception {
        long avant = totalDesCompteurs();

        HttpResponse<byte[]> reponse = telecharger("?type=RIB&compteId=999999", "1");

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(texte(reponse)).isEqualTo("{\"message\":\"Compte introuvable\"}");
        assertThat(totalDesCompteurs()).isEqualTo(avant);
    }

    @Test
    @Tag("FR-017-08")
    @Tag("FR-008-04")
    @Tag("BUG-058")
    @Tag("BUG-027")
    void le_compte_d_un_autre_client_rend_403_sans_pdf_ni_compteur_et_celui_du_titulaire_est_servi() throws Exception {
        long avant = totalDesCompteurs();

        for (String type : new String[] {"RIB", "ATTESTATION"}) {
            HttpResponse<byte[]> refus = telecharger("?type=" + type + "&compteId=3", "1");

            assertThat(refus.statusCode()).as(type).isEqualTo(403);
            assertThat(texte(refus)).as(type).isEqualTo("{\"message\":\"Accès refusé\"}");
            assertThat(refus.headers().firstValue("Content-Type")).as(type)
                    .hasValueSatisfying(contenu -> assertThat(contenu).startsWith("application/json"));
        }
        assertThat(totalDesCompteurs()).isEqualTo(avant);
        // Les contrôles du type et du compteId précèdent celui de l'appartenance.
        assertThat(texte(telecharger("?type=FACTURE&compteId=3", "1"))).contains("Type inconnu : FACTURE");
        assertThat(texte(telecharger("?type=RIB&compteId=abc", "1"))).contains("accountId manquant");
        assertThat(telecharger("?type=RIB&compteId=3", "2").statusCode()).isEqualTo(200);
    }

    @Test
    @Tag("FR-008-12")
    void chaque_telechargement_reussi_incremente_le_compteur_de_sa_ligne_et_un_refus_n_incremente_rien() throws Exception {
        assertThat(compteur("RIB", "compteId=1")).isZero();

        assertThat(telecharger("?type=RIB&compteId=1", "1").statusCode()).isEqualTo(200);
        assertThat(compteur("RIB", "compteId=1")).isEqualTo(1);
        assertThat(telecharger("?type=RIB&compteId=3", "1").statusCode()).isEqualTo(403);
        assertThat(telecharger("?type=RIB&compteId=999999", "1").statusCode()).isEqualTo(404);
        assertThat(telecharger("?type=RIB", "1").statusCode()).isEqualTo(400);
        assertThat(compteur("RIB", "compteId=1")).isEqualTo(1);
        assertThat(telecharger("?type=RIB&compteId=1", "1").statusCode()).isEqualTo(200);

        assertThat(compteur("RIB", "compteId=1")).isEqualTo(2);
        assertThat(jdbc.sql("select count(*) from documents where document_type = 'RIB'").query(Long.class).single())
                .isEqualTo(1L);
        assertThat(jdbc.sql("select filename || '|' || mime_type || '|' || (checksum is not null) from documents"
                + " where document_type = 'RIB'").query(String.class).single())
                .isEqualTo("rib_1.pdf|application/pdf|true");
        assertThat(compteur("ATTESTATION", "compteId=1")).isZero();
        // Les 5 lignes de référence du legacy ne sont pas touchées.
        assertThat(jdbc.sql("select sum(download_count) from documents where id <= 5").query(Long.class).single())
                .isEqualTo(7L);
    }

    @Test
    void le_releve_du_compte_d_un_autre_client_rend_403_et_les_routes_voisines_les_textes_communs() throws Exception {
        assertThat(telecharger("?type=RELEVE&compteId=3&annee=2026&mois=3", "1").statusCode()).isEqualTo(403);
        HttpResponse<String> inconnue = avecJeton(port, "GET", "/api/documents/inconnu", jetonValide("1"));
        assertThat(inconnue.statusCode()).isEqualTo(404);
        assertThat(inconnue.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
        HttpResponse<String> methode = avecJeton(port, "POST", "/api/documents/telechargement", jetonValide("1"));
        assertThat(methode.statusCode()).isEqualTo(405);
        assertThat(methode.body()).isEqualTo("{\"message\":\"Ressource introuvable\"}");
    }

    @Test
    void un_titulaire_absent_de_la_projection_des_clients_rend_404_client_introuvable() throws Exception {
        OutilsHttp.insererCompte(base, 40, 99, "FR76-0099-0001", "COURANT", "10.00");

        HttpResponse<byte[]> reponse = telecharger("?type=RIB&compteId=40", "99");

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(texte(reponse)).isEqualTo("{\"message\":\"Client introuvable\"}");
    }
}
