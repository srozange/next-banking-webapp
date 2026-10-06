package fr.nextbanking.bourse.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Valeur à ajouter à la watchlist, normalisée et validée : symbole rogné en majuscules, nom et marché rognés, devise
 * rognée en majuscules (FR-009-12, FR-029-10). Contrôles dans l'ordre : symbole requis, nom requis, puis longueurs,
 * marché et devise dans leurs listes fermées (FR-029-13, FR-029-14). Le doublon est vérifié par le cas d'usage.
 */
public record NouvelleValeur(String symbole, String nom, String marche, String devise) {

    public static final int SYMBOLE_MAX = 20;
    public static final int NOM_MAX = 100;
    public static final Set<String> MARCHES = Set.of("Euronext Paris", "NASDAQ", "NYSE", "LSE", "Autre");
    public static final Set<String> DEVISES = Set.of("EUR", "USD", "GBP");

    public NouvelleValeur {
        if (symbole == null || symbole.isBlank()) {
            throw AjoutRefuse.pour(AjoutRefuse.Motif.SYMBOLE_REQUIS);
        }
        if (nom == null || nom.isBlank()) {
            throw AjoutRefuse.pour(AjoutRefuse.Motif.NOM_REQUIS);
        }
        symbole = symbole.trim().toUpperCase(Locale.ROOT);
        nom = nom.trim();
        marche = marche == null ? null : marche.trim();
        devise = devise == null ? null : devise.trim().toUpperCase(Locale.ROOT);
        if (symbole.length() > SYMBOLE_MAX || nom.length() > NOM_MAX
                || marche == null || !MARCHES.contains(marche) || devise == null || !DEVISES.contains(devise)) {
            throw AjoutRefuse.pour(AjoutRefuse.Motif.REQUETE_INVALIDE);
        }
    }

    /** Valeur normalisée et validée à partir de la saisie brute. */
    public static NouvelleValeur depuis(String symbole, String nom, String marche, String devise) {
        return new NouvelleValeur(symbole, nom, marche, devise);
    }
}
