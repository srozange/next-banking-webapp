package fr.nextbanking.coeurbancaire.domain;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Numéro de compte destinataire. {@code toString()} le masque ({@code FR76****0189} : 4 premiers caractères,
 * {@code ****}, 4 derniers) : toute journalisation passe par cet objet-valeur, jamais par le texte en clair (BUG-086).
 */
public record Iban(String valeur) {

    private static final int EXTREMITE = 4;
    private static final String MASQUE = "****";
    private static final String MASQUE_AFFICHAGE = "***";
    private static final int LONGUEUR_CODE_PAYS = 2;
    private static final int RANG_DEBUT_CHIFFRES = 4;
    private static final int MODULO = 97;
    private static final Pattern FORMAT = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");
    /** Pays de la zone SEPA (RG-013-010) : UE, EEE, Suisse, Royaume-Uni, Monaco, Saint-Marin, Vatican, Andorre. */
    private static final Set<String> ZONE_SEPA = Set.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
            "IS", "LI", "NO", "CH", "GB", "MC", "SM", "VA", "AD");

    public Iban {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le numéro de compte est obligatoire.");
        }
    }

    /** Saisie normalisée (RG-013-021) : tous les blancs supprimés, majuscules ; absente, elle donne le texte vide. */
    public static String normaliser(String saisie) {
        return saisie == null ? "" : saisie.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /** Code pays : les deux premiers caractères, en majuscules (le numéro entier s'il est plus court). */
    public String codePays() {
        return valeur.substring(0, Math.min(valeur.length(), LONGUEUR_CODE_PAYS)).toUpperCase(Locale.ROOT);
    }

    /**
     * Contrôle MOD-97 (ISO 13616) : format pays, clé et 11 à 30 caractères alphanumériques, puis les quatre premiers
     * caractères déplacés à la fin, les lettres remplacées par 10 à 35, et un reste de 1 modulo 97.
     */
    public boolean controleMod97() {
        String majuscules = valeur.toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(majuscules).matches()) {
            return false;
        }
        String reordonne = majuscules.substring(RANG_DEBUT_CHIFFRES) + majuscules.substring(0, RANG_DEBUT_CHIFFRES);
        int reste = 0;
        for (char caractere : reordonne.toCharArray()) {
            int chiffre = Character.digit(caractere, 36);
            reste = (chiffre < 10 ? reste * 10 + chiffre : reste * 100 + chiffre) % MODULO;
        }
        return reste == 1;
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
