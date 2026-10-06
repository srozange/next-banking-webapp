package fr.nextbanking.coeurbancaire.domain;

/**
 * Numéro de compte destinataire. {@code toString()} le masque ({@code FR76****0189} : 4 premiers caractères,
 * {@code ****}, 4 derniers) : toute journalisation passe par cet objet-valeur, jamais par le texte en clair (BUG-086).
 */
public record Iban(String valeur) {

    private static final int EXTREMITE = 4;
    private static final String MASQUE = "****";

    public Iban {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le numéro de compte est obligatoire.");
        }
    }

    /** Vrai si le numéro commence par le préfixe, sans tenir compte de la casse. */
    public boolean commencePar(String prefixe) {
        return valeur.regionMatches(true, 0, prefixe, 0, prefixe.length());
    }

    /** Numéro en clair, réservé au texte stocké d'une alerte ; jamais à un journal. */
    public String enClair() {
        return valeur;
    }

    @Override
    public String toString() {
        if (valeur.length() <= 2 * EXTREMITE) {
            return MASQUE;
        }
        return valeur.substring(0, EXTREMITE) + MASQUE + valeur.substring(valeur.length() - EXTREMITE);
    }
}
