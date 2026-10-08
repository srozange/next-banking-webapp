package fr.nextbanking.coeurbancaire.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Catégorie de dépense du budget (FEAT-006) : libellé affiché et mots-clés, comparés sans tenir compte de la casse ni
 * des accents. L'ordre de déclaration est l'ordre de recherche : un libellé revient à la première catégorie dont un
 * mot-clé y figure, sinon à {@link #AUTRES} (RG-006-007).
 */
public enum CategorieBudget {
    LOGEMENT("Logement", "loyer", "edf", "engie", "gaz", "eau", "charges", "syndic"),
    ALIMENTATION("Alimentation", "carrefour", "leclerc", "auchan", "lidl", "monoprix", "intermarche", "supermarche",
            "boulangerie", "restaurant"),
    TRANSPORT("Transport", "sncf", "ratp", "navigo", "uber", "taxi", "essence", "carburant", "peage", "parking"),
    SANTE("Sante", "pharmacie", "medecin", "docteur", "dentiste", "hopital", "mutuelle"),
    LOISIRS("Loisirs", "cinema", "theatre", "concert", "fnac", "sport", "voyage", "hotel"),
    ABONNEMENTS("Abonnements", "abonnement", "netflix", "spotify", "deezer", "orange", "sfr", "free", "bouygues"),
    EPARGNE("Epargne", "epargne", "livret", "placement"),
    REVENUS("Revenus", "salaire", "remboursement"),
    ASSURANCES("Assurances", "assurance", "axa", "maif", "macif", "allianz"),
    AUTRES("Autres");

    private final String libelle;
    private final List<String> motsCles;

    CategorieBudget(String libelle, String... motsCles) {
        this.libelle = libelle;
        this.motsCles = List.of(motsCles);
    }

    /** Libellé affiché (sans accent, comme au legacy). */
    public String libelle() {
        return libelle;
    }

    /** Mots-clés de la catégorie, en minuscules sans accent. */
    public List<String> motsCles() {
        return motsCles;
    }

    /** Catégorie d'un libellé d'opération : la première dont un mot-clé y figure, sinon {@link #AUTRES}. */
    public static CategorieBudget deLibelle(String libelleOperation) {
        String normalise = normaliser(libelleOperation);
        for (CategorieBudget categorie : values()) {
            for (String motCle : categorie.motsCles) {
                if (normalise.contains(motCle)) {
                    return categorie;
                }
            }
        }
        return AUTRES;
    }

    private static String normaliser(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
