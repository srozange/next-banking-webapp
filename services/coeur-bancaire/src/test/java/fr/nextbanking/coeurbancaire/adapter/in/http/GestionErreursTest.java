package fr.nextbanking.coeurbancaire.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.LimiteInvalide;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GestionErreursTest {

    private final GestionErreurs gestion = new GestionErreurs();

    @Test
    @Tag("FR-011-09")
    void une_requete_illisible_ou_une_limite_invalide_rend_400_requete_invalide() {
        ResponseEntity<GestionErreurs.Message> reponse = gestion.requeteInvalide();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(message(reponse)).isEqualTo("Requête invalide");
        assertThat(new LimiteInvalide(0).getMessage()).contains("0");
    }

    @Test
    void une_erreur_de_spring_mvc_garde_son_statut_publie() {
        ResponseEntity<GestionErreurs.Message> reponse = gestion.autreErreur(
                new HttpRequestMethodNotSupportedException("POST"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(message(reponse)).isEqualTo("Ressource introuvable");
    }

    @Test
    void une_erreur_inattendue_rend_500_sans_detail() {
        ResponseEntity<GestionErreurs.Message> reponse = gestion.autreErreur(new IllegalStateException("détail"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(message(reponse)).isEqualTo("Une erreur technique est survenue");
    }

    private static String message(ResponseEntity<GestionErreurs.Message> reponse) {
        return reponse.getBody().message();
    }
}
