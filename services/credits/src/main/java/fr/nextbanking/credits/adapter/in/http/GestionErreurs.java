package fr.nextbanking.credits.adapter.in.http;

import fr.nextbanking.credits.application.AccesPretRefuse;
import fr.nextbanking.credits.application.PretIntrouvable;
import fr.nextbanking.credits.application.SimulationEnEchec;
import fr.nextbanking.credits.domain.SimulationInvalide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs de l'API au format unique {@code {"message": "..."}}, sans pile, nom de classe ni code interne.
 */
@RestControllerAdvice
public class GestionErreurs {

    private static final Logger JOURNAL = LoggerFactory.getLogger(GestionErreurs.class);

    @ExceptionHandler(SimulationInvalide.class)
    public ResponseEntity<Message> simulationInvalide(SimulationInvalide refus) {
        return switch (refus.motif()) {
            case MONTANT -> reponse(HttpStatus.BAD_REQUEST, Textes.MONTANT_INVALIDE);
            case TAUX -> reponse(HttpStatus.BAD_REQUEST, Textes.TAUX_INVALIDE);
            case DUREE -> reponse(HttpStatus.BAD_REQUEST, Textes.DUREE_INVALIDE);
        };
    }

    @ExceptionHandler(SimulationEnEchec.class)
    public ResponseEntity<Message> simulationEnEchec(SimulationEnEchec echec) {
        JOURNAL.error("Calcul de simulation en échec", echec);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, Textes.ERREUR_SIMULATION);
    }

    @ExceptionHandler(AuthentificationRequise.class)
    public ResponseEntity<Message> authentificationRequise() {
        return reponse(HttpStatus.UNAUTHORIZED, Textes.AUTHENTIFICATION_REQUISE);
    }

    @ExceptionHandler({AccessDeniedException.class, AccesPretRefuse.class})
    public ResponseEntity<Message> accesRefuse() {
        return reponse(HttpStatus.FORBIDDEN, Textes.ACCES_REFUSE);
    }

    @ExceptionHandler(PretIntrouvable.class)
    public ResponseEntity<Message> pretIntrouvable() {
        return reponse(HttpStatus.NOT_FOUND, Textes.RESSOURCE_INTROUVABLE);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Message> requeteInvalide() {
        return reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
    }

    /** Exceptions de Spring MVC qui portent leur statut (404, 405, 415...), sinon erreur technique. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Message> autreErreur(Exception erreur) {
        HttpStatus statut = erreur instanceof ErrorResponse avecStatut
                ? Textes.statutPublie(avecStatut.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        if (statut.is5xxServerError()) {
            JOURNAL.error("Erreur technique", erreur);
        }
        return reponse(statut, Textes.texte(statut));
    }

    private static ResponseEntity<Message> reponse(HttpStatus statut, String texte) {
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_JSON).body(new Message(texte));
    }

    /** Corps d'erreur. */
    public record Message(String message) {
    }
}
