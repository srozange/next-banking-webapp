package fr.nextbanking.credits.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.AccesPretRefuse;
import fr.nextbanking.credits.application.PretIntrouvable;
import fr.nextbanking.credits.application.SimulationEnEchec;
import fr.nextbanking.credits.domain.SimulationInvalide;
import fr.nextbanking.credits.domain.SimulationInvalide.Motif;
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

/**
 * Textes et statuts publiés, dont ceux que les tests d'intégration ne provoquent pas.
 */
class GestionErreursTest {

    private final GestionErreurs gestion = new GestionErreurs();

    @Test
    @Tag("FR-007-02")
    @Tag("FR-004-05")
    void les_trois_refus_de_simulation_rendent_400_avec_le_texte_du_legacy() {
        assertReponse(gestion.simulationInvalide(new SimulationInvalide(Motif.MONTANT)), 400, "Montant invalide");
        assertReponse(gestion.simulationInvalide(new SimulationInvalide(Motif.TAUX)), 400, "Taux invalide (0-30%)");
        assertReponse(gestion.simulationInvalide(new SimulationInvalide(Motif.DUREE)), 400,
                "Duree invalide (1-360 mois)");
    }

    @Test
    @Tag("FR-007-03")
    void une_simulation_en_echec_rend_500_au_message_fixe() {
        assertReponse(gestion.simulationEnEchec(new SimulationEnEchec(new IllegalStateException("détail interne"))),
                500, "Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer.");
    }

    @Test
    @Tag("FR-007-01")
    void les_refus_d_acces_rendent_401_403_404() {
        assertReponse(gestion.authentificationRequise(), 401, "Authentification requise");
        assertReponse(gestion.accesRefuse(), 403, "Accès refusé");
        assertReponse(gestion.pretIntrouvable(), 404, "Ressource introuvable");
        assertThat(new AccesPretRefuse(1L)).isNotNull();
        assertThat(new PretIntrouvable(1L)).isNotNull();
    }

    @Test
    void une_requete_illisible_rend_400() {
        assertReponse(gestion.requeteInvalide(), 400, "Requête invalide");
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
