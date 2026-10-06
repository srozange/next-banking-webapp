package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.domain.LimiteInvalide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs de l'API au format unique {@code {"message": "..."}}, sans pile, nom de classe ni code interne.
 */
@RestControllerAdvice
public class GestionErreurs {

    private static final Logger JOURNAL = LoggerFactory.getLogger(GestionErreurs.class);

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class, LimiteInvalide.class})
    public ResponseEntity<Message> requeteInvalide() {
        return reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
    }

    /** Exceptions de Spring MVC qui portent leur statut (400, 404, 405...), sinon erreur technique. */
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
