package fr.nextbanking.documents.domain;

import java.util.List;

/** Libellés des mois en français, avec accents et majuscule initiale (FEAT-017 MESSAGES.md). */
public final class LibellesMois {

    private static final List<String> LIBELLES = List.of("Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre");

    private LibellesMois() {
    }

    /** Libellé du mois 1 à 12. */
    public static String libelle(int mois) {
        return LIBELLES.get(mois - 1);
    }
}
