package fr.nextbanking.documents.adapter.in.http;

import org.springframework.http.HttpStatus;

/**
 * Textes des réponses d'erreur ({@code {"message"}}) : legacy ({@code docs/retro/features/FEAT-017/MESSAGES.md},
 * graphie du legacy, sans accent) ou format commun des services. Un statut est d'abord ramené aux seuls statuts
 * publiés.
 */
final class Textes {

    static final String AUTHENTIFICATION_REQUISE = "Authentification requise";
    static final String ACCES_REFUSE = "Accès refusé";
    static final String REQUETE_INVALIDE = "Requête invalide";
    static final String RESSOURCE_INTROUVABLE = "Ressource introuvable";
    static final String TYPE_MANQUANT = "Parametre type manquant";
    static final String TYPE_INCONNU = "Type inconnu : ";
    static final String COMPTE_ID_MANQUANT = "accountId manquant";
    static final String COMPTE_INTROUVABLE = "Compte introuvable";
    static final String CLIENT_INTROUVABLE = "Client introuvable";
    /** Toute erreur 500 : ce service ne fait que générer des documents. Jamais de trace ni de nom de composant. */
    static final String ERREUR_GENERATION = "Erreur lors de la generation du document";

    private Textes() {
    }

    /** Statut publié : 400, 401, 403, 404, 405 tels quels ; autre 4xx → 400 ; tout le reste → 500. */
    static HttpStatus statutPublie(int statut) {
        return switch (statut) {
            case 400, 401, 403, 404, 405 -> HttpStatus.valueOf(statut);
            default -> statut >= 400 && statut < 500 ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /** Texte générique d'un statut publié. */
    static String texte(HttpStatus statut) {
        return switch (statut) {
            case BAD_REQUEST -> REQUETE_INVALIDE;
            case UNAUTHORIZED -> AUTHENTIFICATION_REQUISE;
            case FORBIDDEN -> ACCES_REFUSE;
            case NOT_FOUND, METHOD_NOT_ALLOWED -> RESSOURCE_INTROUVABLE;
            default -> ERREUR_GENERATION;
        };
    }
}
