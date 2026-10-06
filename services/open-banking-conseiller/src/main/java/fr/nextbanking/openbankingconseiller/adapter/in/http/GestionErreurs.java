package fr.nextbanking.openbankingconseiller.adapter.in.http;

import fr.nextbanking.openbankingconseiller.application.CompteExterneIntrouvable;
import fr.nextbanking.openbankingconseiller.application.ConnexionPowensImpossible;
import fr.nextbanking.openbankingconseiller.application.PowensIndisponible;
import fr.nextbanking.openbankingconseiller.application.PowensNonConnecte;
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

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Message> requeteInvalide() {
        return reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
    }

    /** Powens refuse ou n'initialise pas la connexion : 503, sans adresse (RG-028-004). */
    @ExceptionHandler(ConnexionPowensImpossible.class)
    public ResponseEntity<Message> connexionPowensImpossible() {
        return reponse(HttpStatus.SERVICE_UNAVAILABLE, Textes.CONNEXION_POWENS_IMPOSSIBLE);
    }

    /** Compte absent des comptes externes du client, dont celui d'un autre client. */
    @ExceptionHandler(CompteExterneIntrouvable.class)
    public ResponseEntity<Message> compteExterneIntrouvable() {
        return reponse(HttpStatus.NOT_FOUND, Textes.RESSOURCE_INTROUVABLE);
    }

    /** Rafraîchissement sans jeton Powens. */
    @ExceptionHandler(PowensNonConnecte.class)
    public ResponseEntity<Message> powensNonConnecte() {
        return reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
    }

    /** Powens en panne : jamais une liste vide (BUG-043, BUG-089). */
    @ExceptionHandler(PowensIndisponible.class)
    public ResponseEntity<Message> powensIndisponible() {
        return reponse(HttpStatus.SERVICE_UNAVAILABLE, Textes.POWENS_INDISPONIBLE);
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
