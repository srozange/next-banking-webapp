package fr.nextbanking.credits.adapter.in.http;

import org.springframework.http.HttpStatus;

/**
 * Textes des réponses d'erreur ({@code {"message"}}) : legacy ({@code MESSAGES.md}, FEAT-007) ou
 * {@code textes-nouveaux.md}. Un statut est d'abord ramené aux seuls statuts publiés.
 */
final class Textes {

    static final String REQUETE_INVALIDE = "Requête invalide";
    static final String AUTHENTIFICATION_REQUISE = "Authentification requise";
    static final String ACCES_REFUSE = "Accès refusé";
    static final String RESSOURCE_INTROUVABLE = "Ressource introuvable";
    static final String MONTANT_INVALIDE = "Montant invalide";
    static final String TAUX_INVALIDE = "Taux invalide (0-30%)";
    static final String DUREE_INVALIDE = "Duree invalide (1-360 mois)";
    /** pret.erreur.simulation (textes-nouveaux.md). */
    static final String ERREUR_SIMULATION = "Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer.";
    static final String ERREUR_TECHNIQUE = "Une erreur technique est survenue";

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
            default -> ERREUR_TECHNIQUE;
        };
    }
}
