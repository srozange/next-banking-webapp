package fr.nextbanking.openbankingconseiller.adapter.in.http;

import org.springframework.http.HttpStatus;

/**
 * Textes des réponses d'erreur ({@code {"message"}}) : legacy ({@code MESSAGES.md}, casse comprise) ou
 * {@code textes-nouveaux.md}, lot « Stories ». Un statut est d'abord ramené aux seuls statuts publiés.
 */
final class Textes {

    static final String REQUETE_INVALIDE = "Requête invalide";
    static final String AUTHENTIFICATION_REQUISE = "Authentification requise";
    static final String ACCES_REFUSE = "Accès refusé";
    static final String RESSOURCE_INTROUVABLE = "Ressource introuvable";
    static final String ERREUR_TECHNIQUE = "Une erreur technique est survenue";

    // Legacy (MESSAGES.md), orthographe d'origine ; BUG-038 : aucun code 401/403 n'est exposé.
    static final String IA_INDISPONIBLE =
            "Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence.";
    private static final String IA_STATUT = "Je suis momentanement indisponible (HTTP %d). Veuillez reessayer.";

    private Textes() {
    }

    /** Statut publié : 400, 401, 403, 404, 405 tels quels ; autre 4xx → 400 ; tout le reste → 500. */
    static HttpStatus statutPublie(int statut) {
        return switch (statut) {
            case 400, 401, 403, 404, 405 -> HttpStatus.valueOf(statut);
            default -> statut >= 400 && statut < 500 ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /** Texte rendu pour un statut non 2xx du service d'IA (RG-010-020). */
    static String iaStatut(int statut) {
        return IA_STATUT.formatted(statut);
    }

    /** Texte générique d'un statut publié. */
    static String texte(HttpStatus statut) {
        return switch (statut) {
            case BAD_REQUEST -> REQUETE_INVALIDE;
            case UNAUTHORIZED -> AUTHENTIFICATION_REQUISE;
            case FORBIDDEN -> ACCES_REFUSE;
            case NOT_FOUND, METHOD_NOT_ALLOWED -> RESSOURCE_INTROUVABLE;
            default -> ERREUR_TECHNIQUE;
        };
    }
}
