package fr.nextbanking.notifications.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.notifications.application.AccesNotificationRefuse;
import fr.nextbanking.notifications.application.NotificationIntrouvable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@Tag("FR-002-07")
class GestionErreursTest {

    private final GestionErreurs gestion = new GestionErreurs();

    private static void assertReponse(ResponseEntity<GestionErreurs.Message> reponse, HttpStatus statut, String texte) {
        assertThat(reponse.getStatusCode()).isEqualTo(statut);
        assertThat(reponse.getBody()).isEqualTo(new GestionErreurs.Message(texte));
    }

    @Test
    void chaque_erreur_a_son_statut_et_son_texte_sans_detail_technique() {
        assertReponse(gestion.authentificationRequise(), HttpStatus.UNAUTHORIZED, "Authentification requise");
        assertReponse(gestion.accesRefuse(), HttpStatus.FORBIDDEN, "Accès refusé");
        assertReponse(gestion.notificationIntrouvable(), HttpStatus.NOT_FOUND, "Ressource introuvable");
        assertReponse(gestion.requeteInvalide(), HttpStatus.BAD_REQUEST, "Requête invalide");
        assertThat(new AccesNotificationRefuse(3).getMessage()).contains("3");
        assertThat(new NotificationIntrouvable(3).getMessage()).contains("3");
    }

    @Test
    void une_exception_de_spring_garde_son_statut_publie() {
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)),
                HttpStatus.METHOD_NOT_ALLOWED, "Ressource introuvable");
        assertReponse(gestion.autreErreur(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)),
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur technique est survenue");
    }

    @Test
    void une_autre_exception_est_une_erreur_technique() {
        assertReponse(gestion.autreErreur(new IllegalStateException("détail interne")),
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur technique est survenue");
    }
}
