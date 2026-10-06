package fr.nextbanking.openbankingconseiller.application;

/**
 * Le service d'IA n'a pas pu répondre. Le message ne porte jamais la clé, l'adresse ni le corps du fournisseur ;
 * aucune pile n'est capturée.
 */
public class ServiceIaIndisponible extends RuntimeException {

    /** Nature de l'échec, qui détermine le texte rendu à l'utilisateur. */
    public enum Nature {
        /** Clé refusée (401 ou 403) : jamais exposée comme telle. */
        AUTHENTIFICATION,
        /** Statut non 2xx autre que 401/403 : le statut est rendu. */
        STATUT,
        /** Délai, réponse illisible, coupe-circuit ouvert, clé absente. */
        INDISPONIBLE
    }

    private final Nature nature;
    private final int statut;

    private ServiceIaIndisponible(Nature nature, int statut, String motif) {
        super(motif, null, false, false);
        this.nature = nature;
        this.statut = statut;
    }

    public static ServiceIaIndisponible authentification(int statut) {
        return new ServiceIaIndisponible(Nature.AUTHENTIFICATION, statut, "authentification refusée (statut " + statut + ")");
    }

    public static ServiceIaIndisponible statut(int statut) {
        return new ServiceIaIndisponible(Nature.STATUT, statut, "statut " + statut);
    }

    public static ServiceIaIndisponible indisponible(String motif) {
        return new ServiceIaIndisponible(Nature.INDISPONIBLE, 0, motif);
    }

    public Nature nature() {
        return nature;
    }

    /** Statut reçu du fournisseur, 0 pour une indisponibilité sans statut. */
    public int statutRecu() {
        return statut;
    }
}
