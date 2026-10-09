package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire;
import fr.nextbanking.coeurbancaire.domain.ErreurPlafond;
import fr.nextbanking.coeurbancaire.domain.ErreurReleve;
import fr.nextbanking.coeurbancaire.domain.ErreurSaisieBeneficiaire;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs de l'API des virements au format unique {@code {"message": "..."}}, sans pile, nom de classe ni code
 * interne (NFR-SEC-04) : le texte d'un refus est celui du domaine ; le détail technique d'un 5xx n'est que journalisé.
 * Consulté avant le gestionnaire général {@link GestionErreurs}, qui capte aussi {@code Exception}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {PointEntreeVirements.class, PointEntreeRevue.class, PointEntreeBeneficiaires.class,
        PointEntreeVirementsProgrammes.class, PointEntreePlafonds.class, PointEntreePortefeuille.class})
public class GestionErreursVirements {

    static final String ISSUE_REFUS_CONTROLE = "REFUSE_ERREUR_CONTROLE";

    private static final Logger JOURNAL = LoggerFactory.getLogger(GestionErreursVirements.class);

    private final EnrichissementTrace trace;

    public GestionErreursVirements(EnrichissementTrace trace) {
        this.trace = trace;
    }

    /**
     * Un contrôle de fraude en erreur répond {@code {"message", "reference"}} : la référence est l'identifiant de la
     * trace courante, enrichie de l'issue et de l'erreur ; les autres refus répondent {@code {"message"}}.
     */
    @ExceptionHandler(ErreurVirement.class)
    public ResponseEntity<Object> virementRefuse(ErreurVirement refus) {
        HttpStatus statut = statut(refus);
        if (statut.is5xxServerError()) {
            JOURNAL.error("Virement non pris en compte", refus.getCause());
        }
        if (refus.motif() == Cause.CONTROLE_EN_ERREUR) {
            trace.issue(ISSUE_REFUS_CONTROLE);
            trace.erreur(refus);
            String reference = trace.reference();
            return corps(statut, new MessageAvecReference(TextesVirement.refusControle(reference), reference));
        }
        return corps(statut, new Message(refus.getMessage()));
    }

    @ExceptionHandler(ErreurReleve.class)
    public ResponseEntity<Message> releveRefuse(ErreurReleve refus) {
        HttpStatus statut = refus.motif() == ErreurReleve.Cause.COMPTE_ETRANGER ? HttpStatus.FORBIDDEN
                : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_JSON)
                .body(new Message(refus.getMessage()));
    }

    @ExceptionHandler(ErreurBeneficiaire.class)
    public ResponseEntity<Message> beneficiaireRefuse(ErreurBeneficiaire refus) {
        HttpStatus statut = refus.motif() == ErreurBeneficiaire.Cause.ETRANGER ? HttpStatus.FORBIDDEN
                : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_JSON)
                .body(new Message(refus.getMessage()));
    }

    /** Saisie refusée : 400 {@code {"message", "champ"}} ; un dépassement de longueur répond « Requête invalide ». */
    @ExceptionHandler(ErreurSaisieBeneficiaire.class)
    public ResponseEntity<Object> saisieRefusee(ErreurSaisieBeneficiaire refus) {
        String message = refus.motif() == ErreurSaisieBeneficiaire.Cause.TROP_LONG ? Textes.REQUETE_INVALIDE
                : refus.getMessage();
        return corps(HttpStatus.BAD_REQUEST, new MessageAvecChamp(message, refus.champ().nom()));
    }

    /**
     * Création d'un virement programmé refusée : 400 saisie, 403 compte source d'autrui, 404 compte source ou
     * destinataire introuvable. Le corps porte le champ en cause quand il y en a un ; sans texte propre, « Requête
     * invalide ».
     */
    @ExceptionHandler(ErreurVirementProgramme.class)
    public ResponseEntity<Object> virementProgrammeRefuse(ErreurVirementProgramme refus) {
        HttpStatus statut = switch (refus.motif()) {
            case COMPTE_SOURCE_ETRANGER -> HttpStatus.FORBIDDEN;
            case COMPTE_SOURCE_INTROUVABLE, DESTINATAIRE_INTROUVABLE, INTROUVABLE -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        String message = refus.motif().texte() == null ? Textes.REQUETE_INVALIDE : refus.motif().texte();
        if (refus.motif().champ() == null) {
            return corps(statut, new Message(message));
        }
        return corps(statut, new MessageAvecChamp(message, refus.motif().champ().nom()));
    }

    /** Plafond refusé : 400 saisie (texte du domaine, « Requête invalide » pour un mois illisible), 404 introuvable. */
    @ExceptionHandler(ErreurPlafond.class)
    public ResponseEntity<Object> plafondRefuse(ErreurPlafond refus) {
        return switch (refus.motif()) {
            case INTROUVABLE -> reponse(HttpStatus.NOT_FOUND, refus.getMessage());
            case MOIS_INVALIDE -> reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
            default -> reponse(HttpStatus.BAD_REQUEST, refus.getMessage());
        };
    }

    /** Sujet du jeton non numérique : 401 « Authentification requise ». */
    @ExceptionHandler(SujetIllisible.class)
    public ResponseEntity<Object> sujetIllisible() {
        return reponse(HttpStatus.UNAUTHORIZED, Textes.AUTHENTIFICATION_REQUISE);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Object> requeteInvalide() {
        return reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
    }

    /** Exceptions de Spring MVC qui portent leur statut (404, 405, 415...), sinon erreur technique. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> autreErreur(Exception erreur) {
        HttpStatus statut = erreur instanceof ErrorResponse avecStatut
                ? Textes.statutPublie(avecStatut.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        if (statut.is5xxServerError()) {
            JOURNAL.error("Erreur technique", erreur);
        }
        return reponse(statut, Textes.texte(statut));
    }

    private static HttpStatus statut(ErreurVirement refus) {
        return switch (refus.motif()) {
            case COMPTES_OBLIGATOIRES, COMPTES_IDENTIQUES, MONTANT_INVALIDE, MOTIF_TROP_LONG, SOLDE_INSUFFISANT,
                    MOTIF_DECISION_OBLIGATOIRE -> HttpStatus.BAD_REQUEST;
            case COMPTE_INTROUVABLE, VIREMENT_NON_SUSPENDU -> HttpStatus.NOT_FOUND;
            case COMPTE_ETRANGER, ROLE_INSUFFISANT -> HttpStatus.FORBIDDEN;
            case REMISE_IMPOSSIBLE, CLE_IDEMPOTENCE_DEJA_UTILISEE, CONTROLE_EN_ERREUR ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ResponseEntity<Object> reponse(HttpStatus statut, String texte) {
        return corps(statut, new Message(texte));
    }

    private static ResponseEntity<Object> corps(HttpStatus statut, Object corps) {
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_JSON).body(corps);
    }

    /** Corps d'erreur. */
    public record Message(String message) {
    }

    /** Corps d'erreur d'une saisie refusée : {@code champ} désigne le champ du formulaire en cause. */
    public record MessageAvecChamp(String message, String champ) {
    }

    /** Corps d'erreur du contrôle de fraude en erreur : la référence est l'identifiant de la trace courante. */
    public record MessageAvecReference(String message, String reference) {
    }
}
