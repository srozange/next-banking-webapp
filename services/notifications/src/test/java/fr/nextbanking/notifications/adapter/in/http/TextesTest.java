package fr.nextbanking.notifications.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("FR-002-07")
class TextesTest {

    @Test
    void les_statuts_publies_sont_rendus_tels_quels() {
        for (int statut : new int[] {400, 401, 403, 404, 405}) {
            assertThat(Textes.statutPublie(statut).value()).isEqualTo(statut);
        }
    }

    @Test
    void un_autre_statut_client_devient_400_et_le_reste_500() {
        assertThat(Textes.statutPublie(415)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Textes.statutPublie(503)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Textes.statutPublie(302)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void chaque_statut_publie_a_son_texte_generique() {
        assertThat(Textes.texte(HttpStatus.BAD_REQUEST)).isEqualTo("Requête invalide");
        assertThat(Textes.texte(HttpStatus.UNAUTHORIZED)).isEqualTo("Authentification requise");
        assertThat(Textes.texte(HttpStatus.FORBIDDEN)).isEqualTo("Accès refusé");
        assertThat(Textes.texte(HttpStatus.NOT_FOUND)).isEqualTo("Ressource introuvable");
        assertThat(Textes.texte(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo("Ressource introuvable");
        assertThat(Textes.texte(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("Une erreur technique est survenue");
    }
}
