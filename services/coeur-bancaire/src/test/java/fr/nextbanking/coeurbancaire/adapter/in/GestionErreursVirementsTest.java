package fr.nextbanking.coeurbancaire.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ErreurReleve;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import io.micrometer.observation.ObservationRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Format unique des erreurs de l'API des virements : le statut dépend de la cause, le corps est le seul champ
 * {@code message} (plus {@code reference} pour un contrôle de fraude en erreur), sans pile ni nom d'exception.
 */
@Tag("FR-003-02")
@Tag("NFR-SEC-04")
class GestionErreursVirementsTest {

    private final GestionErreursVirements gestion = new GestionErreursVirements(new EnrichissementTrace(
            ObservationRegistry.create(), new StaticListableBeanFactory().getBeanProvider(
                    io.micrometer.tracing.Tracer.class)));

    @Test
    void chaque_cause_a_son_statut_et_son_texte() {
        Map<Cause, HttpStatus> attendus = new EnumMap<>(Cause.class);
        attendus.put(Cause.COMPTES_OBLIGATOIRES, HttpStatus.BAD_REQUEST);
        attendus.put(Cause.COMPTES_IDENTIQUES, HttpStatus.BAD_REQUEST);
        attendus.put(Cause.MONTANT_INVALIDE, HttpStatus.BAD_REQUEST);
        attendus.put(Cause.MOTIF_TROP_LONG, HttpStatus.BAD_REQUEST);
        attendus.put(Cause.SOLDE_INSUFFISANT, HttpStatus.BAD_REQUEST);
        attendus.put(Cause.MOTIF_DECISION_OBLIGATOIRE, HttpStatus.BAD_REQUEST);
        attendus.put(Cause.COMPTE_INTROUVABLE, HttpStatus.NOT_FOUND);
        attendus.put(Cause.VIREMENT_NON_SUSPENDU, HttpStatus.NOT_FOUND);
        attendus.put(Cause.COMPTE_ETRANGER, HttpStatus.FORBIDDEN);
        attendus.put(Cause.ROLE_INSUFFISANT, HttpStatus.FORBIDDEN);
        attendus.put(Cause.REMISE_IMPOSSIBLE, HttpStatus.INTERNAL_SERVER_ERROR);
        attendus.put(Cause.CLE_IDEMPOTENCE_DEJA_UTILISEE, HttpStatus.INTERNAL_SERVER_ERROR);
        attendus.put(Cause.CONTROLE_EN_ERREUR, HttpStatus.INTERNAL_SERVER_ERROR);

        for (Cause cause : Cause.values()) {
            ResponseEntity<Object> reponse = gestion.virementRefuse(
                    new ErreurVirement(cause, new IllegalStateException("détail interne")));

            assertThat(reponse.getStatusCode()).as(cause.name()).isEqualTo(attendus.get(cause));
            if (cause != Cause.CONTROLE_EN_ERREUR) {
                assertThat(reponse.getBody()).isEqualTo(new GestionErreursVirements.Message(cause.texte()));
            }
        }
    }

    @Test
    @Tag("BUG-084")
    @Tag("FR-003-27")
    void un_controle_en_erreur_rend_le_message_et_la_reference_de_trace() {
        ResponseEntity<Object> reponse = gestion.virementRefuse(new ErreurVirement(Cause.CONTROLE_EN_ERREUR));

        GestionErreursVirements.MessageAvecReference corps =
                (GestionErreursVirements.MessageAvecReference) reponse.getBody();
        assertThat(corps.reference()).isEqualTo("indisponible");
        assertThat(corps.message()).isEqualTo("Votre virement n'a pas pu être vérifié. Rien n'a été débité. "
                + "Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; "
                + "si cela persiste, contactez-nous avec la référence indisponible.");
        assertThat(corps.message()).doesNotContainIgnoringCase("fraude");
    }

    @Test
    void une_requete_illisible_rend_400_et_une_erreur_inattendue_le_message_generique() {
        assertThat(gestion.requeteInvalide().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(gestion.requeteInvalide().getBody())
                .isEqualTo(new GestionErreursVirements.Message("Requête invalide"));

        ResponseEntity<Object> inattendue = gestion.autreErreur(new IllegalStateException("détail interne"));
        assertThat(inattendue.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(inattendue.getBody())
                .isEqualTo(new GestionErreursVirements.Message("Une erreur technique est survenue"));

        ResponseEntity<Object> methode = gestion.autreErreur(
                new HttpRequestMethodNotSupportedException(HttpMethod.GET.name()));
        assertThat(methode.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(methode.getBody()).isEqualTo(new GestionErreursVirements.Message("Ressource introuvable"));
    }

    @Test
    @Tag("FR-005-05")
    void un_releve_refuse_rend_403_pour_un_compte_d_autrui_et_404_pour_un_compte_inexistant() {
        ResponseEntity<GestionErreursVirements.Message> etranger = gestion.releveRefuse(
                new ErreurReleve(ErreurReleve.Cause.COMPTE_ETRANGER));
        ResponseEntity<GestionErreursVirements.Message> introuvable = gestion.releveRefuse(
                new ErreurReleve(ErreurReleve.Cause.COMPTE_INTROUVABLE));

        assertThat(etranger.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(etranger.getBody().message()).isEqualTo("Accès refusé");
        assertThat(introuvable.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(introuvable.getBody().message()).isEqualTo("Ressource introuvable");
    }
}
