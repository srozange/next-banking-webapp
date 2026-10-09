package fr.nextbanking.documents.adapter;

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
 * Téléchargement du relevé mensuel et de l'IFU sur une vraie base : mois et année découpés en Europe/Paris, PDF relus
 * par PDFBox, validations avant tout 404/403, client du jeton seul pour l'IFU, compteur de téléchargements.
 * Horloge fixe (10 octobre 2026 à Paris).
 */
@SpringBootTest(classes = {ApplicationDocuments.class, ConfigurationHorlogeFixe.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class ReleveIfuHttpIT {

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
        OutilsHttp.insererCompte(base, 3, 2, "FR76-0002-0001", "COURANT", "500.00");
        // Mars 2026 (heure de Paris, UTC+1 jusqu'au 29 mars puis UTC+2), avec les bords de mois.
        OutilsHttp.insererOperation(base, 10, 1, "2026-03-15T10:00:00Z", "Loyer mars", "DEBIT", "100.00", "2030.00");
        OutilsHttp.insererOperation(base, 11, 1, "2026-03-01T09:00:00Z", "Salaire mars", "CREDIT", "50.00", "2180.00");
        OutilsHttp.insererOperation(base, 12, 1, "2026-03-31T21:59:59Z", "Dernier instant de mars", "DEBIT", "1.00",
                "2029.00");
        OutilsHttp.insererOperation(base, 13, 1, "2026-03-31T22:30:00Z", "Bord de mois avril", "DEBIT", "2.00",
                "2027.00");
        OutilsHttp.insererOperation(base, 14, 1, "2026-04-10T10:00:00Z", "Operation avril", "CREDIT", "3.00",
                "2030.00");
        OutilsHttp.insererOperation(base, 15, 1, "2026-02-28T22:59:59Z", "Dernier instant de fevrier", "CREDIT",
                "4.00", "2000.00");
        OutilsHttp.insererOperation(base, 16, 3, "2026-03-10T10:00:00Z", "Operation d autrui", "CREDIT", "9.00",
                "509.00");
        // IFU 2025 de user1 (comptes 1 et 2) : intérêts en 2025 uniquement, crédits seulement.
        OutilsHttp.insererOperation(base, 20, 1, "2025-06-30T10:00:00Z", "Versement interet T2", "CREDIT", "10.00",
                "10.00");
        OutilsHttp.insererOperation(base, 21, 2, "2025-12-31T22:30:00Z", "Versement interet annuel", "CREDIT", "2.35",
                "12.35");
        OutilsHttp.insererOperation(base, 22, 1, "2025-07-01T10:00:00Z", "Frais interet de dossier", "DEBIT", "50.00",
                "0.00");
        OutilsHttp.insererOperation(base, 23, 1, "2025-08-01T10:00:00Z", "Salaire", "CREDIT", "1000.00", "1000.00");
        OutilsHttp.insererOperation(base, 24, 1, "2024-12-31T22:59:59Z", "Versement interet 2024", "CREDIT", "7.00",
                "7.00");
        OutilsHttp.insererOperation(base, 25, 1, "2026-01-01T00:00:00Z", "Versement interet 2026", "CREDIT", "8.00",
                "8.00");
        OutilsHttp.insererOperation(base, 26, 3, "2025-06-30T10:00:00Z", "Versement interet d autrui", "CREDIT",
                "100.00", "100.00");
    }

    private HttpResponse<byte[]> telecharger(String requete, String clientId) throws Exception {
        return octetsAvecJeton(port, "/api/documents/telechargement" + requete, jetonValide(clientId));
    }

    private static String texte(HttpResponse<byte[]> reponse) {
        return new String(reponse.body(), StandardCharsets.UTF_8);
    }

    private long totalDesCompteurs() {
        return jdbc.sql("select coalesce(sum(download_count), 0) from documents").query(Long.class).single();
    }

    private long compteur(String type, String parametres) {
        return jdbc.sql("""
                select coalesce(sum(download_count), 0) from documents
                where document_type = :type and parametres = :parametres""")
                .param("type", type).param("parametres", parametres).query(Long.class).single();
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-07")
    @Tag("FR-017-11")
    void le_releve_de_mars_ne_liste_que_les_operations_de_mars_en_europe_paris_par_date_croissante() throws Exception {
        HttpResponse<byte[]> reponse = telecharger("?type=RELEVE&compteId=1&annee=2026&mois=3", "1");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).contains("application/pdf");
        assertThat(reponse.headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"releve_1_2026_03.pdf\"");
        assertThat(reponse.headers().firstValue("Cache-Control")).contains("no-cache, no-store, must-revalidate");
        String pdf = PdfTexte.extraire(reponse.body());
        assertThat(pdf).contains("Demo Bank — Releve de compte", "Compte FR76-0001-0001 (COURANT)",
                "Titulaire Jean Dupont", "Periode Mars 2026", "Solde actuel 2130.00 EUR",
                "Date Description Type Montant Solde apres",
                "01/03/2026 Salaire mars CREDIT +50.00 EUR 2180.00 EUR",
                "15/03/2026 Loyer mars DEBIT -100.00 EUR 2030.00 EUR",
                "31/03/2026 Dernier instant de mars DEBIT -1.00 EUR 2029.00 EUR");
        assertThat(pdf.indexOf("Salaire mars")).isLessThan(pdf.indexOf("Loyer mars"));
        assertThat(pdf.indexOf("Loyer mars")).isLessThan(pdf.indexOf("Dernier instant de mars"));
        // 22h30 UTC le 31 mars = 00h30 le 1er avril à Paris : avril ; 22h59 UTC le 28 février = 23h59 : février.
        assertThat(pdf).doesNotContain("Bord de mois avril", "Operation avril", "Dernier instant de fevrier",
                "Operation d autrui", "Aucune operation");
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-07")
    void l_operation_du_bord_de_mois_est_listee_en_avril_a_la_date_de_paris() throws Exception {
        String avril = PdfTexte.extraire(telecharger("?type=RELEVE&compteId=1&annee=2026&mois=4", "1").body());

        assertThat(avril).contains("Periode Avril 2026", "01/04/2026 Bord de mois avril DEBIT -2.00 EUR 2027.00 EUR",
                "10/04/2026 Operation avril CREDIT +3.00 EUR 2030.00 EUR").doesNotContain("Dernier instant de mars");
        String fevrier = PdfTexte.extraire(telecharger("?type=RELEVE&compteId=1&annee=2026&mois=2", "1").body());
        assertThat(fevrier).contains("Periode Février 2026", "28/02/2026 Dernier instant de fevrier CREDIT +4.00 EUR");
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-07")
    void un_mois_sans_operation_affiche_le_message_du_legacy() throws Exception {
        HttpResponse<byte[]> reponse = telecharger("?type=RELEVE&compteId=1&annee=2026&mois=7", "1");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(PdfTexte.extraire(reponse.body())).contains("Periode Juillet 2026",
                "Aucune operation sur cette periode.");
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-12")
    void le_compteur_du_releve_est_incremente_sous_ses_parametres_et_jamais_sur_un_refus() throws Exception {
        long avant = totalDesCompteurs();

        assertThat(telecharger("?type=RELEVE&compteId=1&annee=2026&mois=3", "1").statusCode()).isEqualTo(200);
        assertThat(telecharger("?type=RELEVE&compteId=1&annee=2026&mois=3", "1").statusCode()).isEqualTo(200);
        assertThat(telecharger("?type=RELEVE&compteId=1&annee=2026&mois=4", "1").statusCode()).isEqualTo(200);

        assertThat(compteur("RELEVE", "compteId=1;annee=2026;mois=3")).isEqualTo(2);
        assertThat(compteur("RELEVE", "compteId=1;annee=2026;mois=4")).isEqualTo(1);
        assertThat(totalDesCompteurs()).isEqualTo(avant + 3);
        assertThat(telecharger("?type=RELEVE&compteId=3&annee=2026&mois=3", "1").statusCode()).isEqualTo(403);
        assertThat(telecharger("?type=RELEVE&compteId=1&annee=2026&mois=13", "1").statusCode()).isEqualTo(400);
        assertThat(totalDesCompteurs()).isEqualTo(avant + 3);
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-04")
    void le_releve_d_un_compte_d_autrui_rend_403_pour_tout_role_et_un_compte_inexistant_404() throws Exception {
        long avant = totalDesCompteurs();

        for (String role : new String[] {"CLIENT", "CONSEILLER", "ADMIN"}) {
            HttpResponse<byte[]> refus = octetsAvecJeton(port,
                    "/api/documents/telechargement?type=RELEVE&compteId=3&annee=2026&mois=3", jetonValide("1", role));
            assertThat(refus.statusCode()).as(role).isEqualTo(403);
            assertThat(texte(refus)).as(role).isEqualTo("{\"message\":\"Accès refusé\"}");
        }
        HttpResponse<byte[]> introuvable = telecharger("?type=RELEVE&compteId=999999&annee=2026&mois=3", "1");
        assertThat(introuvable.statusCode()).isEqualTo(404);
        assertThat(texte(introuvable)).isEqualTo("{\"message\":\"Compte introuvable\"}");
        assertThat(totalDesCompteurs()).isEqualTo(avant);
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-017-11")
    void annee_et_mois_invalides_rendent_400_avant_le_404_et_le_403_sans_pdf_ni_compteur() throws Exception {
        long avant = totalDesCompteurs();
        String[] requetes = {
            "?type=RELEVE&compteId=1", "?type=RELEVE&compteId=1&annee=2026", "?type=RELEVE&compteId=1&mois=3",
            "?type=RELEVE&compteId=1&annee=&mois=3", "?type=RELEVE&compteId=1&annee=2026&mois=",
            "?type=RELEVE&compteId=1&annee=abc&mois=3", "?type=RELEVE&compteId=1&annee=2026&mois=abc",
            "?type=RELEVE&compteId=1&annee=2026&mois=13", "?type=RELEVE&compteId=1&annee=2026&mois=0",
            "?type=RELEVE&compteId=1&annee=1999&mois=3", "?type=RELEVE&compteId=1&annee=2101&mois=3",
            // Avant le 404 (compte inexistant) et le 403 (compte d'autrui).
            "?type=RELEVE&compteId=999999&annee=1999&mois=3", "?type=RELEVE&compteId=3&annee=2026&mois=13",
            "?type=IFU", "?type=IFU&annee=", "?type=IFU&annee=abc", "?type=IFU&annee=1999", "?type=IFU&annee=2101",
        };

        for (String requete : requetes) {
            HttpResponse<byte[]> reponse = telecharger(requete, "1");

            assertThat(reponse.statusCode()).as(requete).isEqualTo(400);
            assertThat(texte(reponse)).as(requete).isEqualTo("{\"message\":\"Requête invalide\"}");
        }
        // Le compteId est contrôlé avant l'année.
        assertThat(texte(telecharger("?type=RELEVE&compteId=abc&annee=1999&mois=13", "1")))
                .isEqualTo("{\"message\":\"accountId manquant\"}");
        assertThat(totalDesCompteurs()).isEqualTo(avant);
    }

    @Test
    @Tag("FR-017-04")
    @Tag("FR-008-10")
    @Tag("FR-017-11")
    void l_ifu_totalise_les_seuls_credits_interet_de_l_annee_civile_de_tous_les_comptes_du_client() throws Exception {
        HttpResponse<byte[]> reponse = telecharger("?type=IFU&annee=2025", "1");

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).contains("application/pdf");
        assertThat(reponse.headers().firstValue("Content-Disposition"))
                .contains("attachment; filename=\"ifu_1_2025.pdf\"");
        assertThat(reponse.headers().firstValue("Cache-Control")).contains("no-cache, no-store, must-revalidate");
        // 10.00 + 2.35 = 12.35 (le 31/12/2025 à 23h30 Paris est dans 2025) ; PFU 3.705 arrondi à 3.71.
        assertThat(PdfTexte.extraire(reponse.body())).contains("Demo Bank — Imprime Fiscal Unique (IFU) 2025",
                "Etablissement Demo Bank SA", "SIREN 123 456 789 00012", "Titulaire Jean Dupont",
                "Adresse 1 rue de la Paix, 75001 Paris", "Annee fiscale 2025",
                "Interets et produits de placement 12.35 EUR", "Prelevement Forfaitaire Unique (30%) 3.71 EUR",
                "Genere le 10/10/2026");
        assertThat(compteur("IFU", "annee=2025")).isEqualTo(1);
    }

    @Test
    @Tag("FR-017-04")
    @Tag("FR-008-10")
    void le_premier_janvier_a_minuit_heure_de_paris_ouvre_l_annee_suivante() throws Exception {
        // 22h59 UTC le 31/12/2024 = 23h59 à Paris (2024) ; 00h00 UTC le 01/01/2026 = 01h00 à Paris (2026).
        assertThat(PdfTexte.extraire(telecharger("?type=IFU&annee=2024", "1").body()))
                .contains("Interets et produits de placement 7.00 EUR", "Prelevement Forfaitaire Unique (30%) 2.10 EUR");
        assertThat(PdfTexte.extraire(telecharger("?type=IFU&annee=2026", "1").body()))
                .contains("Interets et produits de placement 8.00 EUR");
    }

    @Test
    @Tag("FR-017-04")
    void un_ifu_sans_interet_donne_un_total_et_un_pfu_nuls() throws Exception {
        assertThat(PdfTexte.extraire(telecharger("?type=IFU&annee=2023", "1").body()))
                .contains("Annee fiscale 2023", "Interets et produits de placement 0.00 EUR",
                        "Prelevement Forfaitaire Unique (30%) 0.00 EUR");
    }

    @Test
    @Tag("FR-017-04")
    @Tag("NFR-SEC-03")
    void un_parametre_designant_un_autre_client_est_ignore_et_l_ifu_reste_celui_du_jeton() throws Exception {
        for (String forge : new String[] {"&clientId=2", "&customerId=2", "&compteId=3", "&clientId=2&compteId=3"}) {
            HttpResponse<byte[]> reponse = telecharger("?type=IFU&annee=2025" + forge, "1");

            assertThat(reponse.statusCode()).as(forge).isEqualTo(200);
            assertThat(reponse.headers().firstValue("Content-Disposition")).as(forge)
                    .contains("attachment; filename=\"ifu_1_2025.pdf\"");
            String pdf = PdfTexte.extraire(reponse.body());
            assertThat(pdf).as(forge).contains("Titulaire Jean Dupont", "Interets et produits de placement 12.35 EUR");
            assertThat(pdf).as(forge).doesNotContain("112.35", "100.00");
        }
        // Le client 2 obtient le sien : ses propres intérêts (100.00), jamais ceux de user1.
        assertThat(PdfTexte.extraire(telecharger("?type=IFU&annee=2025&clientId=1", "2").body()))
                .contains("Interets et produits de placement 100.00 EUR").doesNotContain("12.35");
    }

    @Test
    @Tag("FR-017-09")
    @Tag("FR-017-04")
    void un_client_du_jeton_absent_de_la_projection_rend_404_client_introuvable_sans_pdf_ni_compteur()
            throws Exception {
        long avant = totalDesCompteurs();

        HttpResponse<byte[]> reponse = telecharger("?type=IFU&annee=2025", "99");

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(texte(reponse)).isEqualTo("{\"message\":\"Client introuvable\"}");
        assertThat(reponse.headers().firstValue("Content-Type"))
                .hasValueSatisfying(type -> assertThat(type).startsWith("application/json"));
        assertThat(totalDesCompteurs()).isEqualTo(avant);
    }
}
