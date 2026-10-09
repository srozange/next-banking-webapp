package fr.nextbanking.documents.adapter.in.http;

import fr.nextbanking.documents.application.AccesCompteRefuse;
import fr.nextbanking.documents.application.ClientIntrouvable;
import fr.nextbanking.documents.application.CompteIntrouvable;
import fr.nextbanking.documents.application.DemandeDocumentInvalide;
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
 * Erreurs de l'API au format unique {@code {"message": "..."}}, sans pile, nom de classe ni code interne : textes du
 * legacy pour les paramètres (400) et les ressources introuvables (404), « Accès refusé » (403) pour le compte d'un
 * autre client, texte unique de génération pour toute erreur inattendue (500).
 */
@RestControllerAdvice
public class GestionErreurs {

    private static final Logger JOURNAL = LoggerFactory.getLogger(GestionErreurs.class);

    @ExceptionHandler(DemandeDocumentInvalide.class)
    public ResponseEntity<Message> demandeInvalide(DemandeDocumentInvalide refus) {
        return switch (refus.motif()) {
            case TYPE_MANQUANT -> reponse(HttpStatus.BAD_REQUEST, Textes.TYPE_MANQUANT);
            case TYPE_INCONNU -> reponse(HttpStatus.BAD_REQUEST, Textes.TYPE_INCONNU + refus.valeur());
            case COMPTE_ID_MANQUANT -> reponse(HttpStatus.BAD_REQUEST, Textes.COMPTE_ID_MANQUANT);
        };
    }

    @ExceptionHandler(CompteIntrouvable.class)
    public ResponseEntity<Message> compteIntrouvable() {
        return reponse(HttpStatus.NOT_FOUND, Textes.COMPTE_INTROUVABLE);
    }

    @ExceptionHandler(ClientIntrouvable.class)
    public ResponseEntity<Message> clientIntrouvable() {
        return reponse(HttpStatus.NOT_FOUND, Textes.CLIENT_INTROUVABLE);
    }

    @ExceptionHandler({AccessDeniedException.class, AccesCompteRefuse.class})
    public ResponseEntity<Message> accesRefuse() {
        return reponse(HttpStatus.FORBIDDEN, Textes.ACCES_REFUSE);
    }

    @ExceptionHandler(AuthentificationRequise.class)
    public ResponseEntity<Message> authentificationRequise() {
        return reponse(HttpStatus.UNAUTHORIZED, Textes.AUTHENTIFICATION_REQUISE);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Message> requeteInvalide() {
        return reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
    }

    /** Exceptions de Spring MVC qui portent leur statut (404, 405...), sinon erreur de génération (500). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Message> autreErreur(Exception erreur) {
        HttpStatus statut = erreur instanceof ErrorResponse avecStatut
                ? Textes.statutPublie(avecStatut.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        if (statut.is5xxServerError()) {
            JOURNAL.error("Erreur lors de la génération d'un document", erreur);
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
