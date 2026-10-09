package fr.nextbanking.documents.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.application.AccesCompteRefuse;
import fr.nextbanking.documents.application.ClientIntrouvable;
import fr.nextbanking.documents.application.CompteIntrouvable;
import fr.nextbanking.documents.application.DemandeDocumentInvalide;
import fr.nextbanking.documents.application.port.in.TelechargerDocument.Commande;
import fr.nextbanking.documents.domain.DocumentPdf;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

/** Textes et statuts publiés, dont ceux que les tests d'intégration ne provoquent pas. */
class GestionErreursTest {

    private final GestionErreurs gestion = new GestionErreurs();

    private static DemandeDocumentInvalide demande(String type, String compteId) {
        return (DemandeDocumentInvalide) org.assertj.core.api.Assertions.catchThrowable(() -> new fr.nextbanking
                .documents.application.TelechargerDocumentCasUsage(null, null, null, null, null, null)
                .telecharger(new Commande(1L, type, compteId, null, null)));
    }

    @Test
    @Tag("FR-017-02")
    @Tag("FR-017-03")
    @Tag("FR-008-11")
    void les_refus_de_parametres_rendent_400_avec_les_textes_du_legacy() {
        assertReponse(gestion.demandeInvalide(demande(null, null)), 400, "Parametre type manquant");
        assertReponse(gestion.demandeInvalide(demande("FACTURE", null)), 400, "Type inconnu : FACTURE");
        assertReponse(gestion.demandeInvalide(demandeCompte()), 400, "accountId manquant");
    }

    @Test
    @Tag("FR-017-07")
    void une_annee_ou_un_mois_invalide_rend_400_avec_le_texte_generique_de_requete_invalide() {
        DemandeDocumentInvalide refus = (DemandeDocumentInvalide) org.assertj.core.api.Assertions.catchThrowable(
                () -> new fr.nextbanking.documents.application.TelechargerDocumentCasUsage(null, null, null, null,
                        null, null).telecharger(new Commande(1L, "IFU", null, "abc", null)));

        assertReponse(gestion.demandeInvalide(refus), 400, "Requête invalide");
    }

    private static DemandeDocumentInvalide demandeCompte() {
        return (DemandeDocumentInvalide) org.assertj.core.api.Assertions.catchThrowable(() -> new fr.nextbanking
                .documents.application.TelechargerDocumentCasUsage(null, null, null, null, null, null)
                .telecharger(new Commande(1L, "RIB", "abc", null, null)));
    }

    @Test
    @Tag("FR-017-09")
    void les_ressources_introuvables_rendent_404() {
        assertReponse(gestion.compteIntrouvable(), 404, "Compte introuvable");
        assertReponse(gestion.clientIntrouvable(), 404, "Client introuvable");
        assertThat(new CompteIntrouvable(5L)).hasMessageContaining("5");
        assertThat(new ClientIntrouvable(6L)).hasMessageContaining("6");
    }

    @Test
    @Tag("FR-017-08")
    void le_compte_d_autrui_rend_403_acces_refuse_sans_message_metier() {
        assertReponse(gestion.accesRefuse(), 403, "Accès refusé");
        assertThat(new AccesCompteRefuse(7L)).hasMessageContaining("7");
    }

    @Test
    @Tag("FR-017-01")
    void un_sujet_de_jeton_illisible_rend_401() {
        assertReponse(gestion.authentificationRequise(), 401, "Authentification requise");
    }

    @Test
    void une_requete_illisible_rend_400() {
        assertReponse(gestion.requeteInvalide(), 400, "Requête invalide");
    }

    @Test
    @Tag("FR-017-10")
    @Tag("BUG-059")
    void une_erreur_inattendue_rend_500_au_texte_unique_sans_detail() {
        ResponseEntity<GestionErreurs.Message> reponse =
                gestion.autreErreur(new IllegalStateException("détail interne com.lowagie.text.DocumentException"));

        assertReponse(reponse, 500, "Erreur lors de la generation du document");
        assertThat(reponse.getBody().message()).doesNotContain("lowagie", "Exception", "interne");
    }

    @Test
    void une_erreur_mvc_garde_son_statut_publie() {
        assertReponse(gestion.autreErreur(new HttpMediaTypeNotAcceptableException("x")), 400, "Requête invalide");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.BAD_GATEWAY)), 500,
                "Erreur lors de la generation du document");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.FORBIDDEN)), 403, "Accès refusé");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.UNAUTHORIZED)), 401,
                "Authentification requise");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.NOT_FOUND)), 404,
                "Ressource introuvable");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)), 405,
                "Ressource introuvable");
    }

    @Test
    void ramene_chaque_statut_a_un_statut_publie() {
        assertThat(Textes.statutPublie(400)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Textes.statutPublie(401)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(Textes.statutPublie(415)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Textes.statutPublie(200)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Textes.statutPublie(503)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void le_dispatch_d_erreur_ne_rend_que_le_message() {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/error");
        requete.setAttribute("jakarta.servlet.error.status_code", 404);

        Map<String, Object> corps = new AttributsErreur().getErrorAttributes(new ServletWebRequest(requete),
                ErrorAttributeOptions.defaults());

        assertThat(corps).containsExactly(Map.entry("message", "Ressource introuvable"));
    }

    @Test
    void le_dispatch_d_erreur_sans_statut_rend_le_texte_de_generation() {
        Map<String, Object> corps = new AttributsErreur().getErrorAttributes(
                new ServletWebRequest(new MockHttpServletRequest("GET", "/error")), ErrorAttributeOptions.defaults());

        assertThat(corps).containsExactly(Map.entry("message", "Erreur lors de la generation du document"));
    }

    @Test
    void le_point_d_entree_rend_le_document_avec_ses_en_tetes() {
        PointEntreeTelechargement point = new PointEntreeTelechargement(commande -> {
            assertThat(commande).isEqualTo(new Commande(1L, "RIB", "1", "2026", "3"));
            return new DocumentPdf("rib_1.pdf", new byte[] {1, 2});
        });
        org.springframework.security.oauth2.jwt.Jwt jeton = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("t").header("alg", "none").subject("1").build();

        ResponseEntity<byte[]> reponse = point.telecharger(jeton, "RIB", "1", "2026", "3");

        assertThat(reponse.getStatusCode().value()).isEqualTo(200);
        assertThat(reponse.getHeaders().getFirst("Content-Type")).isEqualTo("application/pdf");
        assertThat(reponse.getHeaders().getFirst("Content-Disposition")).isEqualTo("attachment; filename=\"rib_1.pdf\"");
        assertThat(reponse.getHeaders().getFirst("Cache-Control")).isEqualTo("no-cache, no-store, must-revalidate");
        assertThat(reponse.getBody()).containsExactly(1, 2);
    }

    @Test
    @Tag("FR-017-01")
    void un_sujet_qui_n_est_pas_un_identifiant_numerique_est_refuse() {
        PointEntreeTelechargement point = new PointEntreeTelechargement(commande -> null);
        org.springframework.security.oauth2.jwt.Jwt jeton = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("t").header("alg", "none").subject("user1").build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> point.telecharger(jeton, "RIB", "1", null, null))
                .isInstanceOf(AuthentificationRequise.class);
    }

    private static void assertReponse(ResponseEntity<GestionErreurs.Message> reponse, int statut, String texte) {
        assertThat(reponse.getStatusCode().value()).isEqualTo(statut);
        assertThat(reponse.getBody()).isEqualTo(new GestionErreurs.Message(texte));
    }
}
