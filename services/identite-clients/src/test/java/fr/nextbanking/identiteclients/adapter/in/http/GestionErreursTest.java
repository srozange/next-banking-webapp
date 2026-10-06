package fr.nextbanking.identiteclients.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Textes et statuts publiés pour les cas que les tests d'intégration ne provoquent pas.
 */
class GestionErreursTest {

    private final GestionErreurs gestion = new GestionErreurs();

    @Test
    void un_acces_refuse_rend_403() {
        assertReponse(gestion.accesRefuse(), 403, "Accès refusé");
    }

    @Test
    void un_role_insuffisant_du_cas_d_usage_rend_403() {
        assertReponse(gestion.accesRefuse(), 403, "Accès refusé");
    }

    @Test
    void un_client_introuvable_rend_404() {
        assertReponse(gestion.clientIntrouvable(), 404, "Ressource introuvable");
    }

    @Test
    void une_erreur_inattendue_rend_500_sans_detail() {
        assertReponse(gestion.autreErreur(new IllegalStateException("détail interne")), 500,
                "Une erreur technique est survenue");
    }

    @Test
    void une_erreur_mvc_garde_son_statut_publie() {
        assertReponse(gestion.autreErreur(new HttpMediaTypeNotAcceptableException("x")), 400, "Requête invalide");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)), 500,
                "Une erreur technique est survenue");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.FORBIDDEN)), 403, "Accès refusé");
    }

    @Test
    void ramene_chaque_statut_a_un_statut_publie() {
        assertThat(Textes.statutPublie(400)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Textes.statutPublie(401)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(Textes.statutPublie(403)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(Textes.statutPublie(404)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(Textes.statutPublie(405)).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(Textes.statutPublie(415)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Textes.statutPublie(499)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Textes.statutPublie(200)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Textes.statutPublie(503)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void le_dispatch_d_erreur_ne_rend_que_le_message() {
        AttributsErreur attributs = new AttributsErreur();
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/error");
        requete.setAttribute("jakarta.servlet.error.status_code", 404);

        Map<String, Object> corps = attributs.getErrorAttributes(new ServletWebRequest(requete),
                ErrorAttributeOptions.defaults());

        assertThat(corps).containsExactly(Map.entry("message", "Ressource introuvable"));
    }

    @Test
    void le_dispatch_d_erreur_sans_statut_rend_une_erreur_technique() {
        Map<String, Object> corps = new AttributsErreur().getErrorAttributes(
                new ServletWebRequest(new MockHttpServletRequest("GET", "/error")), ErrorAttributeOptions.defaults());

        assertThat(corps).containsExactly(Map.entry("message", "Une erreur technique est survenue"));
    }

    private static void assertReponse(ResponseEntity<GestionErreurs.Message> reponse, int statut, String texte) {
        assertThat(reponse.getStatusCode().value()).isEqualTo(statut);
        assertThat(reponse.getBody()).isEqualTo(new GestionErreurs.Message(texte));
    }
}
