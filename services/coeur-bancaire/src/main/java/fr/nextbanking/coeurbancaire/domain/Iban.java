package fr.nextbanking.coeurbancaire.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Numéro de compte destinataire. {@code toString()} le masque ({@code FR76****0189} : 4 premiers caractères,
 * {@code ****}, 4 derniers) : toute journalisation passe par cet objet-valeur, jamais par le texte en clair (BUG-086).
 */
public record Iban(String valeur) {

    private static final int EXTREMITE = 4;
    private static final String MASQUE = "****";
    private static final String MASQUE_AFFICHAGE = "***";
    private static final int LONGUEUR_CODE_PAYS = 2;
    /** Pays de la zone SEPA (RG-013-010) : UE, EEE, Suisse, Royaume-Uni, Monaco, Saint-Marin, Vatican, Andorre. */
    private static final Set<String> ZONE_SEPA = Set.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
            "IS", "LI", "NO", "CH", "GB", "MC", "SM", "VA", "AD");

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

    /**
     * Forme d'affichage de l'écran des bénéficiaires (RG-013-007) : 4 premiers caractères, {@code ***}, 4 derniers
     * ({@code FR35***9010}). Distincte de {@link #toString()}, réservé aux journaux.
     */
    public String affichage() {
        if (valeur.length() <= 2 * EXTREMITE) {
            return MASQUE_AFFICHAGE;
        }
        return valeur.substring(0, EXTREMITE) + MASQUE_AFFICHAGE + valeur.substring(valeur.length() - EXTREMITE);
    }

    /** Vrai si le code pays (deux premières lettres) est celui d'un pays de la zone SEPA (RG-013-010). */
    public boolean zoneSepa() {
        return valeur.length() >= LONGUEUR_CODE_PAYS
                && ZONE_SEPA.contains(valeur.substring(0, LONGUEUR_CODE_PAYS).toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        if (valeur.length() <= 2 * EXTREMITE) {
            return MASQUE;
        }
        return valeur.substring(0, EXTREMITE) + MASQUE + valeur.substring(valeur.length() - EXTREMITE);
    }
}
