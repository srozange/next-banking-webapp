package fr.nextbanking.coeurbancaire.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Format unique des erreurs de l'API des virements : le statut dépend de la cause, le corps est le seul champ
 * {@code message}, sans pile ni nom d'exception.
 */
@Tag("FR-003-02")
@Tag("NFR-SEC-04")
class GestionErreursVirementsTest {

    private final GestionErreursVirements gestion = new GestionErreursVirements();

    @Test
    void chaque_cause_a_son_statut_et_son_texte() {
        Map<Cause, HttpStatus> attendus = Map.of(
                Cause.COMPTES_OBLIGATOIRES, HttpStatus.BAD_REQUEST,
                Cause.COMPTES_IDENTIQUES, HttpStatus.BAD_REQUEST,
                Cause.MONTANT_INVALIDE, HttpStatus.BAD_REQUEST,
                Cause.MOTIF_TROP_LONG, HttpStatus.BAD_REQUEST,
                Cause.SOLDE_INSUFFISANT, HttpStatus.BAD_REQUEST,
                Cause.COMPTE_INTROUVABLE, HttpStatus.NOT_FOUND,
                Cause.COMPTE_ETRANGER, HttpStatus.FORBIDDEN,
                Cause.REMISE_IMPOSSIBLE, HttpStatus.INTERNAL_SERVER_ERROR,
                Cause.CLE_IDEMPOTENCE_DEJA_UTILISEE, HttpStatus.INTERNAL_SERVER_ERROR);

        for (Cause cause : Cause.values()) {
            ResponseEntity<GestionErreursVirements.Message> reponse = gestion.virementRefuse(
                    new ErreurVirement(cause, new IllegalStateException("détail interne")));

            assertThat(reponse.getStatusCode()).as(cause.name()).isEqualTo(attendus.get(cause));
            assertThat(reponse.getBody().message()).isEqualTo(cause.texte());
        }
    }

    @Test
    void une_requete_illisible_rend_400_et_une_erreur_inattendue_le_message_generique() {
        assertThat(gestion.requeteInvalide().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gestion.requeteInvalide().getBody().message()).isEqualTo("Requête invalide");

        ResponseEntity<GestionErreursVirements.Message> inattendue = gestion.autreErreur(
                new IllegalStateException("détail interne"));
        assertThat(inattendue.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(inattendue.getBody().message()).isEqualTo("Une erreur technique est survenue");

        ResponseEntity<GestionErreursVirements.Message> methode = gestion.autreErreur(
                new HttpRequestMethodNotSupportedException(HttpMethod.GET.name()));
        assertThat(methode.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(methode.getBody().message()).isEqualTo("Ressource introuvable");
    }
}
