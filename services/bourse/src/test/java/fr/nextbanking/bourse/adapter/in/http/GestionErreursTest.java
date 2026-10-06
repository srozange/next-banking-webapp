package fr.nextbanking.bourse.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.bourse.domain.AjoutRefuse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GestionErreursTest {

    private final GestionErreurs gestion = new GestionErreurs();

    @Test
    @Tag("FR-029-09")
    void chaque_refus_d_ajout_rend_400_et_son_texte_legacy() {
        assertThat(message(gestion.ajoutRefuse(AjoutRefuse.pour(AjoutRefuse.Motif.SYMBOLE_REQUIS))))
                .isEqualTo("Le symbole est obligatoire (ex: AMZN, SG.PA)");
        assertThat(message(gestion.ajoutRefuse(AjoutRefuse.pour(AjoutRefuse.Motif.NOM_REQUIS))))
                .isEqualTo("Le nom de la valeur est obligatoire");
        assertThat(message(gestion.ajoutRefuse(AjoutRefuse.pour(AjoutRefuse.Motif.REQUETE_INVALIDE))))
                .isEqualTo("Requête invalide");
        assertThat(message(gestion.ajoutRefuse(AjoutRefuse.doublon("BNP.PA"))))
                .isEqualTo("Le symbole « BNP.PA » existe déjà dans votre watchlist");
        assertThat(gestion.ajoutRefuse(AjoutRefuse.doublon("X")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
