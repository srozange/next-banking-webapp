package fr.nextbanking.credits.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.AccesPretRefuse;
import fr.nextbanking.credits.application.DemandePretEnEchec;
import fr.nextbanking.credits.application.EcheanceIntrouvable;
import fr.nextbanking.credits.application.PaiementEnEchec;
import fr.nextbanking.credits.application.PaiementRefuse;
import fr.nextbanking.credits.application.PretIntrouvable;
import fr.nextbanking.credits.application.SuppressionEnEchec;
import fr.nextbanking.credits.application.SuppressionRefusee;
import fr.nextbanking.credits.application.SimulationEnEchec;
import fr.nextbanking.credits.application.TransmissionEnEchec;
import fr.nextbanking.credits.domain.DemandePretInvalide;
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
    @Tag("FR-007-05")
    @Tag("FR-007-06")
    void les_refus_de_demande_rendent_400_avec_les_textes_du_legacy_sans_plage() {
        assertReponse(gestion.demandePretInvalide(new DemandePretInvalide(DemandePretInvalide.Motif.TYPE)), 400,
                "Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer.");
        assertReponse(gestion.demandePretInvalide(new DemandePretInvalide(DemandePretInvalide.Motif.MONTANT)), 400,
                "Montant invalide");
        assertReponse(gestion.demandePretInvalide(new DemandePretInvalide(DemandePretInvalide.Motif.TAUX)), 400,
                "Taux invalide");
        assertReponse(gestion.demandePretInvalide(new DemandePretInvalide(DemandePretInvalide.Motif.DUREE)), 400,
                "Duree invalide");
        assertReponse(gestion.demandePretInvalide(new DemandePretInvalide(DemandePretInvalide.Motif.OBJET)), 400,
                "Veuillez indiquer un motif");
    }

    @Test
    @Tag("FR-024-02")
    @Tag("BUG-077")
    void une_transmission_en_echec_rend_503_et_une_demande_en_echec_rend_500_sans_detail() {
        assertReponse(gestion.transmissionEnEchec(new TransmissionEnEchec(new IllegalStateException("détail"))), 503,
                "Service credit temporairement indisponible, veuillez reessayer plus tard");
        assertReponse(gestion.demandePretEnEchec(new DemandePretEnEchec(new IllegalStateException("détail"))), 500,
                "Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer.");
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
    @Tag("FR-007-12")
    @Tag("FR-007-13")
    void paiement_et_suppression_ont_leurs_textes_propres_sans_detail_technique() {
        String paiement = "Erreur : l'échéance n'a pas été marquée comme payée. Veuillez réessayer.";
        String suppression = "Erreur suppression : le prêt n'a pas été supprimé. Veuillez réessayer.";

        assertReponse(gestion.paiementRefuse(), 403, paiement);
        assertReponse(gestion.suppressionRefusee(), 403, suppression);
        assertReponse(gestion.paiementEnEchec(new PaiementEnEchec(new IllegalStateException("détail"))), 500,
                paiement);
        assertReponse(gestion.suppressionEnEchec(new SuppressionEnEchec(new IllegalStateException("détail"))), 500,
                suppression);
        assertReponse(gestion.pretIntrouvable(), 404, "Ressource introuvable");
        assertThat(new EcheanceIntrouvable(1L)).isNotNull();
        assertThat(new PaiementRefuse(1L)).isNotNull();
        assertThat(new SuppressionRefusee(1L)).isNotNull();
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
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)), 503,
                "Service credit temporairement indisponible, veuillez reessayer plus tard");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.BAD_GATEWAY)), 500,
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
        assertThat(Textes.statutPublie(503)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(Textes.statutPublie(502)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
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
