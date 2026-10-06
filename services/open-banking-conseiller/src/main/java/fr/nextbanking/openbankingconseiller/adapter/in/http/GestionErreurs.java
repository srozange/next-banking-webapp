package fr.nextbanking.openbankingconseiller.adapter.in.http;

import fr.nextbanking.openbankingconseiller.application.MessageVide;
import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
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

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class, MessageVide.class,
            PointEntreeChat.RequeteInvalide.class})
    public ResponseEntity<Message> requeteInvalide() {
        return reponse(HttpStatus.BAD_REQUEST, Textes.REQUETE_INVALIDE);
    }

    /**
     * Service d'IA en échec : statut non 2xx (hors 401/403) en 502 avec ce statut ; tout le reste, y compris un refus
     * d'authentification, en 503 sans code technique (BUG-038). Le motif journalisé ne porte ni clé ni corps.
     */
    @ExceptionHandler(ServiceIaIndisponible.class)
    public ResponseEntity<Message> serviceIaIndisponible(ServiceIaIndisponible erreur) {
        JOURNAL.warn("Service d'IA indisponible : {}", erreur.getMessage());
        if (erreur.nature() == ServiceIaIndisponible.Nature.STATUT) {
            return reponse(HttpStatus.BAD_GATEWAY, Textes.iaStatut(erreur.statutRecu()));
        }
        return reponse(HttpStatus.SERVICE_UNAVAILABLE, Textes.IA_INDISPONIBLE);
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
